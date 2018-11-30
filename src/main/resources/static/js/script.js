"use strict";
(function () {
    window.addEventListener('load', function () {
        bindSidebarToggle();
        bindTocToggle();
        bindShortcut();
        buildTree();
    });

    function bindSidebarToggle() {
        var toggle = document.getElementsByClassName('sidebar-toggle')[0];
        var body = document.getElementsByClassName('sidebar')[0];
        if (!toggle || !body) {
            return;
        }
        toggle.addEventListener('click', function (event) {
            if (body.style.display === 'none') {
                body.style.display = '';
            } else {
                body.style.display = 'none';
            }
            event.preventDefault();
            event.stopPropagation();
        });
    }

    function bindTocToggle() {
        // var toc = document.getElementsByClassName('toc')[0];
        // var toggle = toc.getElementsByTagName('h1')[0];
        // var body = toggle.nextElementSibling;
        var toggle = document.getElementsByClassName('toc-toggle')[0];
        var body = document.getElementsByClassName('toc')[0];
        if (!toggle || !body) {
            if (toggle) {
                toggle.style.display = 'none';
            }
            return;
        }
        toggle.addEventListener('click', function () {
            if (body.style.display === 'none') {
                body.style.display = '';
            } else {
                body.style.display = 'none';
            }
        });
    }

    function bindShortcut() {
        // 按 '/' 搜索框获得焦点，按 'ESC' 搜索框失去焦点
        var searchInput = document.getElementById('search-input');
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
        var tree = document.getElementsByClassName('tree')[0];
        if (!tree) {
            return; // 可能在登录页
        }
        ajax('GET', '/tree.json', function (text) {
            var root = JSON.parse(text);
            unfoldPath(root);
            tree.innerHTML = buildTreeHtml([root]);
            bindToggle(tree);
        });

        function unfoldPath(root) {
            var path = decodeURIComponent(location.pathname);
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
                html += '<li class="node';
                if (node.children) {
                    html += ' fold';
                    html += (node.unfolded ? ' unfolded' : ' folded');
                } else {
                    html += ' leaf';
                }
                html += '">';

                // begin header
                html += '<div class="header' + (node.current ? ' selected' : '') + '">';

                // icon
                html += '<span class="icon">';
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
            var els = tree.getElementsByClassName('fold');
            for (var i = 0; i < els.length; i++) {
                els[i].getElementsByClassName('header')[0].addEventListener('click', toggle);
            }
        }

        function toggle(event) {
            var target = event.target;
            var node = parentWithClass(target, 'node');
            var icon = node.getElementsByClassName('icon')[0];
            if (hasClass(node, 'folded')) {
                icon.innerHTML = unfoldedString;
                replaceClass(node, 'folded', 'unfolded');
            } else {
                icon.innerHTML = foldedString;
                replaceClass(node, 'unfolded', 'folded');
            }
        }
    }

    function hasClass(dom, cls) {
        return new RegExp('\\b' + cls + '\\b').test(dom.className);
    }

    function replaceClass(dom, cls, replacement) {
        dom.className = dom.className.replace(new RegExp('\\b' + cls + '\\b', 'g'), replacement);
    }

    function parentWithClass(dom, cls) {
        var node = dom.parentNode;
        while (node && !hasClass(node, cls)) {
            node = node.parentNode;
        }
        return node;
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





