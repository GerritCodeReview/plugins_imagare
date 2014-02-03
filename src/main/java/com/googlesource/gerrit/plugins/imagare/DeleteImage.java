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

import com.google.gerrit.common.ChangeHooks;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.extensions.events.GitReferenceUpdated;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.googlesource.gerrit.plugins.imagare.DeleteImage.Input;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DeleteImage implements RestModifyView<ImageResource, Input> {
  private static final Logger log = LoggerFactory.getLogger(DeleteImage.class);

  public static class Input {
  }

  private final Provider<IdentifiedUser> self;
  private final GitRepositoryManager repoManager;
  private final GitReferenceUpdated referenceUpdated;
  private final ChangeHooks hooks;

  @Inject
  public DeleteImage(Provider<IdentifiedUser> self,
      GitRepositoryManager repoManager, GitReferenceUpdated referenceUpdated,
      ChangeHooks hooks) {
    this.self = self;
    this.repoManager = repoManager;
    this.referenceUpdated = referenceUpdated;
    this.hooks = hooks;
  }

  @Override
  public Response<?> apply(ImageResource rsrc, Input input)
      throws AuthException, ResourceConflictException,
      RepositoryNotFoundException, IOException {
    if (!rsrc.getControl().canDelete()) {
      throw new AuthException("not allowed to delete image");
    }

    Repository r = repoManager.openRepository(rsrc.getProject());
    try {
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
          referenceUpdated.fire(rsrc.getProject(), u);
          hooks.doRefUpdatedHook(rsrc.getBranchKey(), u, self.get().getAccount());
          break;

        case REJECTED_CURRENT_BRANCH:
          log.warn("Cannot delete " + rsrc.getRef() + ": " + result.name());
          throw new ResourceConflictException("cannot delete current branch");

        default:
          log.error("Cannot delete " + rsrc.getRef() + ": " + result.name());
          throw new ResourceConflictException("cannot delete branch: " + result.name());
      }
    } finally {
      r.close();
    }
    return Response.none();
  }
}
