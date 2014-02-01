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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ImagareAdminScreen extends VerticalPanel {

  static class Factory implements Screen.EntryPoint {
    @Override
    public void onLoad(Screen screen) {
      screen.setPageTitle("Imagare Admin");
      screen.show(new ImagareAdminScreen());
    }
  }

  private TextBox projectBox;
  private ListBox linkDecorationBox;
  private Button saveButton;

  ImagareAdminScreen() {
    setStyleName("imagare-admin-screen");

    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
        .get(new AsyncCallback<ConfigInfo>() {
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

  private void display(ConfigInfo info) {
    HorizontalPanel p = new HorizontalPanel();
    p.setStyleName("imagare-label-panel");
    p.add(new Label("Project:"));
    projectBox = new TextBox();
    projectBox.setValue(info.getDefaultProject());
    p.add(projectBox);
    add(p);

    p = new HorizontalPanel();
    p.setStyleName("imagare-label-panel");
    p.add(new Label("Link Decoration:"));
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

    HorizontalPanel buttons = new HorizontalPanel();
    add(buttons);

    saveButton = new Button("Save");
    saveButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(final ClickEvent event) {
        doSave();
      }
    });
    buttons.add(saveButton);
    saveButton.setEnabled(false);
    OnEditEnabler onEditEnabler = new OnEditEnabler(saveButton, projectBox);
    onEditEnabler.listenTo(linkDecorationBox);

    projectBox.setFocus(true);
    saveButton.setEnabled(false);
  }

  private void doSave() {
    ConfigInfo in = ConfigInfo.create();
    in.setDefaultProject(projectBox.getValue());
    in.setLinkDecoration(linkDecorationBox.getValue(linkDecorationBox.getSelectedIndex()));
    new RestApi("config").id("server").view(Plugin.get().getPluginName(), "config")
        .put(in, new AsyncCallback<JavaScriptObject>() {

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
