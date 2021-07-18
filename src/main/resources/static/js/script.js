"use strict";
var au = util();
var ctx = autumn.ctx;
var prefix = autumn.prefix;
var treeJsonUrl = autumn.treeJsonUrl;
var localStorageRecentVisitKey = prefix + 'recently_visit';
var recentlyVisitMaxCount = 100;
var recentlyVisitPages;
var logoutCookieName = 'logout';
var isMobi = /Mobi/.test(navigator.userAgent);
var alwaysUnfoldRoot = false;
var multipleSelectionEnabled = false;
var containerNode;
var mainNode;
var contentNode;
var treeReady = false;
var treeFirstShow = true;
var isFixed = false;
var fixedClassName = 'fixed';
var lsFixedKey = prefix + 'fixed';
var toggleSidebar = au.emptyFn;
var toggleToc = au.emptyFn;
detectClient();
window.addEventListener('load', function () {
    initVars();
    bindFixedToggle();
    bindSidebarToggle();
    bindTocToggle();
    bindShortcut();
    loadSitemapTree(function (sitemapData) {
        setupQuickSearch(sitemapData);
        updateVisitList();
        recentlyModified();
    });
    // sitemap 移至单独页面
    // bindSitemapToggle();
    anchorLink();
    checkLogout();
    onLoad();
});

function detectClient() {
    var html = au.el('html');
    html.classList.add('js');
    if (!isMobi) {
        html.classList.remove('mobi');
        html.classList.add('desktop');
    }
}

function onLoad() {
    if (autumn.onLoad) {
        autumn.onLoad();
    }
}

function initVars() {
    containerNode = au.el('.container');
    mainNode = au.el('.main');
    contentNode = au.el('.content');
}

function bindSidebarToggle() {
    var toggle = au.el('.sidebar_toggle');
    var sidebar = au.el('.sidebar');
    var toolbar = au.el('.header__toolbar');
    if (!toggle || !sidebar) {
        return;
    }
    var lsKey = prefix + 'sidebar.display';
    if (getComputedStyle(mainNode).getPropertyValue('flex-direction') === 'row') {
        internalToggleSidebar(localStorage.getItem(lsKey) !== '0'); // 默认展开
    }
    toggle.addEventListener('click', toggleSidebarAndRemember);
    toggleSidebar = toggleSidebarAndRemember;

    function internalToggleSidebar(show) {
        if (show == null) {
            show = containerNode.classList.toggle('show_sidebar');
        } else {
            containerNode.classList.toggle('show_sidebar', show);
        }
        if (show) {
            sidebar.prepend(toggle);
        } else {
            toolbar.prepend(toggle);
        }
        return show;
    }

    function toggleSidebarAndRemember() {
        var show = (internalToggleSidebar() ? '1' : '0');
        localStorage.setItem(lsKey, show);
        if (show) {
            selectedNodeScrollIntoViewIfTreeFirstShow();
        }
    }
}

function bindTocToggle() {
    // CSS 已设置 TOC body 默认隐藏
    var toc = au.el('.toc');
    if (!toc) {
        return;
    }
    var toggle = au.el('h3', toc) // markdown
        || au.el('#toctitle', toc); // asciidoc

    var tocBody;
    if (!toggle || !(tocBody = toggle.nextElementSibling) || au.els('li', tocBody).length < 3) {
        toc.style.display = 'none';
        return;
    }
    toggle.classList.add('no_selection', 'action_toggle');
    var lsKey = prefix + 'toc.display';
    internalToggleToc(localStorage.getItem(lsKey) !== '0');
    toggle.addEventListener('click', toggleTocAndRemember);
    toggleToc = toggleTocAndRemember;

    function internalToggleToc(show) {
        if (show == null) {
            show = !contentNode.classList.toggle('hide_toc');
        } else {
            contentNode.classList.toggle('hide_toc', !show);
        }
        return show;
    }

    function toggleTocAndRemember() {
        localStorage.setItem(lsKey, internalToggleToc() ? '1' : '0');
    }
}

function bindShortcut() {
    var searchInput = au.el('.header__row_1__search_input');
    document.addEventListener('keydown', function (event) {
        if (event.target === document.body && !event.altKey && !event.ctrlKey && !event.metaKey) {
            switch (event.key) {
                case '/':
                    if (searchInput) {
                        searchInput.focus();
                        searchInput.select();
                        event.preventDefault();
                    }
                    break;
                case 'f': // 已弃用
                    toggleFixedAndRemember();
                    break;
                case 's': // 已弃用
                    toggleSidebar();
                    break;
                case 't':
                    toggleToc();
                    break;
                case 'g':
                    scroll(0, 0);
                    break;
                case 'G':
                    scroll(0, document.scrollingElement.scrollHeight);
                    break;
                case 'u':
                    document.scrollingElement.scrollTop -= au.getViewportHeight() / 2;
                    break;
                case 'd':
                    document.scrollingElement.scrollTop += au.getViewportHeight() / 2;
                    break;
                case 'j':
                    document.scrollingElement.scrollTop += 48;
                    break;
                case 'k':
                    document.scrollingElement.scrollTop -= 48;
                    break;
            }
        }
    });
}

function loadSitemapTree(then) {
    var treeBox = au.el('.tree_box');
    if (au.els('ul', treeBox).length > 0) {
        unfoldSitemapHash();
        bindNodeToggle(treeBox);
        treeReady = true;
        selectedNodeScrollIntoViewIfTreeFirstShow();
    }

    au.ajax('GET', treeJsonUrl, function (text) {
        var root = JSON.parse(text);

        if (treeBox) {
            if (au.els('ul', treeBox).length === 0) {
                unfoldCurrentPath(root);
                treeBox.innerHTML = buildTreeHtml(root.children);
                bindNodeToggle(treeBox);
                treeReady = true;
                selectedNodeScrollIntoViewIfTreeFirstShow();
            }
        }
        if (typeof (then) === 'function') {
            then(root);
        }
    });

    function unfoldCurrentPath(root) {
        // 找到当前页面节点
        var path = au.pathname().substr(ctx.length);
        var current;
        if (path === '/') { // 首页
            root.unfolded = true;
            return;
        } else { // 非首页
            var dirs = [root];
            OUTER:
                while (dirs.length > 0) {
                    var dir = dirs.pop();
                    for (var i = 0; i < dir.children.length; i++) {
                        var node = dir.children[i];
                        node.parent = dir;
                        if (node.path === path) {
                            node.current = true;
                            current = node;
                            break OUTER;
                        }
                        if (node.children) {
                            dirs.push(node);
                        }
                    }
                }
        }

        // 展开所有父级
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

    function unfoldSitemapHash() {
        var sitemapRoot = au.el('.sitemap');
        au.els('.tree_node_dir').forEach(function (el) {
            el.classList.remove('tree_node_unfolded');
            el.classList.add('tree_node_folded');
        });

        if (!location.hash) {
            return;
        }
        var path = decodeURIComponent(location.hash.substring(1));
        var el = au.el('.sitemap a[href="' + path + '"]');
        if (!el) {
            return;
        }
        el = el.parentElement; // div.tree_node_header
        el.classList.add('tree_node_header_selected');
        while (el && el !== sitemapRoot) {
            if (el.tagName === 'LI' && el.classList.contains('tree_node_dir')) {
                el.classList.remove('tree_node_folded');
                el.classList.add('tree_node_unfolded');
            }
            el = el.parentElement;
        }
    }

    function buildTreeHtml(nodes) {
        if (!nodes || nodes.length === 0) {
            return '';
        }
        var html = '<ul>';
        for (var i = 0; i < nodes.length; i++) {
            var node = nodes[i];
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
            html += '<span class="tree_node_header_icon no_selection"></span>';

            // title
            html += node.children ? '<span class="tree_node_header_name no_selection">' : ('<a href="' + (ctx + node.path) + '">');
            html += node.name;
            html += node.children ? '</span>' : '</a>';

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
        au.els('.tree_node_dir', tree).forEach(function (el) {
            au.el('.tree_node_header', el).addEventListener('click', toggle);
        });
    }

    function toggle(event) {
        var node = event.currentTarget.parentNode; // tree_node
        if (node.classList.contains('tree_node_folded')) {
            au.replaceClass(node, 'tree_node_folded', 'tree_node_unfolded');
        } else {
            au.replaceClass(node, 'tree_node_unfolded', 'tree_node_folded');
        }
    }
}

function bindSitemapToggle() {
    var toogle = au.el('.sitemap_toggle');
    if (!toogle) {
        return;
    }
    var sitemap = au.el('.sitemap');
    toogle.addEventListener('click', function () {
        sitemap.classList.toggle('show');
    });
}

function selectedNodeScrollIntoViewIfTreeFirstShow() {
    if (treeFirstShow && treeReady && containerNode.classList.contains('show_sidebar')) {
        treeFirstShow = false;
        if (getComputedStyle(mainNode).getPropertyValue('flex-direction') === 'row' && !isFixed) {
            return;
        }
        var selected = au.el('.tree_node_header_selected');
        if (!selected) {
            return;
        }
        au.scrollToCenter(selected);
    }
}

function anchorLink() {
    var levels = {
        'h1': true,
        'h2': true,
        'h3': true,
        'h4': true,
        'h5': true,
        'h6': true
    };

    var anchors = au.els('.heading > a.anchor', contentNode);
    for (var i = 0; i < anchors.length; i++) {
        var anchor = anchors[i];
        var hNode = anchor.parentNode;
        var tagName = hNode.tagName.toLowerCase();
        if (!levels[tagName]) {
            continue;
        }
        if (!isMobi) {
            hNode.insertBefore(anchor, hNode.firstChild);
        }
    }
}

function bindFixedToggle() {
    toggleFixed(localStorage.getItem(lsFixedKey) === '1');
    var toggle = au.el('.search_icon');
    if (!toggle) {
        return;
    }
    toggle.addEventListener('click', toggleFixedAndRemember);
}

function toggleFixedAndRemember() {
    localStorage.setItem(lsFixedKey, toggleFixed() ? '1' : '0');
}

function toggleFixed(fixed) {
    if (fixed == null) {
        fixed = containerNode.classList.toggle(fixedClassName);
    } else {
        containerNode.classList.toggle(fixedClassName, fixed);
    }
    isFixed = fixed;
    return fixed
}

function checkLogout() {
    if (au.getCookie(logoutCookieName)) {
        localStorage.removeItem(localStorageRecentVisitKey);
        au.deleteCookie(logoutCookieName);
    }
}