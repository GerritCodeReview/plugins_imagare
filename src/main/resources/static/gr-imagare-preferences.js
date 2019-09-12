// Copyright (C) 2019 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the 'License');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an 'AS IS' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

(function () {
  'use strict';

  Polymer({
    is: 'gr-imagare-preferences',

    properties: {
      _defaultImageProject: String,
      _linkDecoration: String,
      _stageImages: Boolean,
      _prefsChanged: {
        type: Boolean,
        value: false
      },
      _query: {
        type: Function,
        value() {
          return this._queryProjects.bind(this);
        },
      },
    },

    attached() {
      this._getUserPreferences();
    },

    _getUserPreferences() {
      this.plugin.restApi('/accounts/self/')
        .get(`imagare~preference`)
        .then(config => {
          if (!config) {
            return;
          }

          this._linkDecoration = config.link_decoration;
          this._defaultImageProject = config.default_project;
          this._stageImages = config.stage;
        }).catch(response => {
          this.fire('show-error', {message: response});
        });
    },

    _handleImagarePrefsSave(){
      this.plugin.restApi('/accounts/self/')
        .put(`imagare~preference`, {
          default_project: this._defaultImageProject,
          link_decoration: this._linkDecoration,
          stage: this._stageImages,
        }).then(() => {
          this._prefsChanged = false;
        }).catch(response => {
          this.fire('show-error', {message: response});
        });
    },

    _handlePrefsChanged() {
      this._prefsChanged = true;
    },

    _handleStageImagesChanged(event){
      this._handlePrefsChanged();
      this._stageImages = event.target.checked;
    },

    _queryProjects(input) {
      let query;
      if (!input || input === this._defaultImageProject) {
        query = '';
      } else {
        query = `?prefix=${input}`;
      }

      return this.plugin.restApi('/a/projects/').get(query)
          .then(response => {
            const projects = [];
            for (const key in response) {
              if (!response.hasOwnProperty(key)) { continue; }
              projects.push({
                name: key,
                value: decodeURIComponent(response[key].id),
              });
            }
            return projects;
          });
    },
  });
})();
