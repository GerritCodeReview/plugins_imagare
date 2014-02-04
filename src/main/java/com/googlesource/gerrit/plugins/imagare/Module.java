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

import static com.google.gerrit.server.config.ConfigResource.CONFIG_KIND;
import static com.google.gerrit.server.project.ProjectResource.PROJECT_KIND;
import static com.google.gerrit.server.account.AccountResource.ACCOUNT_KIND;
import static com.googlesource.gerrit.plugins.imagare.DeleteOwnImagesCapability.DELETE_OWN_IMAGES;
import static com.googlesource.gerrit.plugins.imagare.ImageResource.IMAGE_KIND;

import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.AbstractModule;

public class Module extends AbstractModule {

  @Override
  protected void configure() {
    DynamicSet.bind(binder(), TopMenu.class).to(ImagareMenu.class);
    bind(com.google.gerrit.extensions.config.CapabilityDefinition.class)
        .annotatedWith(Exports.named(DELETE_OWN_IMAGES))
        .to(DeleteOwnImagesCapability.class);
    install(new RestApiModule() {
      @Override
      protected void configure() {
        DynamicMap.mapOf(binder(), IMAGE_KIND);
        bind(ImagesCollection.class);
        child(PROJECT_KIND, "images").to(ImagesCollection.class);
        delete(IMAGE_KIND).to(DeleteImage.class);
        get(CONFIG_KIND, "config").to(GetConfig.class);
        put(CONFIG_KIND, "config").to(PutConfig.class);
        get(ACCOUNT_KIND, "preference").to(GetPreference.class);
        put(ACCOUNT_KIND, "preference").to(PutPreference.class);
      }
    });
  }
}
