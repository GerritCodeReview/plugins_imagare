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
    is: 'gr-imagare-upload',
    _legacyUndefinedCheck: true,

    properties: {
      _loading: {
        type: Boolean,
        value: true,
      },
      _allUploaded: {
        type: Boolean,
        value: false,
      },
      _imageProject: String,
      _defaultImageProject: String,
      _images: {
        type: Map,
        value: new Map(),
      },
      _stageImages: {
        type: Boolean,
        value: true,
      },
      _undefinedFileCounter: {
        type: Number,
        value: 0,
      },
      _query: {
        type: Function,
        value() {
          return this._queryProjects.bind(this);
        },
      },
    },

    listeners: {
      upload: '_handleUploadImage',
      clear: '_handleClearImage',
    },

    attached() {
      this.fire('title-change', { title: 'Image Upload' });

      window.addEventListener('dragover', e => {
        e = e || event;
        e.preventDefault();
      }, false);

      window.addEventListener('drop', e => {
        e = e || event;
        e.preventDefault();
      }, false);

      document.getElementById('dragDropArea').addEventListener('paste', e => {
        e = e || event;
        e.preventDefault();
      }, false);

      this._getUserPreferences();
    },

    _computeUploadAllDisabled() {
      if (this._images) {
        for (let value of this._images.values()) {
          if (!value.uploaded) {
            this._allUploaded = false;
            return;
          }
        }
      }

      this._allUploaded = true;
    },

    _computeLoadingClass(loading) {
      return loading ? 'loading' : '';
    },

    _computeSettingsUrl() {
      return `${location.origin}/settings#ImagarePreferences`;
    },

    _createImageObject(name, data, url, list_entry, uploaded) {
      return {
        data: data,
        name: name,
        url: url,
        list_entry: list_entry,
        uploaded: uploaded,
      }
    },

    _createListEntry(name, data, url) {
      let imagePanel = document.createElement('gr-imagare-list-item');
      imagePanel.setAttribute("image-name", name);

      if(data) {
        imagePanel.setAttribute("image-data", data);
      }

      if(url) {
        imagePanel.setAttribute("image-url", url);
        imagePanel.uploaded = true;
      } else {
        imagePanel.uploaded = false;
      }

      document.getElementById('imageList').appendChild(imagePanel);

      return imagePanel;
    },

    _getUserPreferences() {
      this.plugin.restApi('/accounts/self/')
        .get(`imagare~preference`)
        .then(config => {
          if (!config) {
            return;
          }

          this._defaultImageProject = config.default_project;
          this._imageProject = config.default_project;
          this._stageImages = config.stage;
          this._loading = false;
        });
    },

    _handleClearAllImages() {
      let container = document.getElementById('imageList');
      while (container.firstChild) {
        container.removeChild(container.firstChild);
      }
      document.getElementById('imageListContainer').hidden = true;

      this._images.clear()
    },

    _handleClearImage(event) {
      this._images.delete(event.target.imageName);
      event.target.remove();
      if (!document.getElementById('imageList').hasChildNodes()) {
        document.getElementById('imageListContainer').hidden = true;
      }
    },

    _handleDrop(event) {
      event.preventDefault();
      event.stopPropagation();

      for (let file of event.dataTransfer.files) {
        if (!file.type.match('image/.*')) {
          this.fire('show-error', { message: `No image file: ${file.name}` });
        }
        let fr = new FileReader();
        fr.file = file;
        fr.onload = fileLoadEvent => {
          if (this._stageImages) {
            this._stageImage(fr.file.name, fileLoadEvent.target.result);
          } else {
            let image = this._createImageObject(fr.file.name, fileLoadEvent.target.result);
            this._images.set(fr.file.name, image);
            this._uploadImage(image);
          }
        }
        fr.readAsDataURL(file);
      }
    },

    _handleKeyPress(event) {
      event = event || window.event;
      let ctrlDown = event.ctrlKey || event.metaKey;
      if (!ctrlDown) {
        event.preventDefault();
      }
    },

    _handleImagePathChanged(event) {
      for (let file of event.target.files) {
        let fr = new FileReader();
        fr.file = file;
        fr.onload = fileLoadEvent => {
          if (this._stageImages) {
            this._stageImage(fr.file.name, fileLoadEvent.target.result);
          } else {
            let image = this._createImageObject(fr.file.name, fileLoadEvent.target.result);
            this._images.set(fr.file.name, image);
            this._uploadImage(image);
          }
        };
        fr.readAsDataURL(file);
      }

      event.target.value = '';
    },

    _handlePaste(event) {
      let clipboardData = event.clipboardData || event.originalEvent.clipboardData;
      let items = clipboardData.items;
      if (JSON.stringify(items)) {
        let blob;
        for (let item of items) {
          if (item.type.indexOf("image") === 0) {
            blob = item.getAsFile();
          }
        }
        if (blob) {
          let fr = new FileReader();
          fr.onload = fileLoadEvent => {
            let filetype = fileLoadEvent.target.result.slice(
              fileLoadEvent.target.result.indexOf('/') + 1,
              fileLoadEvent.target.result.indexOf(';'));
            let filename = `undefined-${this._undefinedFileCounter}.${filetype}`;
            this._undefinedFileCounter++;
            if (this._stageImages) {
              this._stageImage(filename, fileLoadEvent.target.result);
            } else {
              let image = this._createImageObject(filename, fileLoadEvent.target.result);
              this._images.set(filename, image);
              this._uploadImage(image);
            }
          };
          fr.readAsDataURL(blob);
        } else {
          event.preventDefault();
          this.fire('show-error', { message: `No image file` });
        }
      }
    },

    _handleUploadAllImages() {
      for (let image of this._images.values()) {
        this._uploadImage(image);
      }
    },

    _handleUploadImage(event) {
      let image = this._createImageObject(
        event.target.imageName,
        event.target.imageData,
        null,
        event.target);
      this._images.set(image.name, image);
      this._uploadImage(image);
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

    _stageImage(name, data) {
      if (this._images.has(name)) {
        let fileName = name.slice(0, name.lastIndexOf('.'));
        let fileExtension = name.slice(name.lastIndexOf('.'));
        name = `${fileName}-${this._undefinedFileCounter}${fileExtension}`;
        this._undefinedFileCounter++;
      }
      let imagePanel = this._createListEntry(name, data, null);
      this._images.set(name, this._createImageObject(name, data, null, imagePanel));
      document.getElementById('imageListContainer').hidden = false;
      this._computeUploadAllDisabled();
    },

    _uploadImage(image) {
      if (image && image.uploaded) {
        return;
      }

      this.plugin.restApi('/projects')
        .post(`/${this._imageProject}/imagare~images`, {
          image_data: image.data,
          file_name: image.name,
        })
        .then(response => {
          if (!image.list_entry) {
            image.list_entry = this._createListEntry(image.name, image.data, response.url);
          } else {
            image.list_entry.setAttribute("image-url", response.url);
            image.list_entry.uploaded = true;
          }

          this._images.set(
            image.name,
            this._createImageObject(
              image.name, image.data, response.url, image.list_entry, true));

          document.getElementById('imageListContainer').hidden = false;
          this._computeUploadAllDisabled();
        }).catch(response => {
          this.fire('show-error', {message: response});
        });
    }
  });
})();
