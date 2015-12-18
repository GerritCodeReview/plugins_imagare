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
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ImageResourceRenderer;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageUploadScreen extends VerticalPanel {

  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Image Upload");
      screen.show(new ImageUploadScreen());
    }
  }

  static TextBox projectBox;
  static UploadStagePanel uploadStagePanel;
  static UploadedImagesPanel uploadedPanel;

  private final UploadByDropOrPastePanel uploadPanel;

  ImageUploadScreen() {
    setStyleName("imagare-image-upload-screen");

    HorizontalPanel p = new HorizontalPanel();
    p.setStyleName("imagare-menu-panel");
    Anchor prefsAnchor = new Anchor(new ImageResourceRenderer().render(
        ImagarePlugin.RESOURCES.preferences()),
        "#/x/" + Plugin.get().getPluginName() + "/preferences");
    prefsAnchor.setTitle("Edit Preferences");
    p.add(prefsAnchor);
    add(p);

    p = new HorizontalPanel();
    p.setStyleName("imagare-label-panel");
    p.add(new Label("Project"));
    Image projectInfo = new Image(ImagarePlugin.RESOURCES.info());
    projectInfo.setTitle("The project to which the images are uploaded.");
    p.add(projectInfo);
    p.add(new Label(":"));
    projectBox = new TextBox();
    p.add(projectBox);
    add(p);

    add(new UploadByFileSelection());
    uploadPanel = new UploadByDropOrPastePanel();
    add(uploadPanel);
    uploadStagePanel = new UploadStagePanel();
    add(uploadStagePanel);
    uploadedPanel = new UploadedImagesPanel();
    add(uploadedPanel);

    new RestApi("accounts").id("self").view(Plugin.get().getPluginName(), "preference")
        .get(new AsyncCallback<ConfigInfo>() {
          @Override
          public void onSuccess(ConfigInfo info) {
            ImageUploader.setStage(info.stage());
            String project = getParameter("project");
            if (project != null) {
              projectBox.setValue(project);
            } else {
              projectBox.setValue(info.getDefaultProject());
            }
            uploadPanel.focus();
          }

          @Override
          public void onFailure(Throwable caught) {
            // never invoked
          }
        });
  }

  private static String getParameter(String name) {
    List<String> values = getParameters().get(name);
    if (values == null) {
      return null;
    } else {
      return values.get(values.size() - 1);
    }
  }

  private static Map<String, List<String>> getParameters() {
    Map<String, List<String>> parameter = new HashMap<>();

    if (!Window.Location.getHash().contains("?")) {
      return parameter;
    }

    String queryString =
        Window.Location.getHash().substring(
            Window.Location.getHash().indexOf('?') + 1);
    for (String kvPair : queryString.split("&")) {
      String[] kv = kvPair.split("=", 2);
      if (kv[0].length() == 0) {
        continue;
      }

      List<String> values = parameter.get(kv[0]);
      if (values == null) {
        values = new ArrayList<>();
        parameter.put(kv[0], values);
      }
      values.add(kv.length > 1 ? URL.decodeQueryString(kv[1]) : "");
    }

    return parameter;
  }
}
