"use strict";

function setupQuickSearch(root) {
    var searchInput = document.getElementsByClassName('header__row_1__search_input')[0];
    var categoryAndTags = document.getElementsByClassName('search_box__category_and_tags')[0];
    var categoryAndTagsToggle = document.getElementsByClassName('search_box__category_and_tags_toggle')[0];
    var qsrClose = document.getElementsByClassName('qsr__close')[0];
    var qsrList = document.getElementsByClassName('qsr__list')[0];
    var qsrAllPages;
    var qsrMaxLines = 6;
    var qsrSelectedIndex = -1;
    var categoryPrefix = 'c:';
    var tagPrefix = 't:';
    var qsOpened = false;
    var allPages;
    var pathToPage;
    var lastS;
    var pendingQs = 0;
    var qsTimeoutId;

    getAllPages(root);
    setUpCategoryAndTags();
    bindSearchInputEvent();
    bindQuickSearchCloseEvent();

    function bindSearchInputEvent() {
        searchInput.addEventListener('focus', function () {
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
    }

    function bindQuickSearchCloseEvent() {
        // 不用 blur 关闭 QuickSearch，因为会导致无法用鼠标点击搜索结果，在鼠标点到链接之前 QuickSearch 就关闭了

        qsrClose.addEventListener('click', function (event) {
            closeQs();
            event.stopPropagation();
        });

        document.addEventListener('click', function () {
            if (qsOpened) {
                qs();
            }
        });

        document.getElementsByClassName('header__row_1__search_form')[0].addEventListener('click', function (event) {
            // 避免搜索框为空时，点击搜索结果（最近访问），qs 关闭
            event.stopPropagation();
            if (qsOpened && document.activeElement !== searchInput) {
                searchInput.focus();
            }
        });
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
        var matchers = parseS(s);
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
        matchers.forEach(function (matcher) {
            pages = matcher.search(pages);
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
        return s.split(/\s+/).map(function (part) {
            if (part.startsWith(categoryPrefix)) {
                const category = part.substr(categoryPrefix.length);
                if (category.length > 0) {
                    return new CategoryMatcher(part, category);
                }
            }
            if (part.startsWith(tagPrefix)) {
                const tag = part.substr(tagPrefix.length);
                if (tag.length > 0) {
                    return new TagMatcher(part, tag);
                }
            }
            if (part.length > 1 && part.charAt(0) === '-') {
                return new ExcludeMatcher(part, part.substr(1));
            }
            return new ExactMatcher(part, part);
        });
    }

    class Matcher {
        constructor(expression, searchStr) {
            this.expression = expression;
            this.searchStr = searchStr;
        }
    }

    class CategoryMatcher extends Matcher {
        search(pages) {
            const _this = this;
            return pages.filter(function (page) {
                return page.category === _this.searchStr;
            });
        }
    }

    class TagMatcher extends Matcher {
        search(pages) {
            const _this = this;
            return pages.filter(function (page) {
                return page.tags.includes(_this.searchStr);
            })
        }
    }

    // 搜索 name, title, path
    class ExactMatcher extends Matcher {
        search(pages) {
            const _this = this;
            return pages.filter(function (page) {
                return getPageHit(page, _this.searchStr).hitCount > 0;
            });
        }
    }

    class ExcludeMatcher extends Matcher {
        search(pages) {
            const _this = this;
            return pages.filter(function (page) {
                return getPageHit(page, _this.searchStr).hitCount === 0;
            });
        }
    }

    function getPageHit(page, searchStr) {
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
            var showAllIcon = qsrList.getElementsByClassName('qsr__list__show_all__icon')[0];
            showAllIcon.addEventListener('click', showMoreResult);

            var showAllBtn = qsrList.getElementsByClassName('qsr__list__show_all__btn')[0];
            showAllBtn.addEventListener('click', showMoreResult);
            showAllBtn.classList.add('action_toggle');
        }
    }

    function renderQsrHtml(html) {
        qsrList.innerHTML = html;
        qsrClose.classList.toggle('show', true);
    }

    function showRecentlyVisit() {
        if (recentlyVisitPages == null) {
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
            return 'Search: ' + s;
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
        var html = '';
        var i = 0;
        for (; i < len; i++) {
            var page = pages[i];
            html += '<li>';
            html += '<div class="qsr__list__link_line">';
            html += createResultLink(page.path, page.searching.titlePreview, page.searching.pathPreview);
            html += '</div>';
            html += '</li>';
        }

        if (i < pages.length) {
            html += '<li class="qsr__list__show_all">';
            html += '<div class="qsr__list__link_line">';
            html += '<span class="qsr__list__show_all__icon"></span>';
            html += '<span class="qsr__list__show_all__btn no_selection">' + (i + 1) + ' ... ' + pages.length + '</span>';
            html += '<div class="qsr__list__link_line">';
            html += '</li>';
        }
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

    function qsrSelectedIndexInBound() {
        return qsrSelectedIndex >= 0 && qsrSelectedIndex < qsrList.children.length;
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

    function resetSelect() {
        unSelect();
        qsrSelectedIndex = -1;
    }

    function clearResult() {
        qsrList.innerHTML = '';
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

    function setUpCategoryAndTags() {
        var categoryField = 'category';
        var tagField = 'tags';
        var selectedClassName = 'cat_selected';
        categoryAndTags.innerHTML = buildHtml(allPages, categoryField, '分类') + buildHtml(allPages, tagField, '标签');
        var categoryList = document.getElementsByClassName('search_box__' + categoryField + '_list')[0];
        var tagList = document.getElementsByClassName('search_box__' + tagField + '_list')[0];
        const categoryTitle = document.getElementsByClassName('search_box__' + categoryField + '_list_title')[0];
        const tagTitle = document.getElementsByClassName('search_box__' + tagField + '_list_title')[0];

        Array.prototype.forEach.call(categoryList.getElementsByTagName('li'), function (li) {
            li.addEventListener('click', toggleCategorySelected);
        });

        Array.prototype.forEach.call(tagList.getElementsByTagName('li'), function (li) {
            li.addEventListener('click', toggleTagSelected);
        });

        categoryTitle.addEventListener('click', function () {
            cleanSelected(categoryList, CategoryMatcher);
            focusAndQs();
        });

        tagTitle.addEventListener('click', function () {
            cleanSelected(tagList, TagMatcher);
            focusAndQs();
        });

        bindCategoryAndTagsToggle();

        function buildHtml(pages, field, title) {
            var counters = [];
            pages.filter(function (page) {
                var items = page[field];
                return items instanceof Array ? items.length > 0 : items;
            }).map(function (page) {
                var items = page[field];
                return items instanceof Array ? items : [items];
            }).reduce(function (itemToCounter, items) {
                items.forEach(function (item) {
                    if (!itemToCounter[item]) {
                        var counter = {count: 1};
                        counter[field] = item;
                        itemToCounter[item] = counter;
                        counters.push(counter);
                    } else {
                        itemToCounter[item].count++;
                    }
                });
                return itemToCounter;
            }, {});

            counters.sort(function (counter1, counter2) {
                var v = counter2.count - counter1.count;
                if (v !== 0) {
                    return v;
                }
                return compareStringIgnoreCase(counter1[field], counter2[field]);
            });

            var html = '<div class="' + field + '_box">';
            html += '<span class="search_box__' + field + '_list_title">' + title + '</span>';
            html += '<ul class="search_box__' + field + '_list">';
            counters.forEach(function (item) {
                html += '<li class="action_toggle"><span class="' + (field === 'tags' ? 'tag' : 'category') + '">' + item[field] + '</span>';
                html += '<span class="count"> (' + item.count + ') </span>';
                html += '</li>';
            });
            html += '</ul></div>';
            return html;
        }

        function sContains(s, exp) {
            const matchers = parseS(s);
            for (let i = 0; i < matchers.length; i++) {
                if (matchers[i].expression === exp) {
                    return true;
                }
            }
            return false;
        }

        function sRemove(s, exp) {
            return sFilter(s, function (matcher) {
                return matcher.expression !== exp;
            });
        }

        function sRemoveMatcherType(s, matcherFn) {
            return sFilter(s, function (matcher) {
                return matcher.constructor.name !== matcherFn.name;
            });
        }

        function sFilter(s, test) {
            const stringBuilder = [];
            const matchers = parseS(s);
            for (let i = 0; i < matchers.length; i++) {
                if (test(matchers[i])) {
                    stringBuilder.push(matchers[i].expression);
                }
            }
            return stringBuilder.join(' ');
        }

        function getTag(li) {
            return li.getElementsByClassName('tag')[0].innerText;
        }

        function getCategory(li) {
            return li.getElementsByClassName('category')[0].innerText;
        }

        function toArray(elements) {
            // elements 不是正常的数组，如果在遍历过程中对 element 进行修改导致不符合之前的查询条件，
            // 该 element 会自动从 elements 中移除，导致遍历漏掉之后的元素。
            // 将 elements 转为正常的数组可避免这种情况
            return Array.prototype.map.call(elements, function (e) {
                return e;
            });
        }

        function toggleCategorySelected(event) {
            var li = event.currentTarget;
            var category = getCategory(li);
            var exp = categoryPrefix + category;
            var s = searchInput.value;
            var selected = li.classList.toggle(selectedClassName);

            if (!selected) {
                searchInput.value = sRemove(s, exp);
                focusAndQs();
                return;
            }

            if (!multiSelectEnabled(event)) {
                cleanTagSelected();
            }
            cleanCategorySelected();
            li.classList.add(selectedClassName);
            searchInput.value = exp + ' ' + searchInput.value;
            focusAndQs();
        }

        function toggleTagSelected(event) {
            var li = event.currentTarget;
            var tag = getTag(li);
            var exp = tagPrefix + tag;
            var s = searchInput.value;
            var selected = li.classList.toggle(selectedClassName);

            if (!selected) {
                searchInput.value = sRemove(s, exp);
                focusAndQs();
                return;
            }

            if (multiSelectEnabled(event)) {
                if (!sContains(s, exp)) {
                    searchInput.value = exp + ' ' + s;
                }
                focusAndQs();
                return;
            }

            cleanTagSelected();
            cleanCategorySelected();
            li.classList.add(selectedClassName);
            searchInput.value = exp + ' ' + searchInput.value;
            focusAndQs();
        }

        function multiSelectEnabled(event) {
            return multipleSelectionEnabled || event.metaKey || event.ctrlKey;
        }

        function focusAndQs() {
            searchInput.focus();
            qs();
        }

        function cleanTagSelected() {
            cleanSelected(tagList, TagMatcher);
        }

        function cleanCategorySelected() {
            cleanSelected(categoryList, CategoryMatcher);
        }

        function cleanSelected(parent, matcherType) {
            removeSelectedClass(parent);
            searchInput.value = sRemoveMatcherType(searchInput.value, matcherType);
        }

        function removeSelectedClass(parent) {
            toArray(parent.getElementsByClassName(selectedClassName)).forEach(function (el) {
                el.classList.remove(selectedClassName);
            });
        }

        function bindCategoryAndTagsToggle() {
            categoryAndTagsToggle.addEventListener('click', function () {
                var show = categoryAndTags.classList.toggle('show');
                categoryAndTagsToggle.classList.toggle('unfolded', show);
                if (show) {
                    syncS();
                }
                if (qsOpened) {
                    searchInput.focus();
                }
            });
        }

        function syncS() {
            var cSelected = {};
            var tSelected = {};
            parseS(searchInput.value).forEach(function (matcher) {
                if (matcher instanceof CategoryMatcher) {
                    cSelected[matcher.searchStr] = true;
                }
                if (matcher instanceof TagMatcher) {
                    tSelected[matcher.searchStr] = true;
                }
            });
            removeSelectedClass(categoryList);
            Array.prototype.forEach.call(categoryList.getElementsByClassName('category'), function (el) {
                if (cSelected[el.innerText]) {
                    el.parentNode.classList.add(selectedClassName);
                }
            });
            removeSelectedClass(tagList);
            Array.prototype.forEach.call(tagList.getElementsByClassName('tag'), function (el) {
                if (tSelected[el.innerText]) {
                    el.parentNode.classList.add(selectedClassName);
                }
            });
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
