window.addEventListener('load', buildTree);

function buildTree() {
    var url = '/tree.json';
    ajax('GET', url, function (text) {

        root = JSON.parse(text);
        console.log(root);

        unfoldPath(root);
        var tree = document.getElementsByClassName('tree')[0];
        tree.innerHTML = createHtmlNode(root);
        bindToggle(tree);
    });

    function unfoldPath(root) {
        var path = location.pathname;
        var dirs = [root];
        var current;
        while (dirs.length > 0) {
            var dir = dirs.pop();
            for (var i = 0; i < dir.children.length; i++) {
                var node = dir.children[i];
                node.parent = dir;
                if (node.path == path) {
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
            var parent = current.parent;
            while (parent) {
                parent.unfolded = true;
                parent = parent.parent;
            }
        }
    }

    var foldedString = '+';
    var unfoldedString = '−';
    var leafString = '•';

    function createHtmlNode(node) {
        var html = '<div class="node ' + (node.unfolded ? 'unfolded' : 'folded') + '">';
        html += '<div class="title' + (node.current ? ' selected' : '') + '">';
        html += '<span class="icon">';
        if (node.children) {
            html += (node.unfolded ? unfoldedString : foldedString);
        } else {
            html += leafString;
        }
        html += '</span>';
        if (node.children) {
            html += '<a>';
        } else {
            html += '<a href="' + node.path + '">';
        }
        html += node.name;
        html += '</a></div>'; // end title

        if (node.children) {
            html += '<div class="children">';
            for (var i = 0; i < node.children.length; i++) {
                html += createHtmlNode(node.children[i]);
            }
            html += '</div>';
        }
        html += '</div>';
        return html;
    }

    function bindToggle(tree) {
        var childrenArray = tree.getElementsByClassName('children');
        for (var i = 0; i < childrenArray.length; i++) {
            childrenArray[i]
                .parentNode
                .getElementsByClassName('icon')[0]
                .addEventListener('click', toggle);
        }
    }

    function toggle(event) {
        var target = event.target;
        var node = target.parentNode.parentNode;
        var folded = /\bfolded\b/.test(node.className);
        if (folded) {
            target.innerHTML = unfoldedString;
            node.className = node.className.replace(/\bfolded\b/, 'unfolded');
        } else {
            target.innerHTML = foldedString;
            node.className = node.className.replace(/\bunfolded\b/, 'folded');
        }
    }
}

function ajax(method, url, success, error) {
    var r = new XMLHttpRequest();
    r.open(method, url, true);
    r.onreadystatechange = function () {
        if (r.readyState != 4) return;
        if (r.status == 200) {
            if (typeof(success == 'function')) success(r.responseText);
        } else {
            if (typeof(error == 'function')) error(r.responseText);
        }
    };
    r.send("");
}





