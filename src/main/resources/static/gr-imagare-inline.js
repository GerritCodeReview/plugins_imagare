// Copyright (C) 2019 The Android Open Source Project
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

(function () {
  'use strict';

  const LINK_DECORATIONS = {
    NONE: 1,
    INLINE: 2,
    TOOLTIP: 3,
  };

  Polymer({
    is: 'gr-imagare-inline',

    properties: {
      _expandedObserver: MutationObserver,
      _messageAddedObserver: MutationObserver,
      _messages: Object,
      _link_decoration: Number,
      _pattern: String,
      _decorator_fn: Function,
    },

    attached() {
      this._getAccountPrefs().then(() => {
        if (this._link_decoration === LINK_DECORATIONS.NONE) {
          return;
        }

        this._messages = this._getMessages();

        for (const message of this._messages) {
          if (message.classList.contains('expanded')) {
            this._renderLinks(message);
          }
        }

        this._expandedObserver = new MutationObserver(mutations => {
          mutations.forEach(mut => {
            if (!mut.target.classList.contains('expanded')){
              return;
            }
            this._renderLinks(mut.target);
          });
        });

        this._messageAddedObserver = new MutationObserver(mutations => {
          mutations.forEach(mut => {
            mut.addedNodes.forEach(node => {
              if (node.tagName === "GR-MESSAGE") {
                this._addExpandedObservers(node);
              }
            });
          });
        });

        this._messageAddedObserver.observe(
          document.getElementsByTagName('gr-messages-list')[0],
          {
            childList: true,
          });

        this._addObserversToMessages();
      });
    },

    detached() {
      this._expandedObserver.disconnect();
      this._messageAddedObserver.disconnect();
    },

    _addObserversToMessages() {
      if (!this._messages) {
        return;
      }

      for (const message of this._messages) {
        this._addExpandedObservers(message);
      }
    },

    _addExpandedObservers(message) {
      this._expandedObserver.observe(message, {
        attributes: true,
        attributeOldValue: true,
        attributFilter: ['class'],
      });
    },

    _getAccountPrefs() {
      return this.plugin.restApi('/accounts/self/imagare~preference')
        .get('')
        .then(prefs => {
          if (!prefs || !prefs.link_decoration) {
            this._link_decoration = LINK_DECORATIONS.NONE;
            this._pattern = '.*';
          } else {
            this._link_decoration = LINK_DECORATIONS[prefs.link_decoration.toUpperCase()];
            this._pattern = prefs.pattern || '.*';
          }

          switch (this._link_decoration) {
            case LINK_DECORATIONS.INLINE:
              this._decorator_fn = this._insertImage.bind(this);
              break;
            case LINK_DECORATIONS.TOOLTIP:
              this._decorator_fn = this._addTooltip.bind(this);
              break;
            case LINK_DECORATIONS.NONE:
            default:
              this._decorator_fn = () => {};
          }
        });
    },

    _getMessages() {
      let messageList = document.getElementsByTagName('gr-messages-list')[0];
      if (messageList) {
        return messageList.getElementsByTagName('gr-message');
      }
    },

    _getLinksFromMessage(message) {
      let links = [];
      let linkedTexts = message.getElementsByTagName('gr-linked-text');
      for (const e of linkedTexts) {
        let aTags = e.getElementsByTagName('a');
        if (aTags && aTags.length > 0){
          for (const a of aTags){
            if (a.getElementsByTagName('img').length > 0) {
              continue;
            }
            if (!a.href.match(this._pattern)) {
              continue;
            }

            links = links.concat(a);
          }
        }
      }
      return links.length > 0 ? links : null;
    },

    _renderLinks(message) {
      let links = this._getLinksFromMessage(message);

      if (!links) {
        return;
      }

      for (const link of links) {
        this._decorator_fn(link);
      }
    },

    _createImage(url) {
      let img = document.createElement('img');
      img.setAttribute("src", url);
      img.setAttribute("style", "max-width: 100%; height: auto;");

      return img;
    },

    _insertImage(link) {
      if (!link) {
        return;
      }

      link.replaceWith(this._createImage(link.href));
    },

    _addTooltip(link) {
      if (!link) {
        return;
      }

      link.onmouseover = (event) => {
        let img = this._createImage(link.href);
        img.onmouseout = (event) => {
          event.target.replaceWith(link);
        }

        event.target.replaceWith(img);
      }
    },
  });
})();
