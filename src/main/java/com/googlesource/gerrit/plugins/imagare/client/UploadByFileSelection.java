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

import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;

public class UploadByFileSelection extends HorizontalPanel {
  UploadByFileSelection() {
    this("All-Projects");
  }

  UploadByFileSelection(String project) {
    init0();

    FormPanel form = new FormPanel();
    FileUpload upload = new FileUpload();
    upload.setName("Select Image");
    upload.getElement().setAttribute("onChange", "imagareSubmit(event)");
    form.add(upload);

    add(form);
  }

  private static native void init0() /*-{
    $wnd.imagareSubmit = function submit(event) {
      var f = event.target.files[0];
      if (f) {
        var r = new FileReader();
        r.onload = function(e) {
          if (f.type.match('image/.*')) {
            @com.googlesource.gerrit.plugins.imagare.client.ImageUploader::uploadImage(Ljava/lang/String;Ljava/lang/String;)(e.target.result, f.name);
          } else {
            $wnd.Gerrit.showError('no image file');
          }
        }
        r.readAsDataURL(f);
      }
    }
  }-*/;
}
