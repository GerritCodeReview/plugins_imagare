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

import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.clippy.client.CopyableLabel;

public class UploadedImagesPanel extends FlowPanel {

  private Label uploadedImagesLabel;
  private PopupPanel popup;

  UploadedImagesPanel() {
    setStyleName("imagare-uploaded-images-panel");

    uploadedImagesLabel = new Label("Uploaded Images:");
    uploadedImagesLabel.setStyleName("imagare-uploaded-images-label");
    uploadedImagesLabel.setVisible(false);
    add(uploadedImagesLabel);

    popup = new PopupPanel();
    popup.setVisible(false);
  }

  void add(final String url) {
    uploadedImagesLabel.setVisible(true);
    insert(new ImagePreview(url), 1);
  }

  private class ImagePreview extends VerticalPanel {
    ImagePreview(final String url) {
      setStyleName("imagare-uploaded-image-preview-panel");

      String fileName = url.substring(url.indexOf('/', url.indexOf("/rev/") + 5) + 1);
      fileName = fileName.replaceAll("\\+", " ");
      Label fileNameLabel = new Label(fileName);
      fileNameLabel.setStyleName("imagare-uploaded-image-title");
      add(fileNameLabel);

      final Image img = new Image(url);
      img.setStyleName("imagare-uploaded-image-preview");
      add(img);

      img.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          if (!popup.isVisible()) {
            Image previewImg = new Image(url);
            previewImg.setStyleName("imagare-image-popup");
            previewImg.getElement().setAttribute("style",
                previewImg.getElement().getAttribute("style")
                    + "position: absolute; top: " + (img.getAbsoluteTop() + img.getHeight() + 20) + "px; "
                    + "left: " + img.getAbsoluteLeft() + "px;");
            popup.add(previewImg);

            popup.show();
            popup.setVisible(true);
          }
        }
      });
      img.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          popup.setVisible(false);
          popup.clear();
        }
      });

      CopyableLabel copyLabel = new CopyableLabel(url);
      copyLabel.setStyleName("imagare-uploaded-copy-label");
      add(copyLabel);
    }
  }
}
