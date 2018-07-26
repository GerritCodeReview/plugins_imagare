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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.MethodNotAllowedException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.reviewdb.client.Branch;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.GerritPersonIdent;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.mime.FileTypeRegistry;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.project.CreateRefControl;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.imagare.PostImage.Input;
import eu.medsea.mimeutil.MimeType;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.Config;
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

public class PostImage implements RestModifyView<ProjectResource, Input> {

  public static class Input {
    public String imageData;
    public String fileName;
  }

  private final FileTypeRegistry registry;
  private final Pattern imageDataPattern;
  private final Provider<IdentifiedUser> self;
  private final GitRepositoryManager repoManager;
  private final GitReferenceUpdated referenceUpdated;
  private final PersonIdent myIdent;
  private final String canonicalWebUrl;
  private final Config cfg;
  private final String pluginName;
  private final CreateRefControl createRefControl;

  @Inject
  public PostImage(
      FileTypeRegistry registry,
      Provider<IdentifiedUser> self,
      GitRepositoryManager repoManager,
      GitReferenceUpdated referenceUpdated,
      @GerritPersonIdent PersonIdent myIdent,
      @CanonicalWebUrl String canonicalWebUrl,
      @GerritServerConfig Config cfg,
      @PluginName String pluginName,
      CreateRefControl createRefControl) {
    this.registry = registry;
    this.imageDataPattern = Pattern.compile("data:([\\w/.-]+);([\\w]+),(.*)");
    this.self = self;
    this.repoManager = repoManager;
    this.referenceUpdated = referenceUpdated;
    this.myIdent = myIdent;
    this.canonicalWebUrl = canonicalWebUrl;
    this.cfg = cfg;
    this.pluginName = pluginName;
    this.createRefControl = createRefControl;
  }

  @Override
  public Response<ImageInfo> apply(ProjectResource rsrc, Input input)
      throws MethodNotAllowedException, BadRequestException, AuthException, IOException,
          ResourceConflictException {
    if (input == null) {
      input = new Input();
    }
    ImageInfo info;
    if (input.imageData != null) {
      info = storeImage(rsrc.getControl(), input.imageData, input.fileName);
    } else {
      throw new BadRequestException("no image data");
    }
    return Response.created(info);
  }

  private ImageInfo storeImage(ProjectControl pc, String imageData, String fileName)
      throws MethodNotAllowedException, BadRequestException, AuthException, IOException,
          ResourceConflictException {
    Matcher m = imageDataPattern.matcher(imageData);
    if (m.matches()) {
      String receivedMimeType = m.group(1);
      String encoding = m.group(2);
      String encodedContent = m.group(3);

      if (fileName == null) {
        int pos = receivedMimeType.indexOf('/');
        if (pos > 0 && pos + 1 < receivedMimeType.length()) {
          fileName = "img." + receivedMimeType.substring(pos + 1);
        } else {
          throw new BadRequestException("bad mime type: " + receivedMimeType);
        }
      }

      if ("base64".equals(encoding)) {
        byte[] content = Base64.decode(encodedContent);
        MimeType mimeType = registry.getMimeType("img." + receivedMimeType, content);
        if (!"image".equals(mimeType.getMediaType())) {
          throw new MethodNotAllowedException("no image");
        }
        if (!receivedMimeType.equals(mimeType.toString())) {
          throw new BadRequestException("incorrect mime type");
        }
        return new ImageInfo(storeImage(pc, content, fileName));
      }
      throw new MethodNotAllowedException("unsupported encoding");
    }
    throw new BadRequestException("invalid image data");
  }

  private String storeImage(ProjectControl pc, byte[] content, String fileName)
      throws AuthException, IOException, ResourceConflictException {
    long maxSize = getEffectiveMaxObjectSizeLimit(pc.getProjectState());
    // maxSize == 0 means that there is no limit
    if (maxSize > 0 && content.length > maxSize) {
      throw new ResourceConflictException("image too large");
    }

    String ref = getRef(content, fileName);

    try (Repository repo = repoManager.openRepository(pc.getProject().getNameKey())) {
      ObjectId commitId = repo.resolve(ref);
      if (commitId != null) {
        // this image exists already
        return getUrl(pc.getProject().getNameKey(), ref, fileName);
      }

      try (RevWalk rw = new RevWalk(repo);
          ObjectInserter oi = repo.newObjectInserter()) {
        ObjectId blobId = oi.insert(Constants.OBJ_BLOB, content);
        oi.flush();

        TreeFormatter tf = new TreeFormatter();
        tf.append(fileName, FileMode.REGULAR_FILE, blobId);
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

        try {
          createRefControl.checkCreateRef(
              self,
              repo,
              new Branch.NameKey(pc.getProject().getNameKey(), ref),
              rw.parseCommit(commitId));
        } catch (PermissionBackendException | AuthException | NoSuchProjectException e) {
          throw new AuthException(
              String.format("Project %s doesn't allow image upload.", pc.getProject().getName()));
        }

        RefUpdate ru = repo.updateRef(ref);
        ru.setExpectedOldObjectId(ObjectId.zeroId());
        ru.setNewObjectId(commitId);
        ru.disableRefLog();
        if (ru.update(rw) == RefUpdate.Result.NEW) {
          referenceUpdated.fire(pc.getProject().getNameKey(), ru, self.get().getAccount());
        } else {
          throw new IOException(
              String.format(
                  "Failed to create ref %s in %s: %s",
                  ref, pc.getProject().getName(), ru.getResult()));
        }

        return getUrl(pc.getProject().getNameKey(), ref, fileName);
      }
    }
  }

  private String getRef(byte[] content, String fileName) {
    try (ObjectInserter oi = new ObjectInserter.Formatter()) {
      String id =
          oi.idFor(Constants.OBJ_BLOB, ArrayUtils.addAll(content, fileName.getBytes())).getName();
      StringBuilder ref = new StringBuilder();
      ref.append(Constants.R_REFS);
      ref.append("images/");
      ref.append(id.substring(0, 2));
      ref.append("/");
      ref.append(id.substring(2));
      return ref.toString();
    }
  }

  private String getUrl(Project.NameKey project, String rev, String fileName) {
    StringBuilder url = new StringBuilder();
    url.append(canonicalWebUrl);
    if (!canonicalWebUrl.endsWith("/")) {
      url.append("/");
    }
    url.append("plugins/");
    url.append(IdString.fromDecoded(pluginName).encoded());
    url.append("/project/");
    url.append(IdString.fromDecoded(project.get()).encoded());
    url.append("/rev/");
    url.append(IdString.fromDecoded(rev).encoded());
    url.append("/");
    url.append(IdString.fromDecoded(fileName).encoded());
    return url.toString();
  }

  private long getEffectiveMaxObjectSizeLimit(ProjectState p) {
    long global = cfg.getLong("receive", "maxObjectSizeLimit", 0);
    long local = p.getMaxObjectSizeLimit();
    if (global > 0 && local > 0) {
      return Math.min(global, local);
    }
    // zero means "no limit", in this case the max is more limiting
    return Math.max(global, local);
  }

  public static class ImageInfo {
    public String url;

    public ImageInfo(String url) {
      this.url = url;
    }
  }
}
