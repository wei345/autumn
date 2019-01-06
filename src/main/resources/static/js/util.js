"use strict";
function util() {
    var au = {}; // Autumn Util

    au.emptyFn = function () {
    };

    au.isEmpty = function (str) {
        return str == null || str.length === 0;
    };

    au.each = function (obj, fn) {
        for (const k in obj) {
            if (obj.hasOwnProperty(k)) {
                fn(k, obj[k]);
            }
        }
    };

    au.escapeHtml = function (unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    };

    function escapeRegExp(str) {
        return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, '\\$1');
    }

    // 可用于 Array.sort
    au.compareStringIgnoreCase = function (s1, s2) {
        var n1 = s1.length;
        var n2 = s2.length;
        var min = Math.min(n1, n2);
        for (var i = 0; i < min; i++) {
            var c1 = s1.charAt(i);
            var c2 = s2.charAt(i);
            if (c1 !== c2) {
                c1 = c1.toUpperCase();
                c2 = c2.toUpperCase();
                if (c1 !== c2) {
                    c1 = c1.toLowerCase();
                    c2 = c2.toLowerCase();
                    if (c1 !== c2) {
                        // No overflow because of numeric promotion
                        return c1 - c2;
                    }
                }
            }
        }
        return n1 - n2;
    };

    au.stringEqualsIgnoreCase = function (str1, str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        if (str1.length !== str2.length) {
            return false;
        }
        return au.compareStringIgnoreCase(str1, str2) === 0;
    };

    au.el = function (selectors, context) {
        return (context || document).querySelector(selectors);
    };

    au.els = function (selectors, context) {
        // elements 不是正常的数组，如果在遍历过程中对 element 进行修改导致不符合之前的查询条件，
        // 该 element 会自动从 elements 中移除，导致遍历漏掉之后的元素。
        // 将 elements 转为正常的数组可避免这种情况
        return Array.prototype.map.call((context || document).querySelectorAll(selectors), el => el);
    };

    au.replaceClass = function (dom, cls, replacement) {
        // Safari 不支持 classList.replace
        dom.className = dom.className.replace(new RegExp('\\b' + escapeRegExp(cls) + '\\b', 'g'), replacement);
    };

    au.ajax = function (method, url, success, error) {
        var r = new XMLHttpRequest();
        r.open(method, url, true);
        r.onreadystatechange = function () {
            if (r.readyState !== 4) return;
            if (r.status === 200) {
                if (typeof success === 'function') success(r.responseText);
            } else {
                if (typeof error === 'function') error(r.responseText);
            }
        };
        r.send("");
    };

    au.parseQueryString = function (queryString) {
        var params = {};
        var parts = queryString.substr(1).split('&');
        for (var i = 0; i < parts.length; i++) {
            var keyValuePair = parts[i].split('=');
            var key = decodeURIComponent(keyValuePair[0]);
            var value = keyValuePair[1] ?
                decodeURIComponent(keyValuePair[1].replace(/\+/g, ' ')) :
                keyValuePair[1];

            switch (typeof(params[key])) {
                case 'undefined':
                    params[key] = value;
                    break; //first
                case 'array':
                    params[key].push(value);
                    break; //third or more
                default:
                    params[key] = [params[key], value]; // second
            }
        }
        return params;
    };

    au.getCookie = function (name) {
        var v = document.cookie.match('(^|;) ?' + name + '=([^;]*)(;|$)');
        return v ? v[2] : null;
    };

    au.setCookie = function (name, value, seconds) {
        var d = new Date();
        d.setTime(d.getTime() + 1000 * seconds);
        document.cookie = name + '=' + value + ';path=' + ctx + '/;expires=' + d.toGMTString();
    };

    au.deleteCookie = function (name) {
        au.setCookie(name, '', -1);
    };

    /**
     * @returns {string} 首页 /，否则 /xxx
     */
    au.pathname = function () {
        var pathname = location.pathname;
        if (!pathname) {
            pathname = /^(\w+?):\/+?[^\/]+(\/[^?]*)/.exec(location.href)[2];
        }
        if (!pathname) {
            return '/';
        }
        return decodeURIComponent(pathname);
    };

    // -- scroll --
    au.getViewportHeight = function () {
        return window.innerHeight || document.documentElement.clientHeight;
    };

    au.getViewportWidth = function () {
        return window.innerWidth || document.documentElement.clientWidth;
    };

    au.isElementInViewport = function (el) {
        var rect = el.getBoundingClientRect();
        return (
            rect.top >= 0 &&
            rect.left >= 0 &&
            rect.bottom <= au.getViewportHeight() &&
            rect.right <= au.getViewportWidth()
        );
    };

    function getScrollParent(node) {
        if (node === document.body) { // body 没有滚动条
            return document.scrollingElement;
        }

        let overflowY = window.getComputedStyle(node).overflowY;
        let isScrollable = overflowY !== 'visible' && overflowY !== 'hidden';

        if (isScrollable && node.scrollHeight > node.clientHeight) {
            return node;
        } else {
            return getScrollParent(node.parentNode);
        }
    }

    function scrollTo(el, expectRectTop) {
        // 使用 scroll，不使用 selected.scrollIntoView({block: 'center'}) ，
        // 因为如果 selected 在页面底部，scrollIntoView 会"过量滚动"，导致 body 也向上滚动一段距离，
        // 另外，Safari 不支持 scrollIntoView 选项。
        var rect = el.getBoundingClientRect(); // 相对于 Viewport 左上角的坐标
        if (Math.abs(rect.top - expectRectTop) < 16) {
            return;
        }
        var scrollEl = getScrollParent(el);
        var maxScrollTop = scrollEl.scrollHeight - scrollEl.clientHeight;
        if (maxScrollTop === 0) { // 手机 chrome 可能为 0
            maxScrollTop = scrollEl.scrollHeight - au.getViewportHeight();
        }
        var expectScrollTop = scrollEl.scrollTop + (rect.top - expectRectTop);
        var scrollTop = Math.max(0, Math.min(maxScrollTop, expectScrollTop));
        scrollEl.scroll(0, scrollTop);
    }

    au.scrollToTop = function (el) {
        scrollTo(el, 0);
    };

    au.scrollToCenter = function (el) {
        var expectRectTop = (au.getViewportHeight() / 2) - (el.clientHeight / 2);
        scrollTo(el, expectRectTop);
    };

    function getSelectionText() {
        var text = '';
        if (window.getSelection) {
            text = window.getSelection().toString();
        } else if (document.selection && document.selection.type !== 'Control') {
            text = document.selection.createRange().text;
        }
        return text;
    }

    return au;
}