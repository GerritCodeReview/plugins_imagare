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

package com.googlesource.gerrit.plugins.imagare.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gerrit.plugin.client.screen.Screen;

public class ImagareAdminScreen extends ImagareConfigScreen {

  static class Factory implements Screen.EntryPoint {
    private final boolean enableImageServer;

    Factory(boolean enableImageServer) {
      this.enableImageServer = enableImageServer;
    }

    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle(Plugin.get().getName() + "Admin");
      screen.show(new ImagareAdminScreen(enableImageServer));
    }
  }

  ImagareAdminScreen(boolean enableImageServer) {
    super(
        enableImageServer,
        new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config"));
  }
}
