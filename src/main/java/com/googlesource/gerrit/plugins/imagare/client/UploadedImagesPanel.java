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
import com.google.gerrit.plugin.client.rpc.NoContent;
import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwtexpui.clippy.client.CopyableLabel;
import com.google.gwtexpui.safehtml.client.SafeHtmlBuilder;

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
    private final Image img;
    private final Image fullScreenIcon;
    private Timer fullScreenIconHideTimer;
    private final Image deleteIcon;
    private Timer deleteIconHideTimer;

    ImagePreview(final String url) {
      setStyleName("imagare-uploaded-image-preview-panel");

      int revIndex = url.indexOf("/rev/");
      int projectIndex = url.lastIndexOf('/', revIndex - 1);
      int fileIndex = url.indexOf('/', revIndex + 5);
      final String project = url.substring(projectIndex + 1, revIndex);
      final String ref = url.substring(revIndex + 5, fileIndex);
      final String fileName = url.substring(fileIndex + 1).replaceAll("\\+", " ");
      Label fileNameLabel = new Label(fileName);
      fileNameLabel.setStyleName("imagare-uploaded-image-title");
      add(fileNameLabel);

      img = new Image(url);
      img.setStyleName("imagare-uploaded-image-preview");
      add(img);

      fullScreenIcon = new Image(ImagarePlugin.RESOURCES.fullScreen());
      fullScreenIcon.setStyleName("imagare-fullscreen-icon");
      fullScreenIcon.setTitle("Full Screen");
      fullScreenIcon.setVisible(false);
      add(fullScreenIcon);

      deleteIcon = new Image(ImagarePlugin.RESOURCES.delete());
      deleteIcon.setStyleName("imagare-delete-icon");
      deleteIcon.setTitle("Delete Image");
      deleteIcon.setVisible(false);
      add(deleteIcon);

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

          cancelHideFullScreenIcon();
          fullScreenIcon.getElement().setAttribute("style",
              fullScreenIcon.getElement().getAttribute("style")
                  + "position: absolute; top: " + img.getAbsoluteTop() + "px; "
                  + "left: " + img.getAbsoluteLeft() + "px;");
          fullScreenIcon.setVisible(true);

          cancelHideDeleteIcon();
          deleteIcon.getElement().setAttribute("style",
              deleteIcon.getElement().getAttribute("style")
                  + "position: absolute; top: " + img.getAbsoluteTop() + "px; "
                  + "left: " + (img.getAbsoluteLeft() + fullScreenIcon.getWidth()) + "px;");
          deleteIcon.setVisible(true);
        }
      });
      img.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          popup.setVisible(false);
          popup.clear();
          hideFullScreenIcon();
          hideDeleteIcon();
        }
      });

      fullScreenIcon.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          cancelHideFullScreenIcon();
          cancelHideDeleteIcon();
        }
      });

      fullScreenIcon.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          hideFullScreenIcon();
          hideDeleteIcon();
        }
      });

      fullScreenIcon.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          Window.open(url, "_blank", "");
          popup.setVisible(false);
          popup.clear();
        }
      });

      deleteIcon.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          cancelHideFullScreenIcon();
          cancelHideDeleteIcon();
        }
      });

      deleteIcon.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          hideDeleteIcon();
          hideFullScreenIcon();
        }
      });

      deleteIcon.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          new ConfirmationDialog("Delete Image",
              new SafeHtmlBuilder().append("Are you sure you want to delete '" + fileName + "'?").br().br(),
              new ConfirmationCallback() {
                @Override
                public void onOk() {
                  new RestApi("projects").view(project)
                      .view(Plugin.get().getPluginName(), "images").view(ref)
                      .delete(new AsyncCallback<NoContent>() {
                        @Override
                        public void onSuccess(NoContent info) {
                          UploadedImagesPanel.this.remove(ImagePreview.this);
                          UploadedImagesPanel.this.setVisible(
                              UploadedImagesPanel.this.getWidgetCount() > 1);

                          popup.setVisible(false);
                          popup.clear();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                          // never invoked
                        }
                      });
                }
              }).center();
        }
      });

      CopyableLabel copyLabel = new CopyableLabel(url);
      copyLabel.setStyleName("imagare-uploaded-copy-label");
      add(copyLabel);
    }

    private void hideFullScreenIcon() {
      fullScreenIconHideTimer = new Timer() {
        @Override
        public void run() {
          fullScreenIcon.setVisible(false);
        }
      };
      fullScreenIconHideTimer.schedule(20);
    }

    private void cancelHideFullScreenIcon() {
      if (fullScreenIconHideTimer != null) {
        fullScreenIconHideTimer.cancel();
        fullScreenIconHideTimer = null;
      }
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
