// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.imagare;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;

import com.google.common.base.CharMatcher;
import com.google.common.hash.Hashing;
import com.google.common.net.HttpHeaders;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.mime.FileTypeRegistry;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.restapi.project.CommitsCollection;
import com.google.gerrit.server.restapi.project.GetHead;
import com.google.gerrit.util.http.CacheHeaders;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import eu.medsea.mimeutil.MimeType;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

@Singleton
public class ImageServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  public static final String PATH_PREFIX = "/project/";

  private final String pluginName;
  private final ProjectControl.Factory projectControlFactory;
  private final ProjectCache projectCache;
  private final Provider<GetHead> getHead;
  private final GitRepositoryManager repoManager;
  private final FileTypeRegistry fileTypeRegistry;
  private final CommitsCollection commits;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  ImageServlet(
      @PluginName String pluginName,
      ProjectControl.Factory projectControlFactory,
      ProjectCache projectCache,
      Provider<GetHead> getHead,
      GitRepositoryManager repoManager,
      FileTypeRegistry fileTypeRegistry,
      CommitsCollection commits,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend) {
    this.pluginName = pluginName;
    this.projectControlFactory = projectControlFactory;
    this.projectCache = projectCache;
    this.getHead = getHead;
    this.repoManager = repoManager;
    this.fileTypeRegistry = fileTypeRegistry;
    this.commits = commits;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public void service(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
      CacheHeaders.setNotCacheable(res);
      res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      return;
    }

    ResourceKey key = ResourceKey.fromPath(getEncodedPath(req));
    ProjectState state = projectCache.get(key.project);
    if (state == null || key.file == null) {
      notFound(res);
      return;
    }

    MimeType mimeType = fileTypeRegistry.getMimeType(key.file, (byte[]) null);
    if (!("image".equals(mimeType.getMediaType()) && fileTypeRegistry.isSafeInline(mimeType))) {
      notFound(res);
      return;
    }

    try {
      ProjectControl projectControl = projectControlFactory.controlFor(key.project);
      String rev = key.revision;
      if (rev == null || Constants.HEAD.equals(rev)) {
        rev = getHead.get().apply(new ProjectResource(projectControl));
      } else {
        if (!ObjectId.isId(rev)) {
          if (!rev.startsWith(Constants.R_REFS)) {
            rev = Constants.R_HEADS + rev;
          }
          PermissionBackend.ForProject perm = permissionBackend.user(self).project(key.project);
          try {
            perm.ref(rev).check(RefPermission.READ);
          } catch (AuthException e) {
            notFound(res);
            return;
          }
        }
      }
      try (Repository repo = repoManager.openRepository(key.project)) {
        ObjectId revId = repo.resolve(rev != null ? rev : Constants.HEAD);
        if (revId == null) {
          notFound(res);
          return;
        }

        if (ObjectId.isId(rev)) {
          try (RevWalk rw = new RevWalk(repo)) {
            RevCommit commit = rw.parseCommit(repo.resolve(rev));
            if (!commits.canRead(state, repo, commit)) {
              notFound(res);
              return;
            }
          }
        }

        String eTag = null;
        String receivedETag = req.getHeader(HttpHeaders.IF_NONE_MATCH);
        if (receivedETag != null) {
          eTag = computeETag(key.project, revId, key.file);
          if (eTag.equals(receivedETag)) {
            res.sendError(SC_NOT_MODIFIED);
            return;
          }
        }

        if (!"image".equals(mimeType.getMediaType())) {
          notFound(res);
          return;
        }

        try (RevWalk rw = new RevWalk(repo)) {
          RevCommit commit = rw.parseCommit(revId);
          RevTree tree = commit.getTree();
          try (TreeWalk tw = new TreeWalk(repo)) {
            tw.addTree(tree);
            tw.setRecursive(true);
            tw.setFilter(PathFilter.create(key.file));
            if (!tw.next()) {
              notFound(res);
              return;
            }
            ObjectId objectId = tw.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);
            byte[] content = loader.getBytes(Integer.MAX_VALUE);

            mimeType = fileTypeRegistry.getMimeType(key.file, content);
            if (!"image".equals(mimeType.getMediaType())
                || !fileTypeRegistry.isSafeInline(mimeType)) {
              notFound(res);
              return;
            }
            res.setHeader(
                HttpHeaders.ETAG, eTag != null ? eTag : computeETag(key.project, revId, key.file));
            CacheHeaders.setCacheablePrivate(res, 7, TimeUnit.DAYS, false);
            send(req, res, content, mimeType.toString(), commit.getCommitTime());
            return;
          }
        } catch (IOException e) {
          notFound(res);
          return;
        }
      }
    } catch (RepositoryNotFoundException
        | NoSuchProjectException
        | ResourceNotFoundException
        | AuthException
        | RevisionSyntaxException
        | PermissionBackendException e) {
      notFound(res);
      return;
    }
  }

  private String getEncodedPath(HttpServletRequest req) {
    String path = req.getRequestURI();
    String prefix = "/plugins/" + pluginName;
    if (path.startsWith(prefix)) {
      path = path.substring(prefix.length());
    }
    return path;
  }

  private static String computeETag(Project.NameKey project, ObjectId revId, String file) {
    return Hashing.md5()
        .newHasher()
        .putUnencodedChars(project.get())
        .putUnencodedChars(revId.getName())
        .putUnencodedChars(file)
        .hash()
        .toString();
  }

  private void send(
      HttpServletRequest req,
      HttpServletResponse res,
      byte[] content,
      String contentType,
      long lastModified)
      throws IOException {
    if (0 < lastModified) {
      long ifModifiedSince = req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
      if (ifModifiedSince > 0 && ifModifiedSince == lastModified) {
        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }
      res.setDateHeader("Last-Modified", lastModified);
    }
    res.setContentType(contentType);
    res.setCharacterEncoding(UTF_8.name());
    res.setContentLength(content.length);
    res.getOutputStream().write(content);
  }

  private static void notFound(HttpServletResponse res) throws IOException {
    CacheHeaders.setNotCacheable(res);
    res.sendError(HttpServletResponse.SC_NOT_FOUND);
  }

  private static class ResourceKey {
    final Project.NameKey project;
    final String file;
    final String revision;

    static ResourceKey fromPath(String path) {
      String project;
      String file = null;
      String revision = null;

      if (!path.startsWith(PATH_PREFIX)) {
        // should not happen since this servlet is only registered to handle
        // paths that start with this prefix
        throw new IllegalStateException("path must start with '" + PATH_PREFIX + "'");
      }
      path = path.substring(PATH_PREFIX.length());

      int i = path.indexOf('/');
      if (i != -1 && i != path.length() - 1) {
        project = IdString.fromUrl(path.substring(0, i)).get();
        String rest = path.substring(i + 1);

        if (rest.startsWith("rev/")) {
          if (rest.length() > 4) {
            rest = rest.substring(4);
            i = rest.indexOf('/');
            if (i != -1 && i != path.length() - 1) {
              revision = IdString.fromUrl(rest.substring(0, i)).get();
              file = IdString.fromUrl(rest.substring(i + 1)).get();
            } else {
              revision = IdString.fromUrl(rest).get();
            }
          }
        } else {
          file = IdString.fromUrl(rest).get();
        }

      } else {
        project = IdString.fromUrl(CharMatcher.is('/').trimTrailingFrom(path)).get();
      }

      return new ResourceKey(project, file, revision);
    }

    private ResourceKey(String p, String f, String r) {
      project = new Project.NameKey(p);
      file = f;
      revision = r;
    }
  }
}
