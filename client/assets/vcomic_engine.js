/*
 * VComic comic-source JavaScript runtime (init.js)
 *
 * Copyright 2026 VComic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ---------------------------------------------------------------------------
 * CLEAN-ROOM IMPLEMENTATION
 * ---------------------------------------------------------------------------
 * This file is an original VComic implementation of the comic-source host API
 * surface expected by community `.js` sources (globals / method names /
 * sendMessage shapes documented in VComic docs and host bridges).
 *
 * It does NOT contain code copied from Venera or any other third-party
 * comic-reader project. API names are retained solely for source compatibility.
 *
 * Host contract (native): global `sendMessage(object) -> any` is injected by
 * QuickJS/JNI before this script runs. Global `appVersion` may also be set.
 */

/* global sendMessage, appVersion */

// ---------------------------------------------------------------------------
// JSON bridge: ArrayBuffer <-> { __type: 'ArrayBuffer', data: base64 }
// Host encodes binary the same way (JsEngine.encodeBinaryTree).
// ---------------------------------------------------------------------------
(function installBinarySafeSendMessage() {
    var nativeSend = sendMessage;
    var B64 =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    function u8ToBase64(u8) {
        var out = "";
        var i = 0;
        var n = u8.length;
        while (i < n) {
            var a = u8[i++];
            var hasB = i < n;
            var b = hasB ? u8[i++] : 0;
            var hasC = i < n;
            var c = hasC ? u8[i++] : 0;
            var triple = (a << 16) | (b << 8) | c;
            out += B64.charAt((triple >> 18) & 63);
            out += B64.charAt((triple >> 12) & 63);
            out += hasB ? B64.charAt((triple >> 6) & 63) : "=";
            out += hasC ? B64.charAt(triple & 63) : "=";
        }
        return out;
    }

    function base64ToU8(b64) {
        var s = String(b64).replace(/[\s\r\n]/g, "");
        var pad = s.endsWith("==") ? 2 : s.endsWith("=") ? 1 : 0;
        var outLen = Math.floor((s.length * 3) / 4) - pad;
        var bytes = new Uint8Array(outLen);
        var o = 0;
        for (var i = 0; i < s.length; i += 4) {
            var e1 = B64.indexOf(s.charAt(i));
            var e2 = B64.indexOf(s.charAt(i + 1));
            var c3 = s.charAt(i + 2);
            var c4 = s.charAt(i + 3);
            var e3 = c3 === "=" || c3 === "" ? -1 : B64.indexOf(c3);
            var e4 = c4 === "=" || c4 === "" ? -1 : B64.indexOf(c4);
            if (e1 < 0 || e2 < 0) continue;
            bytes[o++] = (e1 << 2) | (e2 >> 4);
            if (e3 >= 0) bytes[o++] = ((e2 & 15) << 4) | (e3 >> 2);
            if (e4 >= 0) bytes[o++] = ((e3 & 3) << 6) | e4;
        }
        return o === bytes.length ? bytes : bytes.subarray(0, o);
    }

    function pack(value, seen) {
        if (value == null) return value;
        if (value instanceof ArrayBuffer) {
            return {
                __type: "ArrayBuffer",
                data: u8ToBase64(new Uint8Array(value)),
            };
        }
        if (typeof ArrayBuffer !== "undefined" && ArrayBuffer.isView && ArrayBuffer.isView(value)) {
            var v = value;
            var sliced = v.buffer.slice(v.byteOffset, v.byteOffset + v.byteLength);
            return {
                __type: "ArrayBuffer",
                data: u8ToBase64(new Uint8Array(sliced)),
            };
        }
        if (typeof value === "object") {
            var set = seen || new Set();
            if (set.has(value)) return null;
            set.add(value);
            if (Array.isArray(value)) {
                return value.map(function (item) {
                    return pack(item, set);
                });
            }
            var obj = {};
            var keys = Object.keys(value);
            for (var i = 0; i < keys.length; i++) {
                obj[keys[i]] = pack(value[keys[i]], set);
            }
            return obj;
        }
        return value;
    }

    function unpack(value, seen) {
        if (value == null) return value;
        if (typeof value !== "object") return value;
        var set = seen || new Set();
        if (set.has(value)) return null;
        set.add(value);
        if (Array.isArray(value)) {
            return value.map(function (item) {
                return unpack(item, set);
            });
        }
        if (value.__type === "ArrayBuffer" && typeof value.data === "string") {
            var view = base64ToU8(value.data);
            var ab = new ArrayBuffer(view.byteLength);
            new Uint8Array(ab).set(view);
            return ab;
        }
        var obj = {};
        var keys = Object.keys(value);
        for (var i = 0; i < keys.length; i++) {
            obj[keys[i]] = unpack(value[keys[i]], set);
        }
        return obj;
    }

    sendMessage = function (message) {
        var raw = nativeSend(pack(message));
        if (raw && typeof raw.then === "function") {
            return raw.then(unpack);
        }
        return unpack(raw);
    };
})();

// ---------------------------------------------------------------------------
// Timers (host delay is synchronous; callback runs after delay returns)
// ---------------------------------------------------------------------------

function setTimeout(callback, delayMs) {
    var result = sendMessage({ method: "delay", time: delayMs });
    if (result && typeof result.then === "function") {
        result.then(callback);
    } else {
        Promise.resolve().then(callback);
    }
}

function IntervalHandle(periodMs, fn) {
    this._period = periodMs;
    this._fn = fn;
    this._alive = false;
}

IntervalHandle.prototype._tick = function () {
    if (!this._alive) return;
    this._fn();
    setTimeout(this._tick.bind(this), this._period);
};

IntervalHandle.prototype.run = function () {
    this._alive = true;
    this._tick();
};

IntervalHandle.prototype.cancel = function () {
    this._alive = false;
};

function setInterval(callback, delayMs) {
    var handle = new IntervalHandle(delayMs, callback);
    handle.run();
    return handle;
}

// ---------------------------------------------------------------------------
// Convert — encoding / hashing / AES (host: ConvertBridge)
// ---------------------------------------------------------------------------

var Convert = {
    encodeUtf8: function (str) {
        return sendMessage({
            method: "convert",
            type: "utf8",
            value: str,
            isEncode: true,
        });
    },
    decodeUtf8: function (buf) {
        return sendMessage({
            method: "convert",
            type: "utf8",
            value: buf,
            isEncode: false,
        });
    },
    encodeGbk: function (str) {
        return sendMessage({
            method: "convert",
            type: "gbk",
            value: str,
            isEncode: true,
        });
    },
    decodeGbk: function (buf) {
        return sendMessage({
            method: "convert",
            type: "gbk",
            value: buf,
            isEncode: false,
        });
    },
    encodeBase64: function (buf) {
        return sendMessage({
            method: "convert",
            type: "base64",
            value: buf,
            isEncode: true,
        });
    },
    decodeBase64: function (str) {
        return sendMessage({
            method: "convert",
            type: "base64",
            value: str,
            isEncode: false,
        });
    },
    md5: function (buf) {
        return sendMessage({
            method: "convert",
            type: "md5",
            value: buf,
            isEncode: true,
        });
    },
    sha1: function (buf) {
        return sendMessage({
            method: "convert",
            type: "sha1",
            value: buf,
            isEncode: true,
        });
    },
    sha256: function (buf) {
        return sendMessage({
            method: "convert",
            type: "sha256",
            value: buf,
            isEncode: true,
        });
    },
    sha512: function (buf) {
        return sendMessage({
            method: "convert",
            type: "sha512",
            value: buf,
            isEncode: true,
        });
    },
    hmac: function (key, buf, hashName) {
        return sendMessage({
            method: "convert",
            type: "hmac",
            value: buf,
            key: key,
            hash: hashName,
            isEncode: true,
        });
    },
    hmacString: function (key, buf, hashName) {
        return sendMessage({
            method: "convert",
            type: "hmac",
            value: buf,
            key: key,
            hash: hashName,
            isEncode: true,
            isString: true,
        });
    },
    encryptAesEcb: function (buf, key) {
        return sendMessage({
            method: "convert",
            type: "aes-ecb",
            value: buf,
            key: key,
            isEncode: true,
        });
    },
    decryptAesEcb: function (buf, key) {
        return sendMessage({
            method: "convert",
            type: "aes-ecb",
            value: buf,
            key: key,
            isEncode: false,
        });
    },
    encryptAesCbc: function (buf, key, iv) {
        return sendMessage({
            method: "convert",
            type: "aes-cbc",
            value: buf,
            key: key,
            iv: iv,
            isEncode: true,
        });
    },
    decryptAesCbc: function (buf, key, iv) {
        return sendMessage({
            method: "convert",
            type: "aes-cbc",
            value: buf,
            key: key,
            iv: iv,
            isEncode: false,
        });
    },
    encryptAesCfb: function (buf, key, iv, blockSize) {
        return sendMessage({
            method: "convert",
            type: "aes-cfb",
            value: buf,
            key: key,
            iv: iv,
            blockSize: blockSize,
            isEncode: true,
        });
    },
    decryptAesCfb: function (buf, key, iv, blockSize) {
        return sendMessage({
            method: "convert",
            type: "aes-cfb",
            value: buf,
            key: key,
            iv: iv,
            blockSize: blockSize,
            isEncode: false,
        });
    },
    encryptAesOfb: function (buf, key, blockSize) {
        return sendMessage({
            method: "convert",
            type: "aes-ofb",
            value: buf,
            key: key,
            blockSize: blockSize,
            isEncode: true,
        });
    },
    decryptAesOfb: function (buf, key, blockSize) {
        return sendMessage({
            method: "convert",
            type: "aes-ofb",
            value: buf,
            key: key,
            blockSize: blockSize,
            isEncode: false,
        });
    },
    decryptRsa: function (buf, key) {
        return sendMessage({
            method: "convert",
            type: "rsa",
            value: buf,
            key: key,
            isEncode: false,
        });
    },
    /** Bytes → lowercase hex string (implemented in JS; host also has type "hex"). */
    hexEncode: function (buf) {
        var digits = "0123456789abcdef";
        var u8 = new Uint8Array(buf);
        var parts = new Array(u8.length * 2);
        for (var i = 0; i < u8.length; i++) {
            var b = u8[i];
            parts[i * 2] = digits.charAt((b >> 4) & 0xf);
            parts[i * 2 + 1] = digits.charAt(b & 0xf);
        }
        return parts.join("");
    },
};

// ---------------------------------------------------------------------------
// Random / UUID
// ---------------------------------------------------------------------------

function createUuid() {
    return sendMessage({ method: "uuid" });
}

function randomInt(min, max) {
    return sendMessage({
        method: "random",
        type: "int",
        min: min,
        max: max,
    });
}

function randomDouble(min, max) {
    return sendMessage({
        method: "random",
        type: "double",
        min: min,
        max: max,
    });
}

// ---------------------------------------------------------------------------
// Cookie + Network
// ---------------------------------------------------------------------------

function Cookie(fields) {
    this.name = fields.name;
    this.value = fields.value;
    this.domain = fields.domain;
}

var Network = {
    fetchBytes: async function (method, url, headers, data, extra) {
        var result = await sendMessage({
            method: "http",
            http_method: method,
            bytes: true,
            url: url,
            headers: headers,
            data: data,
            extra: extra,
        });
        if (result && result.error) throw result.error;
        return result;
    },

    sendRequest: async function (method, url, headers, data, extra) {
        var result = await sendMessage({
            method: "http",
            http_method: method,
            url: url,
            headers: headers,
            data: data,
            extra: extra,
        });
        if (result && result.error) throw result.error;
        return result;
    },

    // Historical call shape: 4th positional arg is treated as `data` by sendRequest.
    get: async function (url, headers, extra) {
        return this.sendRequest("GET", url, headers, extra);
    },

    post: async function (url, headers, data, extra) {
        return this.sendRequest("POST", url, headers, data, extra);
    },

    put: async function (url, headers, data, extra) {
        return this.sendRequest("PUT", url, headers, data, extra);
    },

    patch: async function (url, headers, data, extra) {
        return this.sendRequest("PATCH", url, headers, data, extra);
    },

    delete: async function (url, headers, extra) {
        return this.sendRequest("DELETE", url, headers, extra);
    },

    setCookies: function (url, cookies) {
        sendMessage({
            method: "cookie",
            function: "set",
            url: url,
            cookies: cookies,
        });
    },

    getCookies: function (url) {
        return sendMessage({
            method: "cookie",
            function: "get",
            url: url,
        });
    },

    deleteCookies: function (url) {
        sendMessage({
            method: "cookie",
            function: "delete",
            url: url,
        });
    },
};

/**
 * Browser-like fetch (status helpers + body readers).
 */
async function fetch(url, options) {
    var method = "GET";
    var headers = {};
    var body = null;
    if (options) {
        if (options.method) method = options.method;
        if (options.headers) headers = options.headers;
        if (options.body !== undefined) body = options.body;
    }
    var res = await Network.fetchBytes(method, url, headers, body);
    return {
        ok: res.status >= 200 && res.status < 300,
        status: res.status,
        statusText: "",
        headers: res.headers,
        arrayBuffer: async function () {
            return res.body;
        },
        text: async function () {
            return Convert.decodeUtf8(res.body);
        },
        json: async function () {
            return JSON.parse(Convert.decodeUtf8(res.body));
        },
    };
}

// ---------------------------------------------------------------------------
// HTML DOM facade (host: HtmlBridge)
// ---------------------------------------------------------------------------

function HtmlDocument(html) {
    this.key = HtmlDocument._nextId++;
    sendMessage({
        method: "html",
        function: "parse",
        key: this.key,
        data: html,
    });
}
HtmlDocument._nextId = 0;

HtmlDocument.prototype.querySelector = function (query) {
    var id = sendMessage({
        method: "html",
        function: "querySelector",
        key: this.key,
        query: query,
    });
    return id == null ? null : new HtmlElement(id, this.key);
};

HtmlDocument.prototype.querySelectorAll = function (query) {
    var ids = sendMessage({
        method: "html",
        function: "querySelectorAll",
        key: this.key,
        query: query,
    });
    if (!ids) return [];
    return ids.map(function (id) {
        return new HtmlElement(id, this.key);
    }.bind(this));
};

HtmlDocument.prototype.getElementById = function (elementId) {
    var id = sendMessage({
        method: "html",
        function: "getElementById",
        key: this.key,
        id: elementId,
    });
    return id == null ? null : new HtmlElement(id, this.key);
};

HtmlDocument.prototype.dispose = function () {
    sendMessage({
        method: "html",
        function: "dispose",
        key: this.key,
    });
};

function HtmlElement(elementKey, documentKey) {
    this.key = elementKey;
    this.doc = documentKey;
}

Object.defineProperty(HtmlElement.prototype, "text", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getText",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlElement.prototype, "attributes", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getAttributes",
            key: this.key,
            doc: this.doc,
        });
    },
});

HtmlElement.prototype.querySelector = function (query) {
    var id = sendMessage({
        method: "html",
        function: "dom_querySelector",
        key: this.key,
        query: query,
        doc: this.doc,
    });
    return id == null ? null : new HtmlElement(id, this.doc);
};

HtmlElement.prototype.querySelectorAll = function (query) {
    var ids = sendMessage({
        method: "html",
        function: "dom_querySelectorAll",
        key: this.key,
        query: query,
        doc: this.doc,
    });
    if (!ids) return [];
    return ids.map(function (id) {
        return new HtmlElement(id, this.doc);
    }.bind(this));
};

Object.defineProperty(HtmlElement.prototype, "children", {
    get: function () {
        var ids = sendMessage({
            method: "html",
            function: "getChildren",
            key: this.key,
            doc: this.doc,
        });
        if (!ids) return [];
        return ids.map(function (id) {
            return new HtmlElement(id, this.doc);
        }.bind(this));
    },
});

Object.defineProperty(HtmlElement.prototype, "nodes", {
    get: function () {
        var ids = sendMessage({
            method: "html",
            function: "getNodes",
            key: this.key,
            doc: this.doc,
        });
        if (!ids) return [];
        return ids.map(function (id) {
            return new HtmlNode(id, this.doc);
        }.bind(this));
    },
});

Object.defineProperty(HtmlElement.prototype, "innerHTML", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getInnerHTML",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlElement.prototype, "parent", {
    get: function () {
        var id = sendMessage({
            method: "html",
            function: "getParent",
            key: this.key,
            doc: this.doc,
        });
        return id == null ? null : new HtmlElement(id, this.doc);
    },
});

Object.defineProperty(HtmlElement.prototype, "classNames", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getClassNames",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlElement.prototype, "id", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getId",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlElement.prototype, "localName", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "getLocalName",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlElement.prototype, "previousElementSibling", {
    get: function () {
        var id = sendMessage({
            method: "html",
            function: "getPreviousSibling",
            key: this.key,
            doc: this.doc,
        });
        return id == null ? null : new HtmlElement(id, this.doc);
    },
});

Object.defineProperty(HtmlElement.prototype, "nextElementSibling", {
    get: function () {
        var id = sendMessage({
            method: "html",
            function: "getNextSibling",
            key: this.key,
            doc: this.doc,
        });
        return id == null ? null : new HtmlElement(id, this.doc);
    },
});

function HtmlNode(nodeKey, documentKey) {
    this.key = nodeKey;
    this.doc = documentKey;
}

Object.defineProperty(HtmlNode.prototype, "text", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "node_text",
            key: this.key,
            doc: this.doc,
        });
    },
});

Object.defineProperty(HtmlNode.prototype, "type", {
    get: function () {
        return sendMessage({
            method: "html",
            function: "node_type",
            key: this.key,
            doc: this.doc,
        });
    },
});

HtmlNode.prototype.toElement = function () {
    // Host HtmlBridge name is snake_case.
    var id = sendMessage({
        method: "html",
        function: "node_to_element",
        key: this.key,
        doc: this.doc,
    });
    return id == null ? null : new HtmlElement(id, this.doc);
};

// ---------------------------------------------------------------------------
// Logging
// ---------------------------------------------------------------------------

function log(level, title, content) {
    sendMessage({
        method: "log",
        level: level,
        title: title,
        content: content,
    });
}

var console = {
    log: function (content) {
        log("info", "JS Console", content);
    },
    warn: function (content) {
        log("warning", "JS Console", content);
    },
    error: function (content) {
        log("error", "JS Console", content);
    },
};

// ---------------------------------------------------------------------------
// Model constructors (plain data objects for host JSON)
// ---------------------------------------------------------------------------

function Comic(fields) {
    this.id = fields.id;
    this.title = fields.title;
    this.subtitle = fields.subtitle;
    this.subTitle = fields.subTitle;
    this.cover = fields.cover;
    this.tags = fields.tags;
    this.description = fields.description;
    this.maxPage = fields.maxPage;
    this.language = fields.language;
    this.favoriteId = fields.favoriteId;
    this.stars = fields.stars;
}

/** Recursively turn Map (and nested Maps) into plain objects for the host bridge. */
function __mapToPlainObject(value) {
    if (value == null) return value;
    if (typeof Map !== "undefined" && value instanceof Map) {
        var plain = {};
        value.forEach(function (v, k) {
            plain[String(k)] = __mapToPlainObject(v);
        });
        return plain;
    }
    if (Array.isArray(value)) {
        return value.map(__mapToPlainObject);
    }
    if (typeof value === "object") {
        if (
            (typeof ArrayBuffer !== "undefined" && value instanceof ArrayBuffer) ||
            (ArrayBuffer.isView && ArrayBuffer.isView(value))
        ) {
            return value;
        }
        var out = {};
        var keys = Object.keys(value);
        for (var i = 0; i < keys.length; i++) {
            out[keys[i]] = __mapToPlainObject(value[keys[i]]);
        }
        return out;
    }
    return value;
}

function ComicDetails(fields) {
    this.title = fields.title;
    var sub = fields.subtitle;
    if (sub === undefined || sub === null) sub = fields.subTitle;
    this.subtitle = sub;
    this.cover = fields.cover;
    this.description = fields.description;
    var mappedTags = __mapToPlainObject(fields.tags);
    this.tags = mappedTags == null ? fields.tags : mappedTags;
    var mappedChapters = __mapToPlainObject(fields.chapters);
    this.chapters = mappedChapters == null ? fields.chapters : mappedChapters;
    this.isFavorite = fields.isFavorite;
    this.subId = fields.subId;
    this.thumbnails = fields.thumbnails;
    this.recommend = fields.recommend;
    this.commentCount = fields.commentCount;
    this.likesCount = fields.likesCount;
    this.isLiked = fields.isLiked;
    this.uploader = fields.uploader;
    this.updateTime = fields.updateTime;
    this.uploadTime = fields.uploadTime;
    this.url = fields.url;
    this.stars = fields.stars;
    this.maxPage = fields.maxPage;
    this.comments = fields.comments;
}

function Comment(fields) {
    this.userName = fields.userName;
    this.avatar = fields.avatar;
    this.content = fields.content;
    this.time = fields.time;
    this.replyCount = fields.replyCount;
    this.id = fields.id;
    this.isLiked = fields.isLiked;
    this.score = fields.score;
    this.voteStatus = fields.voteStatus;
}

function ImageLoadingConfig(fields) {
    this.url = fields.url;
    this.method = fields.method;
    this.data = fields.data;
    this.headers = fields.headers;
    this.onResponse = fields.onResponse;
    this.modifyImage = fields.modifyImage;
    this.onLoadFailed = fields.onLoadFailed;
}

// ---------------------------------------------------------------------------
// ComicSource base class (sources: `class Foo extends ComicSource`)
// ---------------------------------------------------------------------------

class ComicSource {
    constructor() {
        this.name = "";
        this.key = "";
        this.version = "";
        this.minAppVersion = "";
        this.url = "";
        this.translation = {};
    }

    loadData(dataKey) {
        return sendMessage({
            method: "load_data",
            key: this.key,
            data_key: dataKey,
        });
    }

    loadSetting(settingKey) {
        return sendMessage({
            method: "load_setting",
            key: this.key,
            setting_key: settingKey,
        });
    }

    saveData(dataKey, data) {
        return sendMessage({
            method: "save_data",
            key: this.key,
            data_key: dataKey,
            data: data,
        });
    }

    deleteData(dataKey) {
        return sendMessage({
            method: "delete_data",
            key: this.key,
            data_key: dataKey,
        });
    }

    get isLogged() {
        return sendMessage({
            method: "isLogged",
            key: this.key,
        });
    }

    translate(textKey) {
        var locale = APP.locale;
        var table = this.translation && this.translation[locale];
        if (table && table[textKey] != null) return table[textKey];
        return textKey;
    }

    init() {}
}

ComicSource.sources = {};

// ---------------------------------------------------------------------------
// Image mutate API (modifyImage isolate; host: ImageBridge)
// ---------------------------------------------------------------------------

function Image(imageKey) {
    this.key = imageKey;
}

Image.prototype.copyRange = function (x, y, width, height) {
    var id = sendMessage({
        method: "image",
        function: "copyRange",
        key: this.key,
        x: x,
        y: y,
        width: width,
        height: height,
    });
    return id == null ? null : new Image(id);
};

Image.prototype.copyAndRotate90 = function () {
    var id = sendMessage({
        method: "image",
        function: "copyAndRotate90",
        key: this.key,
    });
    return id == null ? null : new Image(id);
};

Image.prototype.fillImageAt = function (x, y, image) {
    sendMessage({
        method: "image",
        function: "fillImageAt",
        key: this.key,
        x: x,
        y: y,
        image: image.key,
    });
};

Image.prototype.fillImageRangeAt = function (
    x,
    y,
    image,
    srcX,
    srcY,
    width,
    height
) {
    sendMessage({
        method: "image",
        function: "fillImageRangeAt",
        key: this.key,
        x: x,
        y: y,
        image: image.key,
        srcX: srcX,
        srcY: srcY,
        width: width,
        height: height,
    });
};

Object.defineProperty(Image.prototype, "width", {
    get: function () {
        return sendMessage({
            method: "image",
            function: "getWidth",
            key: this.key,
        });
    },
});

Object.defineProperty(Image.prototype, "height", {
    get: function () {
        return sendMessage({
            method: "image",
            function: "getHeight",
            key: this.key,
        });
    },
});

Image.empty = function (width, height) {
    var id = sendMessage({
        method: "image",
        function: "emptyImage",
        width: width,
        height: height,
    });
    return new Image(id);
};

// ---------------------------------------------------------------------------
// UI / APP / clipboard / compute
// ---------------------------------------------------------------------------

var UI = {
    showMessage: function (message) {
        sendMessage({
            method: "UI",
            function: "showMessage",
            message: message,
        });
    },
    showDialog: function (title, content, actions) {
        sendMessage({
            method: "UI",
            function: "showDialog",
            title: title,
            content: content,
            actions: actions,
        });
    },
    launchUrl: function (url) {
        sendMessage({
            method: "UI",
            function: "launchUrl",
            url: url,
        });
    },
    showLoading: function (onCancel) {
        return sendMessage({
            method: "UI",
            function: "showLoading",
            onCancel: onCancel,
        });
    },
    cancelLoading: function (id) {
        sendMessage({
            method: "UI",
            function: "cancelLoading",
            id: id,
        });
    },
    showInputDialog: function (title, validator, image) {
        return sendMessage({
            method: "UI",
            function: "showInputDialog",
            title: title,
            image: image,
            validator: validator,
        });
    },
    showSelectDialog: function (title, options, initialIndex) {
        return sendMessage({
            method: "UI",
            function: "showSelectDialog",
            title: title,
            options: options,
            initialIndex: initialIndex,
        });
    },
};

var APP = {
    get version() {
        return typeof appVersion !== "undefined" ? appVersion : "";
    },
    get locale() {
        return sendMessage({ method: "getLocale" });
    },
    get platform() {
        return sendMessage({ method: "getPlatform" });
    },
};

function setClipboard(text) {
    return sendMessage({
        method: "setClipboard",
        text: text,
    });
}

function getClipboard() {
    return sendMessage({ method: "getClipboard" });
}

function compute(funcSource) {
    var rest = [];
    for (var i = 1; i < arguments.length; i++) rest.push(arguments[i]);
    return sendMessage({
        method: "compute",
        function: funcSource,
        args: rest,
    });
}
