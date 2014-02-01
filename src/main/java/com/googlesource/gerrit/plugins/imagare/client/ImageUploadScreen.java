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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ImageUploadScreen extends VerticalPanel {

  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Image Upload");
      screen.show(new ImageUploadScreen());
    }
  }

  static TextBox projectBox;
  static UploadedImagesPanel uploadedPanel;

  ImageUploadScreen() {
    setStyleName("imagare-image-upload-screen");

    HorizontalPanel p = new HorizontalPanel();
    p.setStyleName("imagare-project-panel");
    p.add(new Label("Project:"));
    projectBox = new TextBox();
    p.add(projectBox);
    add(p);

    add(new UploadByFileSelection());
    add(new UploadByDropOrPastePanel());
    uploadedPanel = new UploadedImagesPanel();
    add(uploadedPanel);

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
        .get(new AsyncCallback<ConfigInfo>() {
          @Override
          public void onSuccess(ConfigInfo info) {
            projectBox.setValue(info.getDefaultProject());
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }
}
