"use strict";

function setupQuickSearch(root) {
    var searchInput = document.getElementsByClassName('header__row_1__search_input')[0];
    var cat = document.getElementsByClassName('search_box__category_and_tags')[0];
    var catToggle = document.getElementsByClassName('header__row_1__search_form__category_and_tags_toggle')[0];
    var qsr = document.getElementsByClassName('qsr')[0];
    var qsrClose = document.getElementsByClassName('qsr__close')[0];
    var qsrAllPages;
    var qsrList;
    var qsrMaxLines = 6;
    var qsrSelectedIndex = -1;
    var categoryPrefix = 'c:';
    var tagPrefix = 't:';
    var foldedString = '+';
    var unfoldedString = '−';
    var catToggleEnabled = false;
    var qsOpened = false;
    var pathToPage; // path -> page
    var allPages;
    var lastS;
    var pendingQs = 0;
    var qsTimeoutId;

    getAllPages(root);
    setUpCategoryAndTags();

    // 如果用 blur 关闭 qs，则无法用鼠标点击搜索结果，因为在鼠标点到链接之前 QuickSearch 就关闭了

    searchInput.addEventListener('focus', function () {
        openCat();
        qs(true);
    });

    searchInput.addEventListener('keydown', function (event) {

        // 上下移动
        var down = (event.key === 'ArrowDown');
        var up = (event.key === 'ArrowUp');
        if (down || up) {
            if (qsrSelectedIndexInBound()) {
                unSelect();
                qsrSelectedIndex += (down ? 1 : -1);
            } else {
                qsrSelectedIndex = (down ? 0 : (qsrList.children.length - 1));
            }
            select();
            event.preventDefault();
            return;
        }

        // 回车跳转到链接或显示全部结果
        if (event.key === 'Enter') {
            if (!qsrSelectedIndexInBound()) {
                return;
            }
            event.preventDefault();

            var selected = qsrList.children[qsrSelectedIndex];
            if (selected.classList.contains('qsr__list__show_all')) {
                showMoreResult();
                return;
            }

            var href = selected.getElementsByTagName('a')[0].href;
            if (href) {
                location.href = href;
            }
            return;
        }

        // ESC 取消选中，或清空输入框，或关闭 QuickSearch
        if (event.key === 'Escape') {
            if (qsrSelectedIndexInBound()) {
                resetSelect();
                event.preventDefault();
                return;
            }

            if (searchInput.value.length > 0) {
                searchInput.value = ''; // 之后 keyup 会触发 qs()
                return;
            }

            searchInput.blur();
            qs();
        }
    });

    searchInput.addEventListener('keyup', function () {
        qs();
    });

    document.addEventListener('click', function () {
        if (qsOpened) {
            qs();
        }
    });

    qsrClose.addEventListener('click', closeQs);

    document.getElementsByClassName('header__row_1__search_form')[0].addEventListener('click', function (evt) {
        // 避免搜索框为空时，点击搜索结果（最近访问），qs 关闭
        evt.stopPropagation();
    });

    function resetSelect() {
        unSelect();
        qsrSelectedIndex = -1;
    }

    function select() {
        if (qsrSelectedIndexInBound()) {
            qsrList.children[qsrSelectedIndex].classList.add('qsr_activation');
        }
    }

    function unSelect() {
        if (qsrSelectedIndexInBound()) {
            qsrList.children[qsrSelectedIndex].classList.remove('qsr_activation');
        }
    }

    function qsrSelectedIndexInBound() {
        return qsrSelectedIndex >= 0 && qsrSelectedIndex < qsrList.children.length;
    }

    function getAllPages(root) {
        pathToPage = {};
        var pages = [];
        var dirs = [root];
        var dir;
        while (dir = dirs.pop()) {
            for (var i = 0; i < dir.children.length; i++) {
                var node = dir.children[i];
                if (node.children) {
                    dirs.push(node);
                } else {
                    pages.push(node);
                    pathToPage[node.path] = node;
                }
            }
        }
        pages.forEach(function (page) {
            page.archived = page.path.startsWith('/archive/');
        });
        allPages = pages;
    }

    function openCat() {
        if (!catToggleEnabled && !cat.classList.contains('show')) {
            cat.classList.toggle('show', true);
            catToggle.innerHTML = unfoldedString;
        }
    }

    function closeCat() {
        if (cat.classList.contains('show')) {
            cat.classList.toggle('show', false);
            catToggle.innerHTML = foldedString;
        }
    }

    function setUpCategoryAndTags() {
        var categoryFieldOfPage = 'category';
        var tagFieldOfPage = 'tags';
        var html = mr(allPages, categoryFieldOfPage, '分类');
        html += mr(allPages, tagFieldOfPage, '标签');
        cat.innerHTML = html;
        bindClick(categoryFieldOfPage);
        bindClick(tagFieldOfPage);
        bindCatToggle();

        function mr(arr, field, title) {
            var a = [];
            arr.filter(function (page) {
                var v = page[field];
                return v instanceof Array ? v.length > 0 : v;
            }).map(function (page) {
                var v = page[field];
                return v instanceof Array ? v : [v];
            }).reduce(function (result, v) {
                v.forEach(function (vitem) {
                    if (!result[vitem]) {
                        var obj = {count: 1};
                        obj[field] = vitem;
                        result[vitem] = obj;
                        a.push(obj);
                    } else {
                        result[vitem].count++;
                    }
                });
                return result;
            }, {});

            a.sort(function (o1, o2) {
                var v = o2.count - o1.count;
                if (v !== 0) {
                    return v;
                }
                return compareStringIgnoreCase(o1[field], o2[field]);
            });

            var html = '<div class="' + field + '_box">';
            html += '<span class="search_box__' + field + '_list_title">' + title + '</span>';
            html += '<ul class="search_box__' + field + '_list">';
            a.forEach(function (item) {
                html += '<li><span class="' + (field === 'tags' ? 'tag' : field) + '">' + item[field] + '</span>';
                html += '<span class="count"> (' + item.count + ') </span>';
                html += '</li>';
            });
            html += '</ul></div>';
            return html;
        }

        function bindClick(field) {
            var categoryOrTag = field === 'category' ? 'category' : 'tag';

            var els = document.getElementsByClassName('search_box__' + field + '_list')[0].getElementsByTagName('li');
            for (var i = 0; i < els.length; i++) {
                els[i].addEventListener('click', function (evt) {
                    categoryOrTagOnClick(evt, categoryOrTag);
                });
            }

            var selectedCategory;

            function categoryOrTagOnClick(event, categoryOrTag) {
                var isTag = categoryOrTag === 'tag';
                var li = event.currentTarget;
                var prefix = isTag ? tagPrefix : categoryPrefix;
                var v = li.getElementsByClassName(categoryOrTag)[0].innerText;
                var t = prefix + v;
                var add = true;
                var newValueBuilder = [];
                parseS(searchInput.value).forEach(function (token) {
                    if (token === t) {
                        add = false;
                        return;
                    }
                    if (isTag) {
                        newValueBuilder.push(token);
                    } else if (!token.startsWith(prefix)) {
                        newValueBuilder.push(token);
                    }
                });
                var newValue = newValueBuilder.join(' ');
                if (add) {
                    newValue = t + ' ' + newValue;
                    if (!isTag) {
                        if (selectedCategory) {
                            selectedCategory.classList.remove('cat_activation');
                        }
                        selectedCategory = li;
                    }
                }
                li.classList.toggle('cat_activation', add);
                searchInput.value = newValue;
                searchInput.focus();
                qs();
            }
        }

        function bindCatToggle() {
            catToggle.addEventListener('click', function () {
                var show = cat.classList.toggle('show');
                catToggle.innerHTML = show ? unfoldedString : foldedString;
                if (show || searchInput.value.length > 0) {
                    searchInput.focus();
                }
            });
            // 初始状态
            catToggle.innerHTML = foldedString;
            catToggleEnabled = true;
        }
    }

    function qs(immediately) {
        // 控制搜索频率
        if (qsTimeoutId) {
            if (immediately) {
                clearTimeout(qsTimeoutId);
            } else {
                pendingQs++;
                return;
            }
        }
        pendingQs = 0;
        qsTimeoutId = setTimeout(function () {
            qsTimeoutId = null;
            if (pendingQs > 0) {
                qs();
            }
        }, 200);

        // 搜索
        // var start = new Date().getTime();
        quickSearch();
        // console.log('qs ' + (new Date().getTime() - start) + ' ms'); // 0 - 10 ms
    }

    function quickSearch() {
        var s = searchInput.value;

        if (qsOpened && s === '' && document.activeElement !== searchInput) {
            closeCat();
            clearResult();
            lastS = null;
            qsOpened = false;
            document.body.classList.remove('qs_opened');
            return;
        }

        qsOpened = true;
        document.body.classList.add('qs_opened');

        s = s.trim();
        if (lastS === s) {
            return;
        }
        lastS = s;

        if (s === '') {
            showRecentlyVisit();
            return;
        }

        doSearch(s);
    }

    function doSearch(s) {
        var tokens = parseS(s);
        var pages = allPages;
        pages.forEach(function (page) {
            page.searching = {
                hitMap: {},
                nameHitList: [],
                titleHitList: [],
                pathHitList: [],
                pathPreview: '',
                titlePreview: '',
                nameEqCount: 0,
                titleEqCount: 0,
                titleHitCount: 0,
                nameHitCount: 0,
                pathHitCount: 0,
                hitCount: 0
            }
        });
        tokens.forEach(function (token) {
            // 搜索分类
            if (token.startsWith(categoryPrefix)) {
                var category = token.substr(categoryPrefix.length);
                if (category.length > 0) {
                    pages = searchCategory(pages, category);
                    return;
                }
            }
            // 搜索标签
            if (token.startsWith(tagPrefix)) {
                var tag = token.substr(tagPrefix.length);
                if (tag.length > 0) {
                    pages = searchTag(pages, tag);
                    return;
                }
            }
            // 搜索 name, title, path
            if (token.length > 1 && token.charAt(0) === '-') {
                var searchStr = token.substr(1);
                pages = searchPagesExclude(pages, searchStr);
                return;
            }
            pages = searchPagesInclude(pages, token);
        });
        sort(pages);
        highlight(pages);
        showSearchResult(pages);
    }

    function parseS(s) {
        s = s.trim();
        if (s.length === 0) {
            return [];
        }
        return s.split(/\s+/);
    }

    function searchCategory(pages, category) {
        return pages.filter(function (page) {
            return page.category === category;
        });
    }

    function searchTag(pages, tag) {
        return pages.filter(function (page) {
            return page.tags.includes(tag);
        })
    }

    function searchPagesInclude(pages, searchStr) {
        return pages.filter(function (page) {
            return searchPage(page, searchStr).hitCount > 0;
        });
    }

    function searchPagesExclude(pages, searchStr) {
        return pages.filter(function (page) {
            return searchPage(page, searchStr).hitCount === 0;
        });
    }

    function searchPage(page, searchStr) {
        var search = searchStr.toLowerCase();

        // page-searchStr 缓存
        if (!page.hitMap) {
            page.hitMap = {};
        }
        var pageHit = page.hitMap[search];
        if (!pageHit) {
            pageHit = {
                nameEq: equalsIgnoreCase(page.name, search),
                titleEq: equalsIgnoreCase(page.title, search),
                nameHitList: searchString(page.name, search),
                titleHitList: searchString(page.title, search),
                pathHitList: searchString(page.path, search)
            };
            pageHit.hitCount = pageHit.titleHitList.length + pageHit.pathHitList.length;
            page.hitMap[search] = pageHit;
        }

        var ps = page.searching;
        if (pageHit.hitCount > 0) {
            // 当前搜索累加
            if (pageHit.nameEq) {
                ps.nameEqCount++;
            }
            if (pageHit.titleEq) {
                ps.titleEqCount++;
            }
            ps.titleHitCount += pageHit.titleHitList.length;
            ps.nameHitCount += pageHit.nameHitList.length;
            ps.pathHitCount += pageHit.pathHitList.length;
            ps.hitCount += pageHit.hitCount;
            ps.nameHitList = ps.nameHitList.concat(pageHit.nameHitList);
            ps.titleHitList = ps.titleHitList.concat(pageHit.titleHitList);
            ps.pathHitList = ps.pathHitList.concat(pageHit.pathHitList);
        }
        ps.hitMap[searchStr] = pageHit;
        return pageHit;
    }

    function searchString(sourceStr, searchStr) {
        if (isEmpty(sourceStr) || isEmpty(searchStr)) {
            return [];
        }
        var source = sourceStr.toLowerCase();
        var search = searchStr.toLowerCase();
        var hits = [];
        var i = 0, start;
        while ((start = source.indexOf(search, i)) >= 0) {
            i = start + search.length;
            hits.push({
                start: start,
                end: i,
                searchStr: searchStr
            });
        }
        return hits;
    }

    function sort(pages) {
        pages.sort(function (o1, o2) {
            var ps1 = o1.searching;
            var ps2 = o2.searching;
            var v;

            // 文件名相等
            v = ps2.nameEqCount - ps1.nameEqCount;
            if (v !== 0) {
                return v;
            }

            // 标题相等
            v = ps2.titleEqCount - ps1.titleEqCount;
            if (v !== 0) {
                return v;
            }

            // 文件名匹配
            v = ps2.nameHitCount - ps1.nameHitCount;
            if (v !== 0) {
                return v;
            }

            // 标题匹配
            v = ps2.titleHitCount - ps1.titleHitCount;
            if (v !== 0) {
                return v;
            }

            // 非归档目录
            var v1 = o1.archived ? 1 : 0;
            var v2 = o2.archived ? 1 : 0;
            v = v1 - v2;
            if (v !== 0) {
                return v;
            }

            // 路径匹配
            v = ps2.pathHitCount - ps1.pathHitCount;
            if (v !== 0) {
                return v;
            }

            // hit count 大在前
            v = ps2.hitCount - ps1.hitCount;
            if (v !== 0) {
                return v;
            }

            // 最近修改日期
            v = o2.modified - o1.modified;
            if (v !== 0) {
                return v;
            }

            // 字典顺序
            return compareStringIgnoreCase(o1.path, o2.path);
        });
    }

    function highlight(pages) {

        pages.forEach(highlightPage);

        function highlightPage(page) {
            var ps = page.searching;
            ps.titlePreview = highlightHitList(page.title, ps.titleHitList);
            ps.pathPreview = highlightHitList(page.path, ps.pathHitList);
        }

        function highlightHitList(str, hitList) {
            var hits = getNonOverlapHits(hitList);
            return highlightHits(str, hits);
        }

        function getNonOverlapHits(hitList) {
            var result = [];
            var prevHit;
            hitList.sort(function (a, b) {
                var v = a.start - b.start;
                if (v !== 0) {
                    return v;
                }
                // 长的优先
                return b.end - a.end;
            }).forEach(function (hit) {
                if (!prevHit || hit.start >= prevHit.end) {
                    result.push(hit);
                    prevHit = hit;
                }
            });
            return result;
        }

        function highlightHits(str, hits) {
            var hlTagOpen = "<em class=\"qsr__search_str\">";
            var hlTagClose = "</em>";

            var html = '';
            var start = 0;
            hits.forEach(function (hit) {
                html += escapeHtml(str.substring(start, hit.start));
                html += hlTagOpen;
                html += escapeHtml(str.substring(hit.start, hit.end));
                html += hlTagClose;
                start = hit.end;
            });

            if (start === 0) {
                return str;
            }

            if (start < str.length) {
                html += escapeHtml(str.substring(start, str.length));
            }

            return html;
        }

        function escapeHtml(unsafe) {
            return unsafe
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#039;");
        }

    }

    function isEmpty(str) {
        return str == null || str.length === 0;
    }

    function equalsIgnoreCase(str1, str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        if (str1.length !== str2.length) {
            return false;
        }
        return compareStringIgnoreCase(str1, str2) === 0;
    }

    // 可用于 Array.sort
    function compareStringIgnoreCase(s1, s2) {
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
    }

    function showSearchResult(pages) {
        qsrAllPages = pages;
        renderPages(pages, qsrMaxLines);
        qsrSelectedIndex = -1;
    }

    function showMoreResult() {
        renderPages(qsrAllPages);
        select();
    }

    function renderPages(pages, maxLines) {
        if (maxLines === undefined) {
            maxLines = pages.length;
        }
        renderQsrHtml(buildResultHtml(pages, maxLines));

        if (maxLines < pages.length) {
            var showAllBtn = qsrList.getElementsByClassName('qsr__list__show_all__btn')[0];
            if (showAllBtn) {
                showAllBtn.addEventListener('click', showMoreResult);
            }
        }
    }

    function renderQsrHtml(html) {
        qsr.innerHTML = html;
        qsrList = document.getElementsByClassName('qsr__list')[0];
        qsrClose.classList.toggle('show', true);
    }

    function showRecentlyVisit() {
        if (!recentlyVisitPages) {
            var currentPath = autumn.pathname();
            var pages = [];
            getVisitList().forEach(function (path) {
                if (path === currentPath) {
                    return;
                }
                pages.push({
                    path: path,
                    searching: {
                        titlePreview: getPageTitleByPath(path),
                        pathPreview: path
                    }
                });
            });
            recentlyVisitPages = pages;
        }
        showSearchResult(recentlyVisitPages);
    }

    function getPageTitleByPath(path) {
        var questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex === -1) {
            var page = pathToPage[path];
            if (page && page.title) {
                return page.title;
            }
            return path;
        }

        var p = path.substring(0, questionMarkIndex);
        if (p !== '/search' && p !== '/search/') {
            return path;
        }
        // search page
        var queryString = path.substr(questionMarkIndex);
        var s = parseQueryString(queryString).s;
        if (s) {
            return '搜索：' + s;
        }
        return path;
    }

    function parseQueryString(queryString) {
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
    }

    function buildResultHtml(pages, maxLength) {
        if (!pages || pages.length === 0) {
            return '';
        }
        var len = Math.min(pages.length, maxLength);
        var html = '<ul class="qsr__list">';
        var i = 0;
        for (; i < len; i++) {
            var page = pages[i];
            html += '<li>';
            html += createResultLink(page.path, page.searching.titlePreview, page.searching.pathPreview);
            html += '</li>';
        }

        if (i < pages.length) {
            html += '<li class="qsr__list__show_all">';
            html += '<span class="qsr__list__show_all__btn no_selection">' + (i + 1) + ' ... ' + pages.length + '</span>';
            html += '</li>';
        }

        html += '</ul>';
        return html;
    }

    function createResultLink(path, titlePreview, pathPreview) {
        if (titlePreview === '') {
            titlePreview = 'No Title';
        }
        var html = '<a href="' + autumn.ctx + path + '">';
        html += '<span class="qsr__list__page_title">' + titlePreview + '</span></br>';
        html += '<span class="qsr__list__page_path">' + pathPreview + '</span>';
        html += '</a>';
        return html;
    }

    function clearResult() {
        qsr.innerHTML = '';
        qsrList = null;
        qsrAllPages = null;
        qsrSelectedIndex = -1;
        qsrClose.classList.toggle('show', false);
    }

    function closeQs() {
        if (qsOpened) {
            searchInput.value = '';
            qs();
        }
    }
}

function checkLogout() {
    if (autumn.getCookie(logoutCookieName)) {
        localStorage.removeItem(lsRecentVisitKey);
        autumn.deleteCookie(logoutCookieName);
    }
}

function updateVisitList() {
    // 确定 currentPath
    var currentPath = autumn.pathname();
    if (autumn.ctx.length > 0) {
        currentPath = currentPath.substr(autumn.ctx.length);
    }
    if (currentPath === '/' || currentPath === '/login' || currentPath === '/help') {
        return;
    }
    if (document.getElementsByClassName('error_page').length > 0) {
        return;
    }
    if (currentPath === '/search') {
        var queryString = location.search;
        if (queryString.length <= 3) {
            return;
        }
        currentPath += queryString;
    }

    // 更新
    var oldVisitList = getVisitList();
    var newVisitList = [];
    var maxLength = recentlyVisitMaxCount;
    newVisitList.push(currentPath);
    for (var i = 0; i < oldVisitList.length && i < maxLength; i++) {
        if (oldVisitList[i] !== currentPath) {
            newVisitList.push(oldVisitList[i]);
        }
    }
    localStorage.setItem(lsRecentVisitKey, JSON.stringify(newVisitList));
}

function getVisitList() {
    var v = localStorage.getItem(lsRecentVisitKey);
    return v ? JSON.parse(v) : [];
}
