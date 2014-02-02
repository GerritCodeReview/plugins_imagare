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

import com.google.gwt.core.client.JavaScriptObject;

public class ConfigInfo extends JavaScriptObject {
  final native String getDefaultProject() /*-{ return this.default_project }-*/;

  final LinkDecoration getLinkDecoration() {
    if (link_decoration() == null) {
      return LinkDecoration.NONE;
    }
    return LinkDecoration.valueOf(link_decoration());
  }
  private final native String link_decoration() /*-{ return this.link_decoration; }-*/;

  final native boolean stage() /*-{ return this.stage ? true : false; }-*/;

  final native void setDefaultProject(String p) /*-{ this.default_project = p; }-*/;
  final native void setLinkDecoration(String d) /*-{ this.link_decoration = d; }-*/;
  final native void setStage(boolean s) /*-{ this.stage = s; }-*/;

  static ConfigInfo create() {
    ConfigInfo g = (ConfigInfo) createObject();
    return g;
  }

  protected ConfigInfo() {
  }
}
