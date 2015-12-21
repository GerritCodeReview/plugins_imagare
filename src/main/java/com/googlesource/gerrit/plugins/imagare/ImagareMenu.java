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
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImagareMenu implements TopMenu {
  private final List<MenuEntry> menuEntries;

  @Inject
  public ImagareMenu(PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    menuEntries = new ArrayList<>();
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName, true);
    if (cfg.getBoolean("enableImageServer", true)) {
      menuEntries.add(new MenuEntry("Tools", Collections
          .singletonList(new MenuItem("Image Upload", "#/x/" + pluginName + "/upload", ""))));
    } else {
      String uploadUrl = cfg.getString("uploadUrl");
      if (uploadUrl != null) {
        menuEntries.add(new MenuEntry("Tools", Collections
            .singletonList(new MenuItem("Image Upload", uploadUrl))));
      }
    }
  }

  @Override
  public List<MenuEntry> getEntries() {
    return menuEntries;
  }
}
