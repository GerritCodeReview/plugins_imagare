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

import './gr-imagare-list-item.js';
import {htmlTemplate} from './gr-imagare-upload_html.js';

function preventDefaultFn(event) {
  event.preventDefault();
}

class GrImagareUpload extends Polymer.GestureEventListeners(
    Polymer.LegacyElementMixin(
        Polymer.Element)) {
  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  /** @returns {string} name of the component */
  static get is() { return 'gr-imagare-upload'; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
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
        value: () => new Map(),
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
    };
  }

  static get listeners() {
    return {
      clear: '_handleClearImage',
      delete: '_handleDeleteImage',
      editName: '_handleEditImageName',
      upload: '_handleUploadImage',
    };
  }

  attached() {
    super.attached();
    this.fire('title-change', {title: 'Image Upload'});

    window.addEventListener('dragover', preventDefaultFn, false);
    window.addEventListener('drop', preventDefaultFn, false);
    this.$.dragDropArea.addEventListener('paste', preventDefaultFn, false);

    this._getUserPreferences();
  }

  detached() {
    window.removeEventListener('dragover', preventDefaultFn, false);
    window.removeEventListener('drop', preventDefaultFn, false);
    this.$.dragDropArea.removeEventListener('paste', preventDefaultFn, false);
  }

  _computeFilenameWithCorrectType(filedata, filename) {
    const realFiletype = filedata.slice(
        filedata.indexOf('/') + 1,
        filedata.indexOf(';'));

    let givenFiletype;

    if (filename.indexOf('.') !== -1) {
      givenFiletype = filename.split('.').pop();
    }

    if (!givenFiletype || realFiletype !== givenFiletype) {
      filename += `.${realFiletype}`;
    }

    return filename;
  }

  _computeLoadingClass(loading) {
    return loading ? 'loading' : '';
  }

  _computeSettingsUrl() {
    return `${location.origin}/settings#ImagarePreferences`;
  }

  _computeUploadAllDisabled() {
    if (this._images) {
      for (const value of this._images.values()) {
        if (!value.uploaded) {
          this._allUploaded = false;
          return;
        }
      }
    }

    this._allUploaded = true;
  }

  _createImageObject(name, data, url, list_entry, uploaded, ref) {
    return {data, list_entry, name, ref, url, uploaded};
  }

  _createListEntry(name, data, url) {
    const imagePanel = document.createElement('gr-imagare-list-item');
    imagePanel.setAttribute('image-name', name);

    if (data) {
      imagePanel.setAttribute('image-data', data);
    }

    if (url) {
      imagePanel.setAttribute('image-url', url);
      imagePanel.uploaded = true;
    } else {
      imagePanel.uploaded = false;
    }

    this.$.imageList.appendChild(imagePanel);

    return imagePanel;
  }

  _deleteImage(image) {
    this.plugin.restApi('/projects')
        .delete(`/${this._imageProject}/imagare~images/${image.ref}`)
        .then(() => {
          image.list_entry.remove();
          this._images.delete(image.name);
          if (!this.$.imageList.hasChildNodes()) {
            this.$.imageListContainer.hidden = true;
          }
        }).catch(response => {
          this.fire('show-error', {message: response});
        });
  }

  _extractImageRef(url) {
    return url.split('/').slice(-2)[0];
  }

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
  }

  _handleClearAllImages() {
    while (this.$.imageList.firstChild) {
      this.$.imageList.removeChild(this.$.imageList.firstChild);
    }
    this.$.imageListContainer.hidden = true;

    this._images.clear();
  }

  _handleClearImage(event) {
    event.stopPropagation();
    this._images.delete(event.target.imageName);
    event.target.remove();
    if (!this.$.imageList.hasChildNodes()) {
      this.$.imageListContainer.hidden = true;
    }
  }

  _handleDeleteImage(event) {
    event.stopPropagation();
    this._deleteImage(this._images.get(event.target.imageName));
  }

  _handleDrop(event) {
    event.preventDefault();
    event.stopPropagation();

    for (const file of event.dataTransfer.files) {
      if (!file.type.match('image/.*')) {
        this.fire('show-error', {message: `No image file: ${file.name}`});
      }
      const fr = new FileReader();
      fr.file = file;
      fr.onload = fileLoadEvent => this._handleFileLoadEvent(
          fr.file.name, fileLoadEvent);
      fr.readAsDataURL(file);
    }
  }

  _handleEditImageName(event) {
    event.stopPropagation();
    const editedImage = this._images.get(event.detail.oldName);
    if (this._images.has(event.detail.newName)) {
      this.fire(
          'show-error',
          {message: 'An image with the same name was already staged.'});
      editedImage.list_entry.setAttribute('image-name', event.detail.oldName);
    } else {
      editedImage.name = event.detail.newName;
      this._images.set(editedImage.name, editedImage);
      this._images.delete(event.detail.oldName);
    }
  }

  _handleFileLoadEvent(filename, event) {
    const correctedFilename = this._computeFilenameWithCorrectType(
        event.target.result, filename);
    if (this._stageImages) {
      this._stageImage(correctedFilename, event.target.result);
    } else {
      const image = this._createImageObject(
          correctedFilename, event.target.result);
      this._images.set(correctedFilename, image);
      this._uploadImage(image);
    }
  }

  _handleKeyPress(event) {
    const ctrlDown = event.ctrlKey || event.metaKey;
    if (!ctrlDown) {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  _handleImagePathChanged(event) {
    for (const file of event.target.files) {
      const fr = new FileReader();
      fr.file = file;
      fr.onload = fileLoadEvent => this._handleFileLoadEvent(
          fr.file.name, fileLoadEvent);
      fr.readAsDataURL(file);
    }

    event.target.value = '';
  }

  _handlePaste(event) {
    const clipboardData = event.clipboardData
        || event.originalEvent.clipboardData;
    const items = clipboardData.items;
    if (JSON.stringify(items)) {
      let blob;
      for (const item of items) {
        if (item.type.indexOf('image') === 0) {
          blob = item.getAsFile();
        }
      }
      if (blob) {
        const fr = new FileReader();
        fr.onload = fileLoadEvent => {
          const filename = `undefined-${this._undefinedFileCounter}`;
          this._undefinedFileCounter++;
          this._handleFileLoadEvent(filename, fileLoadEvent);
        };
        fr.readAsDataURL(blob);
      } else {
        event.preventDefault();
        this.fire('show-error', {message: `No image file`});
      }
    }
  }

  _handleUploadAllImages() {
    for (const image of this._images.values()) {
      this._uploadImage(image);
    }
  }

  _handleUploadImage(event) {
    event.stopPropagation();
    const image = this._createImageObject(
        event.target.imageName,
        event.target.imageData,
        null,
        event.target);
    this._images.set(image.name, image);
    this._uploadImage(image);
  }

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
  }

  _stageImage(name, data) {
    if (this._images.has(name)) {
      const fileName = name.slice(0, name.lastIndexOf('.'));
      const fileExtension = name.slice(name.lastIndexOf('.'));
      name = `${fileName}-${this._undefinedFileCounter}${fileExtension}`;
      this._undefinedFileCounter++;
    }
    const imagePanel = this._createListEntry(name, data, null);
    this._images.set(
        name, this._createImageObject(name, data, null, imagePanel));
    this.$.imageListContainer.hidden = false;
    this._computeUploadAllDisabled();
  }

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
            image.list_entry = this._createListEntry(
                image.name, image.data, response.url);
          } else {
            image.list_entry.setAttribute('image-url', response.url);
            image.list_entry.uploaded = true;
          }

          this._images.set(
              image.name,
              this._createImageObject(
                  image.name, image.data, response.url, image.list_entry, true,
                  this._extractImageRef(response.url)));

          this.$.imageListContainer.hidden = false;
          this._computeUploadAllDisabled();
        }).catch(response => {
          this.fire('show-error', {message: response});
        });
  }
}

customElements.define(GrImagareUpload.is, GrImagareUpload);
