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
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ImageResourceRenderer;

public class ImagarePreferenceScreen extends ImagareConfigScreen {

  static class Factory implements Screen.EntryPoint {
    private final boolean enableImageServer;

    Factory(boolean enableImageServer) {
      this.enableImageServer = enableImageServer;
    }

    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle(Plugin.get().getName() + " Preferences");
      screen.show(new ImagarePreferenceScreen(enableImageServer));
    }
  }

  ImagarePreferenceScreen(boolean enableImageServer) {
    super(
        enableImageServer,
        new RestApi("accounts").id("self").view(Plugin.get().getPluginName(), "preference"));
  }

  @Override
  protected void display(ConfigInfo info) {
    if (enableImageServer) {
      HorizontalPanel p = new HorizontalPanel();
      p.setStyleName("imagare-menu-panel");
      Anchor uploadAnchor =
          new Anchor(
              new ImageResourceRenderer().render(ImagarePlugin.RESOURCES.image()),
              "#/x/" + Plugin.get().getPluginName() + "/upload");
      uploadAnchor.setTitle("Upload Image");
      p.add(uploadAnchor);
      add(p);
    }

    super.display(info);
  }

  @Override
  protected void onSave() {
    super.onSave();
    Cookies.removeCookie(Plugin.get().getPluginName() + "~prefs");
  }
}
