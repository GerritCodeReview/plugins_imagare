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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ImageUploader {
  private static boolean stage;

  public static void setStage(boolean s) {
    stage = s;
  }

  public static final void stageImage(String imageData) {
    stageImage(imageData, null);
  }

  public static final void stageImage(String imageData, String fileName) {
    stageImage(ImageUploadScreen.projectBox.getValue(), imageData, fileName);
  }

  public static final void stageImage(String project, String imageData, String fileName) {
    if (stage) {
      ImageUploadScreen.uploadStagePanel.add(project, imageData, fileName);
    } else {
      uploadImage(project, imageData, fileName);
    }
  }

  public static final void uploadImage(String imageData) {
    uploadImage(imageData, null);
  }

  public static final void uploadImage(String imageData, String fileName) {
    uploadImage(ImageUploadScreen.projectBox.getValue(), imageData, fileName);
  }

  public static final void uploadImage(String project, String imageData, String fileName) {
    ImageInput in = ImageInput.create();
    in.image_data(imageData);
    in.file_name(fileName);

    new RestApi("projects").id(project).view(Plugin.get().getPluginName(), "images")
        .post(in, new AsyncCallback<ImageInfo>() {

          @Override
          public void onSuccess(ImageInfo result) {
            ImageUploadScreen.uploadedPanel.add(result.url());
          }

          @Override
          public void onFailure(Throwable caught) {
          }
        });
  }

  private static class ImageInput extends JavaScriptObject {
    final native void image_data(String d) /*-{ this.image_data = d; }-*/;
    final native void file_name(String n) /*-{ this.file_name = n; }-*/;

    static ImageInput create() {
      return (ImageInput) createObject();
    }

    protected ImageInput() {
    }
  }

  private static class ImageInfo extends JavaScriptObject {
    final native String url() /*-{ return this.url }-*/;

    protected ImageInfo() {
    }
  }
}