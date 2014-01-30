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
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.clippy.client.CopyableLabel;

public class UploadedImagesPanel extends VerticalPanel {

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

    Panel p = new HorizontalPanel();
    insert(p, 1);

    Image img = new Image(url);
    img.setStyleName("imagare-image-preview");
    p.add(img);

    img.addMouseOverHandler(new MouseOverHandler() {
      @Override
      public void onMouseOver(MouseOverEvent event) {
        if (!popup.isVisible()) {
          Image img = new Image(url);
          img.setStyleName("imagare-image-popup");
          popup.add(img);

          popup.center();
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

    p.add(new CopyableLabel(url));
  }
}
