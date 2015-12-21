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

import com.google.gerrit.plugin.client.rpc.RestApi;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public abstract class ImagareConfigScreen extends VerticalPanel {
  protected final boolean enableImageServer;
  private final RestApi restApi;

  private TextBox projectBox;
  private ListBox linkDecorationBox;
  private CheckBox stageBox;
  private Button saveButton;

  protected ImagareConfigScreen(boolean enableImageServer, RestApi restApi) {
    this.enableImageServer = enableImageServer;
    this.restApi = restApi;
    setStyleName("imagare-config-screen");
    restApi.get(new AsyncCallback<ConfigInfo>() {
        @Override
        public void onSuccess(ConfigInfo info) {
          display(info);
        }

        @Override
        public void onFailure(Throwable caught) {
          // never invoked
        }
      });
  }

  protected void display(ConfigInfo info) {
    HorizontalPanel p = new HorizontalPanel();
    p.setStyleName("imagare-label-panel");
    p.add(new Label("Link Decoration"));
    Image linkDecorationInfo = new Image(ImagarePlugin.RESOURCES.info());
    linkDecorationInfo.setTitle("Decoration for image links in the Gerrit WebUI."
        + " 'NONE': no decoration, 'TOOLTIP': the image is shown as tooltip on"
        + " mouse over an image link, 'INLINE': the image is inlined instead of"
        + " the URL.");
    p.add(linkDecorationInfo);
    p.add(new Label(":"));
    linkDecorationBox = new ListBox();
    int i = 0;
    for (LinkDecoration v : LinkDecoration.values()) {
      linkDecorationBox.addItem(v.name());
      if (v.equals(info.getLinkDecoration())) {
        linkDecorationBox.setSelectedIndex(i);
      }
      i++;
    }
    p.add(linkDecorationBox);
    add(p);

    if (enableImageServer) {
      p = new HorizontalPanel();
      p.setStyleName("imagare-label-panel");
      p.add(new Label("Project"));
      Image projectInfo = new Image(ImagarePlugin.RESOURCES.info());
      projectInfo.setTitle("The default project for the image upload.");
      p.add(projectInfo);
      p.add(new Label(":"));
      projectBox = new TextBox();
      projectBox.setValue(info.getDefaultProject());
      p.add(projectBox);
      add(p);

      p = new HorizontalPanel();
      p.setStyleName("imagare-label-panel");
      stageBox = new CheckBox("Stage images before upload");
      stageBox.setValue(info.stage());
      p.add(stageBox);
      Image stageInfo = new Image(ImagarePlugin.RESOURCES.info());
      stageInfo.setTitle("Images are not uploaded immediately but put into a "
          + "staging area. The upload must be triggered explicitely.");
      p.add(stageInfo);
      add(p);
    }

    HorizontalPanel buttons = new HorizontalPanel();
    add(buttons);

    saveButton = new Button("Save");
    saveButton.setStyleName("imagare-save-button");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doSave();
      }
    });
    buttons.add(saveButton);
    saveButton.setEnabled(false);
    OnEditEnabler onEditEnabler = new OnEditEnabler(saveButton, linkDecorationBox);
    if (enableImageServer) {
      onEditEnabler.listenTo(projectBox);
      onEditEnabler.listenTo(stageBox);
    }

    projectBox.setFocus(true);
    saveButton.setEnabled(false);
  }

  private void doSave() {
    ConfigInfo in = ConfigInfo.create();
    in.setLinkDecoration(linkDecorationBox.getValue(linkDecorationBox.getSelectedIndex()));
    if (enableImageServer) {
      in.setDefaultProject(projectBox.getValue());
      in.setStage(stageBox.getValue());
    }
    restApi.put(in, new AsyncCallback<JavaScriptObject>() {
        @Override
        public void onSuccess(JavaScriptObject result) {
          saveButton.setEnabled(false);
        }

        @Override
        public void onFailure(Throwable caught) {
          // never invoked
        }
      });
  }
}
