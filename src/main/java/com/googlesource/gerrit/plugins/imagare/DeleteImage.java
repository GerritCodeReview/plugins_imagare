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
import com.google.gerrit.extensions.api.access.PluginPermission;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.RefPermission;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.imagare.DeleteImage.Input;
import java.io.IOException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteImage implements RestModifyView<ImageResource, Input> {
  private static final Logger log = LoggerFactory.getLogger(DeleteImage.class);

  public static class Input {
  }

  private final String pluginName;
  private final Provider<IdentifiedUser> self;
  private final GitRepositoryManager repoManager;
  private final GitReferenceUpdated referenceUpdated;
  private final PermissionBackend permissionBackend;

  @Inject
  public DeleteImage(@PluginName String pluginName,
      Provider<IdentifiedUser> self,
      GitRepositoryManager repoManager,
      GitReferenceUpdated referenceUpdated,
      PermissionBackend permissionBackend) {
    this.pluginName = pluginName;
    this.self = self;
    this.repoManager = repoManager;
    this.referenceUpdated = referenceUpdated;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public Response<?> apply(ImageResource rsrc, Input input)
      throws AuthException, ResourceConflictException,
      RepositoryNotFoundException, IOException, ResourceNotFoundException,
      PermissionBackendException {

    if (!permissionBackend.user(self).ref(rsrc.getBranchKey()).testOrFalse(
        RefPermission.DELETE)) {
      permissionBackend.user(self).test(new PluginPermission(pluginName,
          DeleteOwnImagesCapability.DELETE_OWN_IMAGES));
    }

    try (Repository r = repoManager.openRepository(rsrc.getProject())) {
      if (!permissionBackend.user(self).ref(rsrc.getBranchKey()).testOrFalse(
          RefPermission.DELETE)) {
        validateOwnImage(r, rsrc.getRef());
      }

      RefUpdate.Result result;
      RefUpdate u;
      try {
        u = r.updateRef(rsrc.getRef());
        u.setForceUpdate(true);
        result = u.delete();
      } catch (IOException e) {
        log.error("Cannot delete " + rsrc.getRef(), e);
        throw e;
      }

      switch (result) {
        case NEW:
        case NO_CHANGE:
        case FAST_FORWARD:
        case FORCED:
          referenceUpdated.fire(rsrc.getProject(), u, self.get().getAccount());
          break;

        case REJECTED_CURRENT_BRANCH:
          log.warn("Cannot delete " + rsrc.getRef() + ": " + result.name());
          throw new ResourceConflictException("cannot delete current branch");

        case IO_FAILURE:
        case LOCK_FAILURE:
        case NOT_ATTEMPTED:
        case REJECTED:
        case RENAMED:
        default:
          log.error("Cannot delete " + rsrc.getRef() + ": " + result.name());
          throw new ResourceConflictException("cannot delete branch: " + result.name());
      }
    }
    return Response.none();
  }

  private void validateOwnImage(Repository repo, String ref)
      throws IOException, ResourceNotFoundException, AuthException {
    Ref r = repo.exactRef(ref);
    if (r == null) {
      throw new ResourceNotFoundException(ref);
    }
    try (RevWalk rw = new RevWalk(repo)) {
      RevCommit commit = rw.parseCommit(r.getObjectId());
      if (!self.get().getNameEmail()
          .equals(getNameEmail(commit.getCommitterIdent()))) {
        throw new AuthException("not allowed to delete image");
      }
    }
  }

  private String getNameEmail(PersonIdent ident) {
    return ident.getName() + " <" + ident.getEmailAddress() + ">";
  }
}
