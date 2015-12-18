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

import com.google.common.base.Strings;
import com.google.gerrit.common.data.GlobalCapability;
import com.google.gerrit.extensions.annotations.CapabilityScope;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.annotations.RequiresCapability;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.UnprocessableEntityException;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.project.ProjectCache;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.imagare.PutConfig.Input;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import java.io.IOException;

@RequiresCapability(value = GlobalCapability.ADMINISTRATE_SERVER, scope = CapabilityScope.CORE)
public class PutConfig implements RestModifyView<ConfigResource, Input>{
  public static class Input {
    public String defaultProject;
    public LinkDecoration linkDecoration;
    public Boolean stage;
  }

  private final PluginConfigFactory cfgFactory;
  private final SitePaths sitePaths;
  private final String pluginName;
  private final ProjectCache projectCache;

  @Inject
  PutConfig(PluginConfigFactory cfgFactory, SitePaths sitePaths,
      @PluginName String pluginName, ProjectCache projectCache) {
    this.cfgFactory = cfgFactory;
    this.sitePaths = sitePaths;
    this.pluginName = pluginName;
    this.projectCache = projectCache;
  }

  @Override
  public Response<String> apply(ConfigResource rsrc, Input input)
      throws IOException, ConfigInvalidException, UnprocessableEntityException {
    if (input == null) {
      input = new Input();
    }
    FileBasedConfig cfg =
        new FileBasedConfig(sitePaths.gerrit_config.toFile(), FS.DETECTED);
    cfg.load();

    if (input.defaultProject != null) {
      if (projectCache.get(new Project.NameKey(input.defaultProject)) == null) {
        throw new UnprocessableEntityException("project '"
            + input.defaultProject + "' does not exist");
      }
      cfg.setString("plugin", pluginName, "defaultProject",
          Strings.emptyToNull(input.defaultProject));
    }

    if (input.linkDecoration != null) {
      if (LinkDecoration.NONE.equals(input.linkDecoration)) {
        cfg.unset("plugin", pluginName, "linkDecoration");
      } else {
        cfg.setEnum("plugin", pluginName, "linkDecoration",
            input.linkDecoration);
      }
    }

    if (input.stage != null) {
      if (input.stage) {
        cfg.setBoolean("plugin", pluginName, "stage", input.stage);
      } else {
        cfg.unset("plugin", pluginName, "stage");
      }
    }

    cfg.save();
    cfgFactory.getFromGerritConfig(pluginName, true);
    return Response.<String> ok("OK");
  }
}
