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
import com.google.gwt.user.client.ui.InlineHyperlink;

public class ImagarePreferenceScreen extends ImagareConfigScreen {

  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Imagare Preferences");
      screen.show(new ImagarePreferenceScreen());
    }
  }

  ImagarePreferenceScreen() {
    super(new RestApi("accounts").id("self")
        .view(Plugin.get().getPluginName(), "preference"));
  }

  @Override
  protected void display(ConfigInfo info) {
    super.display(info);

    InlineHyperlink uploadLink =
        new InlineHyperlink("Upload Image", "/x/"
            + Plugin.get().getPluginName() + "/upload");
    add(uploadLink);
  }
}
