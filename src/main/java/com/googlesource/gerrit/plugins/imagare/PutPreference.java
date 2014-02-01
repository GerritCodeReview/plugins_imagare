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

import static com.googlesource.gerrit.plugins.imagare.GetPreference.PREFERENCE;
import static com.googlesource.gerrit.plugins.imagare.GetPreference.KEY_DEFAULT_PROJECT;
import static com.googlesource.gerrit.plugins.imagare.GetPreference.KEY_LINK_DECORATION;

import com.google.common.base.Strings;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.git.MetaDataUpdate;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.googlesource.gerrit.plugins.imagare.PutConfig.Input;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Config;

import java.io.IOException;

public class PutPreference implements RestModifyView<AccountResource, Input> {
  private final Provider<IdentifiedUser> self;
  private final ProjectCache projectCache;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final String pluginName;

  @Inject
  PutPreference(Provider<IdentifiedUser> self, ProjectCache projectCache,
      MetaDataUpdate.User metaDataUpdateFactory, @PluginName String pluginName) {
    this.self = self;
    this.projectCache = projectCache;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.pluginName = pluginName;
  }

  @Override
  public Response<String> apply(AccountResource rsrc, Input input)
      throws AuthException, RepositoryNotFoundException, IOException {
    if (self.get() != rsrc.getUser()
        && !self.get().getCapabilities().canAdministrateServer()) {
      throw new AuthException("not allowed to change preference");
    }
    if (input == null) {
      input = new Input();
    }

    String username = self.get().getUserName();

    ProjectLevelConfig storage =
        projectCache.getAllProjects().getConfig(pluginName + ".config");
    Config db = storage.get();
    boolean modified = false;

    String defaultProject = db.getString(PREFERENCE, username, KEY_DEFAULT_PROJECT);
    if (Strings.emptyToNull(input.defaultProject) != null) {
      if (!input.defaultProject.equals(defaultProject)) {
        db.setString(PREFERENCE, username, KEY_DEFAULT_PROJECT,
            input.defaultProject);
        modified = true;
      }
    } else {
      if (defaultProject != null) {
        db.unset(PREFERENCE, username, KEY_DEFAULT_PROJECT);
        modified = true;
      }
    }

    if (input.linkDecoration != null) {
      LinkDecoration linkDecoration =
          db.getEnum(PREFERENCE, username, KEY_LINK_DECORATION,
              LinkDecoration.NONE);
      if (!input.linkDecoration.equals(linkDecoration)) {
        db.setEnum(PREFERENCE, username, KEY_LINK_DECORATION,
            input.linkDecoration);
        modified = true;
      }
    } else {
      if (db.getNames(PREFERENCE, username).contains(KEY_LINK_DECORATION)) {
        db.unset(PREFERENCE, username, KEY_LINK_DECORATION);
        modified = true;
      }
    }

    if (modified) {
      MetaDataUpdate md = metaDataUpdateFactory.create(
          projectCache.getAllProjects().getProject().getNameKey());
      md.setMessage("Update " + pluginName + " Preferences for '"
          + username + "'\n");
      storage.commit(md);
    }

    return Response.<String> ok("OK");
  }
}
