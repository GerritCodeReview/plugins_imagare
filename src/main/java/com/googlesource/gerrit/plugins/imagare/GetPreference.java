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

import com.google.common.base.MoreObjects;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.googlesource.gerrit.plugins.imagare.GetConfig.ConfigInfo;
import org.eclipse.jgit.lib.Config;

public class GetPreference implements RestReadView<AccountResource> {
  public static final String PREFERENCE = "preference";
  public static final String KEY_DEFAULT_PROJECT = "defaultProject";
  public static final String KEY_LINK_DECORATION = "linkDecoration";
  public static final String KEY_STAGE = "stage";

  private final Provider<IdentifiedUser> self;
  private final ProjectCache projectCache;
  private final String pluginName;
  private final Provider<GetConfig> getConfig;

  @Inject
  GetPreference(
      Provider<IdentifiedUser> self,
      ProjectCache projectCache,
      @PluginName String pluginName,
      Provider<GetConfig> getConfig) {
    this.self = self;
    this.projectCache = projectCache;
    this.pluginName = pluginName;
    this.getConfig = getConfig;
  }

  @Override
  public ConfigInfo apply(AccountResource rsrc) throws AuthException {
    if (self.get() != rsrc.getUser() && !self.get().getCapabilities().canAdministrateServer()) {
      throw new AuthException("not allowed to get preference");
    }

    String username = self.get().getUserName();

    ConfigInfo globalCfg = getConfig.get().apply(new ConfigResource());

    Config db = projectCache.getAllProjects().getConfig(pluginName + ".config").get();
    ConfigInfo info = new ConfigInfo();

    info.defaultProject =
        MoreObjects.firstNonNull(
            db.getString(PREFERENCE, username, KEY_DEFAULT_PROJECT), globalCfg.defaultProject);

    info.linkDecoration =
        db.getEnum(
            PREFERENCE,
            username,
            KEY_LINK_DECORATION,
            MoreObjects.firstNonNull(globalCfg.linkDecoration, LinkDecoration.NONE));
    if (LinkDecoration.NONE.equals(info.linkDecoration)) {
      info.linkDecoration = null;
    }

    info.stage =
        db.getBoolean(
            PREFERENCE, username, KEY_STAGE, (globalCfg.stage != null ? globalCfg.stage : false));
    if (!info.stage) {
      info.stage = null;
    }

    info.pattern = globalCfg.pattern;

    return info;
  }
}
