/**
 * @license
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import './gr-imagare-inline.js';
import './gr-imagare-preferences.js';
import './gr-imagare-pref-menu-item.js';
import './gr-imagare-upload.js';

Gerrit.install(plugin => {
  plugin.restApi('/config/server/').get('imagare~config').then(config => {
    if (config && config.enable_image_server) {
      plugin.screen('upload', 'gr-imagare-upload');
    }
  });
  plugin.registerCustomComponent(
      'change-view-integration', 'gr-imagare-inline');
  plugin.registerCustomComponent(
      'settings-screen', 'gr-imagare-preferences');
  plugin.registerCustomComponent(
      'settings-menu-item', 'gr-imagare-pref-menu-item');
});
