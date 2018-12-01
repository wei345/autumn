"use strict";
(function () {
    window.addEventListener('load', function () {
        bindSidebarToggle();
        bindTocToggle();
        bindShortcut();
        anchorLink();
        buildTree();
    });

    function bindSidebarToggle() {
        var toggle = document.getElementsByClassName('header__toolbar__sidebar_toggle')[0];
        var target = document.getElementsByClassName('sidebar')[0];
        if (!toggle || !target) {
            return;
        }
        var lsKey;
        switch (pathname()) {
            case '/':
                lsKey = 'autumn.home.sidebar.display';
                break;
            case '/search':
                lsKey = 'autumn.search.sidebar.display';
                break;
            default:
                lsKey = 'autumn.sidebar.display';
                break;
        }
        var main = document.getElementsByClassName('main')[0];
        main.classList.toggle('hide_sidebar', localStorage.getItem(lsKey) === '0');
        toggle.addEventListener('click', function () {
            var val = main.classList.toggle('hide_sidebar') ? '0' : '1';
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
        toggle.classList.add('no_selection');
        var lsKey = 'autumn.toc.display';
        bindToggle(toggle, body, lsKey);

    }

    function bindToggle(toggle, body, lsKey) {
        if (localStorage.getItem(lsKey) === '0') {
            body.style.display = 'none';
        } else {
            body.style.display = 'block';
        }
        toggle.addEventListener('click', function () {
            if (body.style.display === 'none') {
                body.style.display = 'block';
                localStorage.setItem(lsKey, '1');
            } else {
                body.style.display = 'none';
                localStorage.setItem(lsKey, '0');
            }
        });
    }

    function bindShortcut() {
        // 按 '/' 搜索框获得焦点，按 'ESC' 搜索框失去焦点
        var searchInput = document.getElementsByClassName('header__1__search_input')[0];
        if (!searchInput) {
            return; // 可能在登录页
        }
        document.addEventListener('keydown', function (event) {
            if (event.key === '/' && document.activeElement !== searchInput) {
                searchInput.focus();
                searchInput.select();
                event.preventDefault();
            }
            if (event.key === 'Escape' && document.activeElement === searchInput) {
                searchInput.blur();
            }
        });
    }

    function buildTree() {
        var treeBox = document.getElementsByClassName('tree_box')[0];
        if (!treeBox) {
            return; // 可能在登录页
        }
        ajax('GET', '/tree.json', function (text) {
            var root = JSON.parse(text);
            unfoldCurrentPath(root);
            treeBox.innerHTML = buildTreeHtml([root]);
            bindToggle(treeBox);
        });

        function unfoldCurrentPath(root) {
            var path = decodeURIComponent(pathname());
            var dirs = [root];
            var current;
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

            if (current) {
                var parent = current;
                while (parent = parent.parent) {
                    parent.unfolded = true;
                }
            }
        }

        var foldedString = '+';
        var unfoldedString = '−';

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
                    html += (node.unfolded ? unfoldedString : foldedString);
                } else {
                    html += '•';
                }
                html += '</span>';

                // title
                html += (node.children ? '<span>' : ('<a href="' + node.path + '">'));
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

        function bindToggle(tree) {
            var els = tree.getElementsByClassName('tree_node_dir');
            for (var i = 0; i < els.length; i++) {
                els[i].getElementsByClassName('tree_node_header')[0].addEventListener('click', toggle);
            }
        }

        function toggle(event) {
            var target = event.target;
            var node = parentWithClass(target, 'tree_node');
            var icon = node.getElementsByClassName('tree_node_header_icon')[0];
            if (node.classList.contains('tree_node_folded')) {
                icon.innerHTML = unfoldedString;
                replaceClass(node, 'tree_node_folded', 'tree_node_unfolded');
            } else {
                icon.innerHTML = foldedString;
                replaceClass(node, 'tree_node_unfolded', 'tree_node_folded');
            }
        }
    }

    // Safari 不支持 classList.replace
    function replaceClass(dom, cls, replacement) {
        dom.className = dom.className.replace(new RegExp('\\b' + cls + '\\b', 'g'), replacement);
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
            'h6': true,
            'h7': true
        };
        for (var i = 0; i < page.children.length; i++) {
            var node = page.children[i];
            var tagName = node.tagName.toLowerCase();
            if (levels[tagName]) {
                var a = document.createElement('a');
                if (tagName === 'h1') {
                    a.href = (pathname());
                } else {
                    a.href = ('#' + node.id);
                }
                node.insertBefore(a, node.firstChild);
            }
        }
    }

    function pathname() {
        if (location.pathname) {
            return location.pathname;
        }
        var pathname = /^(\w+?):\/+?[^\/]+(\/[^?]*)/.exec(location.href)[2];
        return pathname ? pathname : pathname;
    }

    function ajax(method, url, success, error) {
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
    }

})();





