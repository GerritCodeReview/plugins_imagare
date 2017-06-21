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
import com.google.gerrit.plugin.client.PluginEntryPoint;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ImagarePlugin extends PluginEntryPoint {
  public static final Resources RESOURCES = GWT.create(Resources.class);

  @Override
  public void onPluginLoad() {
    new RestApi("config")
        .id("server")
        .view(Plugin.get().getPluginName(), "config")
        .get(
            new AsyncCallback<ConfigInfo>() {
              @Override
              public void onSuccess(ConfigInfo info) {
                if (info.enableImageServer()) {
                  Plugin.get().screen("upload", new ImageUploadScreen.Factory());
                }

                Plugin.get()
                    .screen("settings", new ImagareAdminScreen.Factory(info.enableImageServer()));
                Plugin.get()
                    .settingsScreen(
                        "preferences",
                        Plugin.get().getName() + " Preferences",
                        new ImagarePreferenceScreen.Factory(info.enableImageServer()));
              }

              @Override
              public void onFailure(Throwable caught) {
                // never invoked
              }
            });
  }
}
