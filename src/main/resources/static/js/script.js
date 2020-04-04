"use strict";
var au = util();
var ctx = autumn.ctx;
var prefix = autumn.prefix;
var treeVersionKeyValue = autumn.treeVersionKeyValue;
var lsRecentVisitKey = prefix + 'recently_visit';
var recentlyVisitMaxCount = 100;
var recentlyVisitPages;
var logoutCookieName = 'logout';
var isMobi = /Mobi/.test(navigator.userAgent);
var alwaysUnfoldRoot = false;
var multipleSelectionEnabled = false;
var container = au.el('.container');
var main = au.el('.main');
var content = au.el('.content');
var treeReady = false;
var treeFirstShow = true;
var isFixed = false;
var fixedClassName = 'fixed';
var lsFixedKey = prefix + 'fixed';
var toggleSidebar = au.emptyFn;
var toggleToc = au.emptyFn;
window.addEventListener('load', function () {
    detectClient();
    bindFixedToggle();
    bindSidebarToggle();
    bindTocToggle();
    bindShortcut();
    buildTree(setupQuickSearch);
    bindSitemapToggle();
    anchorLink();
    checkLogout();
    updateVisitList();
});

function detectClient() {
    var html = document.querySelector('html');
    html.classList.add('js');
    html.classList.add(isMobi ? 'mobi' : 'desktop');
}

function bindSidebarToggle() {
    var toggle = au.el('.sidebar_toggle');
    var sidebar = au.el('.sidebar');
    var toolbar = au.el('.header__toolbar');
    if (!toggle || !sidebar) {
        return;
    }
    var lsKey = prefix + 'sidebar.display';
    if (getComputedStyle(main).getPropertyValue('flex-direction') === 'row') {
        internalToggleSidebar(localStorage.getItem(lsKey) !== '0'); // 默认展开
    }
    toggle.addEventListener('click', toggleSidebarAndRemember);
    toggleSidebar = toggleSidebarAndRemember;

    function internalToggleSidebar(show) {
        if (show == null) {
            show = container.classList.toggle('show_sidebar');
        } else {
            container.classList.toggle('show_sidebar', show);
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
    var toggle = au.el('h3', toc);
    var tocBody = toggle.nextElementSibling;
    if (!toggle || !tocBody || au.els('li', tocBody).length < 2) {
        if (toggle) {
            toggle.style.display = 'none';
        }
        return;
    }
    toggle.classList.add('no_selection', 'action_toggle');
    var lsKey = prefix + 'toc.display';
    internalToggleToc(localStorage.getItem(lsKey) !== '0');
    toggle.addEventListener('click', toggleTocAndRemember);
    toggleToc = toggleTocAndRemember;

    function internalToggleToc(show) {
        if (show == null) {
            show = content.classList.toggle('show_toc');
        } else {
            content.classList.toggle('show_toc', show);
        }
        if (show) {
            toggle.innerText = 'TOC';
        } else {
            toggle.innerText = 'TOC';
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
                case 'f':
                    toggleFixedAndRemember();
                    break;
                case 's':
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
            }
        }
    });
}

function buildTree(then) {
    var treeBox = au.el('.tree_box');

    if (!treeBox) {
        return; // 可能在登录页
    }
    au.ajax('GET', ctx + '/tree.json?' + treeVersionKeyValue, function (text) {
        var root = JSON.parse(text);
        unfoldCurrentPath(root);
        treeBox.innerHTML = buildTreeHtml(root.children);
        bindNodeToggle(treeBox);
        treeReady = true;
        selectedNodeScrollIntoViewIfTreeFirstShow();
        if (typeof(then) === 'function') {
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
        au.els('.tree_node_dir', tree).forEach(function(el){
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
    var sitemap = au.el('.sitemap');
    toogle.addEventListener('click', function () {
        sitemap.classList.toggle('show');
    });
}

function selectedNodeScrollIntoViewIfTreeFirstShow() {
    if (treeFirstShow && treeReady && container.classList.contains('show_sidebar')) {
        treeFirstShow = false;
        if (getComputedStyle(main).getPropertyValue('flex-direction') === 'row' && !isFixed) {
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
    var page = au.el('.page_content');
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
                a.href = au.pathname();
            } else {
                a.href = ('#' + node.id);
            }
            if (isMobi) {
                a.classList.add('heading__anchor__icon');
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
        fixed = container.classList.toggle(fixedClassName);
    } else {
        container.classList.toggle(fixedClassName, fixed);
    }
    isFixed = fixed;
    return fixed
}