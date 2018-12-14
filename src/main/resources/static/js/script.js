"use strict";
var autumn = {
    ctx: '',
    treeVersion: ''
};
var lsRecentVisitKey = 'autumn.recently_visit';
var recentlyVisitMaxCount = 100;
var recentlyVisitPages;
var logoutCookieName = 'logout';
var isMobi = /Mobi/.test(navigator.userAgent);
var alwaysUnfoldRoot = false;
var multipleSelectionEnabled = false;
window.addEventListener('load', function () {
    bindSidebarToggle();
    bindTocToggle();
    bindShortcut();
    buildTree(setupQuickSearch);
    anchorLink();
    checkLogout();
    updateVisitList();
    bindToggle('sitemap');
    detectClient();
});

/**
 * @returns {string} 如果是首页返回 /，否则返回 /xxx
 */
autumn.pathname = function () {
    var pathname = location.pathname;
    if (!pathname) {
        pathname = /^(\w+?):\/+?[^\/]+(\/[^?]*)/.exec(location.href)[2];
    }
    if (!pathname) {
        return '/';
    }
    return decodeURIComponent(pathname);
};

autumn.ajax = function (method, url, success, error) {
    var r = new XMLHttpRequest();
    r.open(method, url, true);
    r.onreadystatechange = function () {
        if (r.readyState !== 4) return;
        if (r.status === 200) {
            if (typeof(success === 'function')) success(r.responseText);
        } else {
            if (typeof(error === 'function')) error(r.responseText);
        }
    };
    r.send("");
};

// Safari 不支持 classList.replace
autumn.replaceClass = function (dom, cls, replacement) {
    dom.className = dom.className.replace(new RegExp('\\b' + escapeRegExp(cls) + '\\b', 'g'), replacement);
};

autumn.getCookie = function (name) {
    var v = document.cookie.match('(^|;) ?' + name + '=([^;]*)(;|$)');
    return v ? v[2] : null;
};

autumn.setCookie = function (name, value, seconds) {
    var d = new Date();
    d.setTime(d.getTime() + 1000 * seconds);
    document.cookie = name + "=" + value + ";path=" + autumn.ctx + "/;expires=" + d.toGMTString();
};

autumn.deleteCookie = function deleteCookie(name) {
    autumn.setCookie(name, '', -1);
};

function escapeRegExp(str) {
    return str.replace(/([.*+?^=!:${}()|\[\]\/\\])/g, "\\$1");
}

function detectClient() {
    document.body.classList.add('js');
    if (isMobi) {
        document.body.classList.add('mobi');
    }
}

function bindSidebarToggle() {
    var toggle = document.getElementsByClassName('sidebar_toggle')[0];
    var target = document.getElementsByClassName('sidebar')[0];
    if (!toggle || !target) {
        return;
    }
    var lsKey;
    switch (autumn.pathname()) {
        case autumn.ctx + '/':
            lsKey = 'autumn.home.sidebar.display';
            break;
        case autumn.ctx + '/search':
            lsKey = 'autumn.search.sidebar.display';
            break;
        default:
            lsKey = 'autumn.sidebar.display';
            break;
    }
    var main = document.getElementsByClassName('main')[0];
    // 默认不显示 Sidebar，除了 '/' 和 '/search'
    if (lsKey !== 'autumn.sidebar.display') {
        main.classList.toggle('show_sidebar', localStorage.getItem(lsKey) !== '0');
    }
    toggle.addEventListener('click', function () {
        var val = main.classList.toggle('show_sidebar') ? '1' : '0';
        localStorage.setItem(lsKey, val);
    });
}

function bindTocToggle() {
    // CSS 已设置 TOC body 默认隐藏
    var toc = document.getElementsByClassName('toc')[0];
    if (!toc) {
        return;
    }
    var toggle = toc.getElementsByTagName('h3')[0];
    var body = toggle.nextElementSibling;
    if (!toggle || !body || body.getElementsByTagName('li').length < 3) {
        if (toggle) {
            toggle.style.display = 'none';
        }
        return;
    }
    toggle.classList.add('no_selection', 'action_toggle');
    var lsKey = 'autumn.toc.display';
    body.classList.toggle('show', localStorage.getItem(lsKey) !== '0');
    toggle.addEventListener('click', function () {
        localStorage.setItem(lsKey, body.classList.toggle('show') ? '1' : '0');
    });
}

function bindShortcut() {
    var searchInput = document.getElementsByClassName('header__row_1__search_input')[0];
    if (!searchInput) {
        return; // 可能在登录页
    }
    document.addEventListener('keydown', function (event) {
        if (event.target === document.body) {
            switch (event.key) {
                case '/':
                    searchInput.focus();
                    searchInput.select();
                    event.preventDefault();
                    break;
                case 'g':
                    scroll(0, 0);
                    break;
                case 'G':
                    scroll(0, document.scrollingElement.scrollHeight);
                    break;
                case 'u':
                    document.scrollingElement.scrollTop -= window.innerHeight / 2;
                    break;
                case 'd':
                    document.scrollingElement.scrollTop += window.innerHeight / 2;
                    break;
            }
        }
    });
}

function buildTree(then) {
    var treeBox = document.getElementsByClassName('tree_box')[0];
    var foldedString = '+';
    var unfoldedString = '−';

    if (!treeBox) {
        return; // 可能在登录页
    }
    autumn.ajax('GET', autumn.ctx + '/tree.json?' + autumn.treeVersion, function (text) {
        var root = JSON.parse(text);
        unfoldCurrentPath(root);
        treeBox.innerHTML = buildTreeHtml([root]);
        bindNodeToggle(treeBox);
        if (typeof(then) === 'function') {
            then(root);
        }
    });

    function unfoldCurrentPath(root) {
        var path = autumn.pathname().substr(autumn.ctx.length);
        var current;
        if (path === '/') {
            current = root;
            root.current = true;
        } else {
            var dirs = [root];
            while (dirs.length > 0) {
                var dir = dirs.pop();
                for (var i = 0; i < dir.children.length; i++) {
                    var node = dir.children[i];
                    node.parent = dir;
                    if (node.path === path) {
                        node.current = true;
                        current = node;
                        break;
                    }
                    if (node.children) {
                        dirs.push(node);
                    }
                }
            }
        }

        if (current) {
            var parent = current;
            while (parent = parent.parent) {
                parent.unfolded = true;
            }
        }

        if (alwaysUnfoldRoot) {
            root.unfolded = true;
        }
    }

    function buildTreeHtml(children) {
        if (!children || children.length === 0) {
            return '';
        }
        var html = '<ul>';
        for (var i = 0; i < children.length; i++) {
            var node = children[i];
            // begin node
            html += '<li class="tree_node';
            if (node.children) {
                html += ' tree_node_dir';
                html += (node.unfolded ? ' tree_node_unfolded' : ' tree_node_folded');
            } else {
                html += ' tree_node_leaf';
            }
            html += '">';

            // begin header
            html += '<div class="tree_node_header' + (node.current ? ' tree_node_header_selected' : '') + '">';

            // icon
            html += '<span class="tree_node_header_icon no_selection">';
            if (node.children) {
                //html += (node.unfolded ? unfoldedString : foldedString);
            } else {
                // html += '•';
            }
            html += '</span>';

            // title
            html += (node.children ? '<span class="tree_node_header_name action_toggle no_selection">' : ('<a href="' + (autumn.ctx + node.path) + '">'));
            html += node.name;
            html += (node.children ? '</span>' : '</a>');

            // end header
            html += '</div>';

            html += buildTreeHtml(node.children);

            // end node
            html += '</li>';
        }
        html += '</ul>';
        return html;
    }

    function bindNodeToggle(tree) {
        var els = tree.getElementsByClassName('tree_node_dir');
        for (var i = 0; i < els.length; i++) {
            var t = els[i].getElementsByClassName('tree_node_header')[0];
            t.addEventListener('click', toggle);
        }
    }

    function toggle(event) {
        var node = event.currentTarget.parentNode; // tree_node
        var icon = node.getElementsByClassName('tree_node_header_icon')[0];
        if (node.classList.contains('tree_node_folded')) {
            // icon.innerHTML = unfoldedString;
            autumn.replaceClass(node, 'tree_node_folded', 'tree_node_unfolded');
        } else {
            // icon.innerHTML = foldedString;
            autumn.replaceClass(node, 'tree_node_unfolded', 'tree_node_folded');
        }
    }
}

function parentWithClass(dom, cls) {
    var node = dom.parentNode;
    while (node && !node.classList.contains(cls)) {
        node = node.parentNode;
    }
    return node;
}

function anchorLink() {
    var page = document.getElementsByClassName('page')[0];
    if (!page) {
        return;
    }
    var levels = {
        'h1': true,
        'h2': true,
        'h3': true,
        'h4': true,
        'h5': true,
        'h6': true
    };
    for (var i = 0, firstHeading = true; i < page.children.length; i++) {
        var node = page.children[i];
        var tagName = node.tagName.toLowerCase();
        if (levels[tagName]) {
            var a = document.createElement('a');
            if (firstHeading && i < 2 && tagName === 'h1') {
                a.href = autumn.pathname();
            } else {
                a.href = ('#' + node.id);
            }
            if (isMobi) {
                a.appendChild(createAnchorIcon());
                node.appendChild(a);
            } else {
                a.classList.add('heading__anchor');
                node.classList.add('heading');
                node.insertBefore(a, node.firstChild);
            }
            firstHeading = false;
        }
    }
}

function createAnchorIcon() {
    var svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.classList.add('heading__anchor__icon');
    svg.setAttribute('width', '16');
    svg.setAttribute('height', '16');
    svg.setAttribute('viewBox', '0 0 512 512');
    svg.setAttribute('focusable', 'false');
    var path = document.createElementNS("http://www.w3.org/2000/svg", "path");
    path.setAttribute("d", "M326.612 185.391c59.747 59.809 58.927 155.698.36 214.59-.11.12-.24.25-.36.37l-67.2 67.2c-59.27 59.27-155.699 59.262-214.96 0-59.27-59.26-59.27-155.7 0-214.96l37.106-37.106c9.84-9.84 26.786-3.3 27.294 10.606.648 17.722 3.826 35.527 9.69 52.721 1.986 5.822.567 12.262-3.783 16.612l-13.087 13.087c-28.026 28.026-28.905 73.66-1.155 101.96 28.024 28.579 74.086 28.749 102.325.51l67.2-67.19c28.191-28.191 28.073-73.757 0-101.83-3.701-3.694-7.429-6.564-10.341-8.569a16.037 16.037 0 0 1-6.947-12.606c-.396-10.567 3.348-21.456 11.698-29.806l21.054-21.055c5.521-5.521 14.182-6.199 20.584-1.731a152.482 152.482 0 0 1 20.522 17.197zM467.547 44.449c-59.261-59.262-155.69-59.27-214.96 0l-67.2 67.2c-.12.12-.25.25-.36.37-58.566 58.892-59.387 154.781.36 214.59a152.454 152.454 0 0 0 20.521 17.196c6.402 4.468 15.064 3.789 20.584-1.731l21.054-21.055c8.35-8.35 12.094-19.239 11.698-29.806a16.037 16.037 0 0 0-6.947-12.606c-2.912-2.005-6.64-4.875-10.341-8.569-28.073-28.073-28.191-73.639 0-101.83l67.2-67.19c28.239-28.239 74.3-28.069 102.325.51 27.75 28.3 26.872 73.934-1.155 101.96l-13.087 13.087c-4.35 4.35-5.769 10.79-3.783 16.612 5.864 17.194 9.042 34.999 9.69 52.721.509 13.906 17.454 20.446 27.294 10.606l37.106-37.106c59.271-59.259 59.271-155.699.001-214.959z");
    svg.appendChild(path);
    return svg;
}

function bindToggle(targetClass) {
    var toggleClass = targetClass + '_toggle';
    var toggle = document.getElementsByClassName(toggleClass)[0];
    var target = document.getElementsByClassName(targetClass)[0];
    if (!toggle || !target) {
        return;
    }
    toggle.addEventListener('click', function () {
        target.classList.toggle('show');
    });
}







