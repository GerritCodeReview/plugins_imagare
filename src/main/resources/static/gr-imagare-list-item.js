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
    is: 'gr-imagare-list-item',

    properties: {
      imageUrl: {
        type: String,
        reflectToAttribute: true,
      },
      imageName: {
        type: String,
        reflectToAttribute: true,
      },
      imageData: {
        type: String,
        reflectToAttribute: true,
      },
      uploaded: {
        type: Boolean,
        observer: '_uploadedChanged',
        reflectToAttribute: true,
      },
      _originalImageName: String,
      _editing: {
        type: Boolean,
        value: false,
      },
      _imageSrc: String,
    },

    attached() {
      this._originalImageName = this.imageName;
      this._setImage();
    },

    _handleCancelRenameName() {
      this.imageName = this._originalImageName;
      this._editing = false;
    },

    _handleClearImage() {
      this.fire("clear");
    },

    _handleDeleteImage() {
      this.fire("delete");
    },

    _handleEditImage() {
      this._editing = true;
    },

    _handleSaveName() {
      this._editing = false;

      if (this._originalImageName === this.imageName) {
        return;
      }

      let oldFileType = this._originalImageName.split('.').pop();
      let newFileType = this.imageName.split('.').pop();
      if (oldFileType !== newFileType) {
        this.imageName += `.${oldFileType}`;
      }

      this.fire("editName", {oldName: this._originalImageName, newName: this.imageName});
    },

    _handleUploadImage() {
      this.fire("upload");
    },

    _openDeleteDialog() {
      this.$.deleteOverlay.open();
    },

    _setImage() {
      if (this.uploaded) {
        this.getElementsByClassName('thumbnail')[0]
          .setAttribute('src', this.imageUrl);
      } else {
        this.getElementsByClassName('thumbnail')[0]
          .setAttribute('src', this.imageData);
      }
    },

    _uploadedChanged(uploaded) {
      this.getElementsByClassName('uploading')[0].hidden = !uploaded;
      this.getElementsByClassName('staging')[0].hidden = uploaded;
      this._setImage();
    },
  });
})();
