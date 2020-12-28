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

package com.googlesource.gerrit.plugins.imagare;

import com.google.common.base.MoreObjects;
import com.google.gerrit.extensions.annotations.PluginCanonicalWebUrl;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.config.ConfigResource;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;

public class GetConfig implements RestReadView<ConfigResource> {

  private final PluginConfig cfg;
  private final String pluginName;
  private final String canonicalWebUrl;

  @Inject
  public GetConfig(
      PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      @PluginCanonicalWebUrl String canonicalWebUrl) {
    this.cfg = cfgFactory.getFromGerritConfig(pluginName);
    this.pluginName = pluginName;
    this.canonicalWebUrl = canonicalWebUrl;
  }

  @Override
  public Response<ConfigInfo> apply(ConfigResource resource) {
    ConfigInfo info = new ConfigInfo();
    info.defaultProject = MoreObjects.firstNonNull(cfg.getString("defaultProject"), "All-Projects");
    info.linkDecoration = cfg.getEnum("linkDecoration", LinkDecoration.INLINE);
    if (LinkDecoration.NONE.equals(info.linkDecoration)) {
      info.linkDecoration = null;
    }
    info.stage = cfg.getBoolean("stage", false);
    if (!info.stage) {
      info.stage = null;
    }
    boolean enableImageServer = cfg.getBoolean("enableImageServer", true);
    info.enableImageServer = enableImageServer;
    if (!info.enableImageServer) {
      info.enableImageServer = null;
    }

    if (enableImageServer) {
      info.pattern =
          escapeRegexpForJavaScript(canonicalWebUrl)
              + "project/.*/rev/.*/.*\\.(jpg|jpeg|png|gif|bmp|ico|svg|tif|tiff)";
      info.uploadUrl = "#/x/" + pluginName + "/upload";
    } else {
      info.pattern = cfg.getString("pattern");
      info.uploadUrl = cfg.getString("uploadUrl");
    }

    return Response.ok(info);
  }

  /**
   * Escapes a string for being used in a JavaScript regexp. The following characters must be
   * escaped: . * + ? ^ $ { } ( ) | [ ] / \
   *
   * @param s string to be escaped
   * @return the escaped string
   */
  private String escapeRegexpForJavaScript(String s) {
    return s.replaceAll("([.*+?^${}()|\\[\\]\\/\\\\])", "\\\\$1");
  }

  public static class ConfigInfo {
    String defaultProject;
    LinkDecoration linkDecoration;
    Boolean stage;
    Boolean enableImageServer;
    String pattern;
    String uploadUrl;
  }
}
