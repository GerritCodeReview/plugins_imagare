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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class UploadStagePanel extends VerticalPanel {

  private Label uploadedImagesLabel;
  private FlowPanel stagedImagesPanel;
  private PopupPanel popup;

  UploadStagePanel() {
    setStyleName("imagare-upload-stage-panel");
    setVisible(false);

    uploadedImagesLabel = new Label("Staged Images:");
    uploadedImagesLabel.setStyleName("imagare-staged-images-label");
    add(uploadedImagesLabel);

    stagedImagesPanel = new FlowPanel();
    stagedImagesPanel.setStyleName("imagare-staged-images-panel");
    add(stagedImagesPanel);

    HorizontalPanel buttons = new HorizontalPanel();
    add(buttons);

    Button uploadButton = new Button("Upload");
    uploadButton.setStyleName("imagare-upload-button");
    uploadButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doUpload();
      }
    });
    buttons.add(uploadButton);

    Button cleanButton = new Button("Clean");
    cleanButton.setStyleName("imagare-clean-button");
    cleanButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        stagedImagesPanel.clear();
        setVisible(false);
      }
    });
    buttons.add(cleanButton);

    popup = new PopupPanel();
    popup.setVisible(false);
  }

  void add(String project, String dataUrl, String fileName) {
    setVisible(true);
    if (!isStaged(project, dataUrl, fileName)) {
      stagedImagesPanel.insert(new ImagePreview(project, dataUrl, fileName), 0);
    }
  }

  boolean isStaged(String project, String dataUrl, String fileName) {
    for (int i = 0; i < stagedImagesPanel.getWidgetCount(); i++) {
      ImagePreview ip = (ImagePreview)stagedImagesPanel.getWidget(i);
      if (project.equals(ip.project)
          && dataUrl.endsWith(ip.dataUrl)
          && (fileName != null ? fileName.equals(ip.fileName) : ip.fileName == null)) {
        return true;
      }
    }
    return false;
  }

  private void doUpload() {
    for (int i = 0; i < stagedImagesPanel.getWidgetCount(); i++) {
      ImagePreview ip = (ImagePreview)stagedImagesPanel.getWidget(i);
      ImageUploader.uploadImage(ip.project, ip.dataUrl, ip.fileName);
    }
    stagedImagesPanel.clear();
    setVisible(false);
  }

  private class ImagePreview extends VerticalPanel {
    final String project;
    final String dataUrl;
    final String fileName;

    private final Image img;
    private final Image deleteIcon;
    private Timer deleteIconHideTimer;

    ImagePreview(String project, final String dataUrl, String fileName) {
      this.project = project;
      this.dataUrl = dataUrl;
      this.fileName = fileName;

      setStyleName("imagare-stage-image-preview-panel");

      Label fileNameLabel = new Label(fileName != null ? fileName : "img.png");
      fileNameLabel.setStyleName("imagare-stage-image-title");
      add(fileNameLabel);

      img = new Image(dataUrl);
      img.setStyleName("imagare-stage-image-preview");
      add(img);

      deleteIcon = new Image(ImagarePlugin.RESOURCES.delete());
      deleteIcon.setStyleName("imagare-delete-icon");
      deleteIcon.setTitle("Delete Image");
      deleteIcon.setVisible(false);
      add(deleteIcon);

      img.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          if (!popup.isVisible()) {
            Image previewImg = new Image(dataUrl);
            previewImg.setStyleName("imagare-image-popup");
            popup.add(previewImg);

            popup.center();
            popup.setVisible(true);
          }

          cancelHideDeleteIcon();
          deleteIcon.getElement().setAttribute("style",
              deleteIcon.getElement().getAttribute("style")
                  + "position: absolute; top: " + img.getAbsoluteTop() + "px; "
                  + "left: " + img.getAbsoluteLeft() + "px;");
          deleteIcon.setVisible(true);
        }
      });
      img.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          popup.setVisible(false);
          popup.clear();
          hideDeleteIcon();
        }
      });

      deleteIcon.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          cancelHideDeleteIcon();
        }
      });

      deleteIcon.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          hideDeleteIcon();
        }
      });

      deleteIcon.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          stagedImagesPanel.remove(ImagePreview.this);
          UploadStagePanel.this.setVisible(stagedImagesPanel.getWidgetCount() != 0);
          popup.setVisible(false);
          popup.clear();
        }
      });

      Label projectLabel = new Label("Project:" + project);
      projectLabel.setStyleName("imagare-stage-label");
      add(projectLabel);
    }

    private void hideDeleteIcon() {
      deleteIconHideTimer = new Timer() {
        @Override
        public void run() {
          deleteIcon.setVisible(false);
        }
      };
      deleteIconHideTimer.schedule(20);
    }

    private void cancelHideDeleteIcon() {
      if (deleteIconHideTimer != null) {
        deleteIconHideTimer.cancel();
        deleteIconHideTimer = null;
      }
    }
  }
}
