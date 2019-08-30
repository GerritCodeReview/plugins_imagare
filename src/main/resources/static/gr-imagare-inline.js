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
    _legacyUndefinedCheck: true,

    properties: {
      _expandedObserver: MutationObserver,
      _messageAddedObserver: MutationObserver,
      _messages: Object,
      _link_decoration: Number,
      _pattern: String,
      _decorator_fn: Function,
    },

    attached() {
      Promise.resolve(this._getAccountPrefs()).then(() => {
        if (this._link_decoration == LINK_DECORATIONS.NONE) {
          return;
        }

        this._expandedObserver = new MutationObserver(mutations => {
          mutations.forEach(mut => {
            if (!mut.target.classList.contains('expanded')){
              return;
            }
            let links = this._getLinksFromMessage(mut.target);

            if (!links) {
              return;
            }

            for (const link of links) {
              this._decorator_fn(link);
            }
          });
        });

        this._messageAddedObserver = new MutationObserver(mutations => {
          mutations.forEach(mut => {
            this._addObserversToMessages()
          });
        });

        this._messageAddedObserver.observe(
          document.getElementsByTagName('gr-messages-list')[0],
          {
            childList: true,
          });

        this._addObserversToMessages()
      });
    },

    _addObserversToMessages() {
      this._messages = this._getMessages();

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
      this.plugin.restApi('/accounts/self/imagare~preference')
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
              this._decorator_fn = this._insert_image;
              break;
            case LINK_DECORATIONS.TOOLTIP:
              this._decorator_fn = this._add_tooltip;
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

    _insert_image(link) {
      if (!link) {
        return;
      }

      link.innerHTML = `<img src="${link.href}">`;
    },

    _add_tooltip(link) {
      if (!link) {
        return;
      }

      link.innerHTML += `<img src="${link.href}" hidden>`;

      link.onmouseover = (event) => {
        event.target.getElementsByTagName('img')[0].hidden = false;
      }

      link.onmouseout = (event) => {
        event.target.getElementsByTagName('img')[0].hidden = true;
      }
    },
  });
})();
