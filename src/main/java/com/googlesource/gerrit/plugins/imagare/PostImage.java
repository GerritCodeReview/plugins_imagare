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

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.FileTypeRegistry;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.RefControl;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.googlesource.gerrit.plugins.imagare.PostImage.Input;

import eu.medsea.mimeutil.MimeType;

import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.Base64;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostImage implements RestModifyView<ProjectResource, Input> {

  public static class Input {
    public String imageData;
  }

  private final FileTypeRegistry registry;
  private final Pattern imageDataPattern;
  private final Provider<IdentifiedUser> self;
  private final GitRepositoryManager repoManager;
  private final PersonIdent myIdent;
  private final String canonicalWebUrl;

  @Inject
  public PostImage(FileTypeRegistry registry, Provider<IdentifiedUser> self,
      GitRepositoryManager repoManager, @GerritPersonIdent PersonIdent myIdent,
      @CanonicalWebUrl String canonicalWebUrl) {
    this.registry = registry;
    this.imageDataPattern = Pattern.compile("data:([\\w/.-]+);([\\w]+),(.*)");
    this.self = self;
    this.repoManager = repoManager;
    this.myIdent = myIdent;
    this.canonicalWebUrl = canonicalWebUrl;
  }

  @Override
  public Response<ImageInfo> apply(ProjectResource rsrc, Input input)
      throws MethodNotAllowedException, BadRequestException, AuthException,
      IOException {
    if (input == null) {
      input = new Input();
    }
    ImageInfo info;
    if (input.imageData != null) {
      info = storeImage(rsrc.getControl(), input.imageData);
    } else {
      throw new BadRequestException("no image data");
    }
    return Response.created(info);
  }

  private ImageInfo storeImage(ProjectControl pc, String imageData)
      throws MethodNotAllowedException, BadRequestException, AuthException,
      IOException {
    Matcher m = imageDataPattern.matcher(imageData);
    if (m.matches()) {
      String receivedMimeType = m.group(1);
      String encoding = m.group(2);
      String encodedContent = m.group(3);

      if ("base64".equals(encoding)) {
        byte[] content = Base64.decode(encodedContent);
        MimeType mimeType =
            registry.getMimeType("img." + receivedMimeType, content);
        if (!"image".equals(mimeType.getMediaType())) {
          throw new MethodNotAllowedException("no image");
        }
        if (!receivedMimeType.equals(mimeType.toString())) {
          throw new BadRequestException("incorrect mime type");
        }
        return new ImageInfo(storeImage(pc, mimeType, content));
      } else {
        throw new MethodNotAllowedException("unsupported encoding");
      }
    } else {
      throw new BadRequestException("invalid image data");
    }
  }

  private String storeImage(ProjectControl pc, MimeType mimeType, byte[] content)
      throws AuthException, IOException {
    String ref = getRef(content);
    RefControl rc = pc.controlForRef(ref);
    String file = "img." + mimeType.getSubType();

    Repository repo = repoManager.openRepository(pc.getProject().getNameKey());
    try {
      ObjectId commitId = repo.resolve(ref);
      if (commitId != null) {
        // this image exists already
        return getUrl(pc.getProject().getNameKey(), commitId.getName(), file);
      }

      RevWalk rw = new RevWalk(repo);
      try {
        ObjectInserter oi = repo.newObjectInserter();
        try {
          ObjectId blobId = oi.insert(Constants.OBJ_BLOB, content);
          oi.flush();

          TreeFormatter tf = new TreeFormatter();
          tf.append(file, FileMode.REGULAR_FILE, blobId);
          ObjectId treeId = tf.insertTo(oi);
          oi.flush();

          PersonIdent authorIdent =
              self.get().newCommitterIdent(myIdent.getWhen(), myIdent.getTimeZone());
          CommitBuilder cb = new CommitBuilder();
          cb.setTreeId(treeId);
          cb.setAuthor(authorIdent);
          cb.setCommitter(authorIdent);
          cb.setMessage("Image Upload");

          commitId = oi.insert(cb);
          oi.flush();

          if (!rc.canCreate(rw, rw.parseCommit(commitId))) {
            throw new AuthException(String.format(
                "Project %s doesn't allow image upload.", pc.getProject().getName()));
          }

          RefUpdate ru = repo.updateRef(ref);
          ru.setExpectedOldObjectId(ObjectId.zeroId());
          ru.setNewObjectId(commitId);
          ru.disableRefLog();
          if (ru.update(rw) != RefUpdate.Result.NEW) {
            throw new IOException(String.format(
                "Failed to create ref %s in %s: %s", ref,
                pc.getProject().getName(), ru.getResult()));
          }

          return getUrl(pc.getProject().getNameKey(), commitId.getName(), file);
        } finally {
          oi.release();
        }

      } finally {
        rw.release();
      }
    } finally {
      repo.close();
    }

  }

  private String getRef(byte[] content) {
    String id = new ObjectInserter.Formatter().idFor(
        Constants.OBJ_COMMIT, content).getName();
    StringBuilder ref = new StringBuilder();
    ref.append(Constants.R_REFS);
    ref.append("images/");
    ref.append(id.substring(0, 2));
    ref.append("/");
    ref.append(id.substring(2));
    return ref.toString();
  }

  private String getUrl(Project.NameKey project, String rev, String file) {
    StringBuilder url = new StringBuilder();
    url.append(canonicalWebUrl);
    if (!canonicalWebUrl.endsWith("/")) {
      url.append("src/");
    }
    url.append("src/");
    url.append(IdString.fromDecoded(project.get()).encoded());
    url.append("/rev/");
    url.append(IdString.fromDecoded(rev).encoded());
    url.append("/");
    url.append(IdString.fromDecoded(file).encoded());
    return url.toString();
  }

  public static class ImageInfo {
    public String url;

    public ImageInfo(String url) {
      this.url = url;
    }
  }
}
