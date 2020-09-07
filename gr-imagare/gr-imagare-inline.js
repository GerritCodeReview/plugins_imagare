/**
 * @license
 * Copyright (C) 2016 The Android Open Source Project
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

const LINK_DECORATIONS = {
  NONE: 1,
  INLINE: 2,
  TOOLTIP: 3,
};

class GrImagareInline extends Polymer.GestureEventListeners(
    Polymer.LegacyElementMixin(
        Polymer.Element)) {
  /** @returns {?} template for this component */
  static get template() { return Polymer.html``; }

  /** @returns {string} name of the component */
  static get is() { return 'gr-imagare-inline'; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
      _expandedObserver: MutationObserver,
      _messageAddedObserver: MutationObserver,
      _messages: Object,
      _link_decoration: Number,
      _pattern: String,
      _decorator_fn: Function,
    };
  }

  attached() {
    super.attached();
    this._getAccountPrefs().then(() => {
      if (this._link_decoration === LINK_DECORATIONS.NONE) {
        return;
      }

      this._expandedObserver = new MutationObserver(mutations => {
        mutations.forEach(mut => {
          if (!mut.target.classList.contains('expanded')) {
            return;
          }
          const links = this._getLinksFromMessage(mut.target);

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
          mut.addedNodes.forEach(node => {
            if (node.tagName === 'GR-MESSAGE') {
              this._addExpandedObservers(node);
            }
          });
        });
      });

      this._messageAddedObserver.observe(
          util.querySelector(document.body, 'gr-messages-list'),
          {
            childList: true,
          });

      this._addObserversToMessages();
    });
  }

  detached() {
    super.detached();
    this._expandedObserver.disconnect();
    this._messageAddedObserver.disconnect();
  }

  _addObserversToMessages() {
    this._messages = this._getMessages();

    if (!this._messages) {
      return;
    }

    for (const message of this._messages) {
      this._addExpandedObservers(message);
    }
  }

  _addExpandedObservers(message) {
    this._expandedObserver.observe(message, {
      attributes: true,
      attributeOldValue: true,
      attributFilter: ['class'],
    });
  }

  _getAccountPrefs() {
    return this.plugin.restApi('/accounts/self/imagare~preference')
        .get('')
        .then(prefs => {
          if (!prefs || !prefs.link_decoration) {
            this._link_decoration = LINK_DECORATIONS.NONE;
            this._pattern = '.*';
          } else {
            this._link_decoration = LINK_DECORATIONS[
                prefs.link_decoration.toUpperCase()
            ];
            this._pattern = prefs.pattern || '.*';
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
  }

  _getMessages() {
    const messageList = util.querySelector(document.body, 'gr-messages-list');
    if (messageList) {
      return util.querySelectorAll(messageList, 'gr-message');
    }
  }

  _getLinksFromMessage(message) {
    let links = [];
    const linkedTexts = util.querySelectorAll(message, 'gr-linked-text');
    for (const e of linkedTexts) {
      const aTags = util.querySelectorAll(e, 'a');
      if (aTags && aTags.length > 0) {
        for (const a of aTags) {
          if (util.querySelectorAll(a, 'img').length > 0) {
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
  }

  _createImage(url) {
    const img = document.createElement('img');
    img.setAttribute('src', url);
    img.setAttribute('style', 'max-width: 100%; height: auto;');

    return img;
  }

  _insertImage(link) {
    if (!link) {
      return;
    }

    link.replaceWith(this._createImage(link.href));
  }

  _addTooltip(link) {
    if (!link) {
      return;
    }

    link.onmouseover = event => {
      const img = this._createImage(link.href);
      img.onmouseout = event => {
        event.target.replaceWith(link);
      };

      event.target.replaceWith(img);
    };
  }
}

customElements.define(GrImagareInline.is, GrImagareInline);
