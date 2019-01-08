"use strict";
function setupQuickSearch(treeRoot) {
    var searchForm = au.el('.header__row_1__search_form');
    var searchInput = au.el('.header__row_1__search_input');
    var ctToggle = au.el('.search_box__ct_toggle');
    var btnClearSearch = au.el('.btn_clear_search');
    var qsrClose = au.el('.qsr__close');
    var qsrList = au.el('.qsr__list');
    var qsrAllPages;
    var qsrResultName;
    var qsrDefaultLines = 6;
    var qsrSelectedIndex = -1;
    var categoryPrefix = 'c:';
    var tagPrefix = 't:';
    var qsOpened = false;
    var allPages;
    var pathToPage;
    var lastS;
    var pendingQs = 0;
    var qsTimeoutId;
    var onQsOpenListeners = [];
    var onQsCloseListeners = [];
    var onSearchInputChangeListeners = [];
    var btnClearSearchVisible = false;

    getAllPages(treeRoot);
    setupCt();
    bindSearchInputEvent();
    bindQuickSearchCloseEvent();
    updateBtnClearSearchVisible(); // 搜索页有初始值

    function bindSearchInputEvent() {
        searchInput.addEventListener('focus', function () {
            if (!isSearchInputFocusing()) {
                setSearchInputFocusing(true);
                if (isMobi) {
                    au.scrollToTop(searchInput);
                }
            }

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
                if (qsrSelectedIndexInBound()) {
                    select();
                    const selected = qsrList.children[qsrSelectedIndex];
                    if (!au.isElementInViewport(selected)) {
                        au.scrollToCenter(selected);
                    }
                }
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

                var href = au.el('a', selected).href;
                if (href) {
                    location.href = href;
                }
                return;
            }

            if (event.key === 'Escape') {
                event.preventDefault(); // 阻止浏览器默认清空 input
                if (qsrSelectedIndexInBound()) {
                    resetSelect();
                    return;
                }

                /* 不清空
                if (searchInput.value.trim().length > 0) {
                    setSearchInputValue(''); // 之后 keyup 会触发 qs()
                    return;
                }*/

                if (qsOpened) {
                    closeQs();
                }
                searchInput.blur();
            }
        });

        /* 按 ESC 或方向键不搜索
        searchInput.addEventListener('keyup', function (event) {
            qs();
        });*/

        // 键盘或鼠标操作内容变化触发搜索
        searchInput.addEventListener('input', function () {
            updateBtnClearSearchVisible();
            qs();
        });

        btnClearSearch.addEventListener('click', function () {
            setSearchInputValue('');
            searchInput.focus();
            updateBtnClearSearchVisible();
            qs();
        });
    }

    function setSearchInputValue(value) {
        searchInput.value = value;
        onSearchInputChangeListeners.forEach(fn => fn());
    }

    function bindQuickSearchCloseEvent() {
        // 不用 blur 关闭 QuickSearch，因为会导致无法用鼠标点击搜索结果，在鼠标点到链接之前 QuickSearch 就关闭了

        qsrClose.addEventListener('click', function (event) {
            if (qsOpened) {
                //setSearchInputValue('');
                closeQs();
                setSearchInputFocusing(false);
            }
            event.stopPropagation();
        });

        document.addEventListener('click', function (event) {
            if (document.activeElement !== searchInput) {
                setSearchInputFocusing(false);
                /* not auto close */
                if (!event.isFromSearchForm && qsOpened) {
                    closeQs();
                }
            }
        });

        searchForm.addEventListener('click', function (event) {
            event.isFromSearchForm = true;
            /*if (qsOpened && document.activeElement !== searchInput && getSelectionText() === '') {
                searchInput.focus();
            }
            */
        });
    }

    function isSearchInputFocusing() {
        return searchInput.classList.contains('header__row_1__search_input_focus');
    }

    function setSearchInputFocusing(isFocusing) {
        searchInput.classList.toggle('header__row_1__search_input_focus', isFocusing);
    }

    function updateBtnClearSearchVisible() {
        if (searchInput.value === '') {
            if (btnClearSearchVisible) {
                btnClearSearch.classList.remove('show');
                btnClearSearchVisible = false;
            }
        } else {
            if (!btnClearSearchVisible) {
                btnClearSearch.classList.add('show');
                btnClearSearchVisible = true;
            }
        }
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

        if (s === '' && document.activeElement !== searchInput) {
            closeQs();
            return;
        }

        if (!qsOpened) {
            openQs();
        }

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

    function openQs() {
        qsOpened = true;
        container.classList.add('qs_opened');
        onQsOpenListeners.forEach(fn => fn());
    }

    function closeQs() {
        if (!qsOpened) {
            return;
        }
        clearResult();
        lastS = null;
        qsOpened = false;
        container.classList.remove('qs_opened');
        container.classList.remove('qsr_more');
        onQsCloseListeners.forEach(fn => fn());
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
                nameEq: au.stringEqualsIgnoreCase(page.name, search),
                titleEq: au.stringEqualsIgnoreCase(page.title, search),
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
        if (au.isEmpty(sourceStr) || au.isEmpty(searchStr)) {
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
            return au.compareStringIgnoreCase(o1.path, o2.path);
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
                html += au.escapeHtml(str.substring(start, hit.start));
                html += hlTagOpen;
                html += au.escapeHtml(str.substring(hit.start, hit.end));
                html += hlTagClose;
                start = hit.end;
            });

            if (start === 0) {
                return str;
            }

            if (start < str.length) {
                html += au.escapeHtml(str.substring(start, str.length));
            }

            return html;
        }
    }

    function showSearchResult(pages, resultName) {
        qsrAllPages = pages;
        qsrResultName = resultName ? resultName : 'results';
        var lines = container.classList.contains('qsr_more') ? pages.length : qsrDefaultLines;
        renderPages(pages, lines, qsrResultName);
        qsrSelectedIndex = -1;
    }

    function showMoreResult() {
        renderPages(qsrAllPages, qsrAllPages.length, qsrResultName);
        select();
        container.classList.add('qsr_more');
        scroll(0, 0);
    }

    function renderPages(pages, maxLines, resultName) {
        au.el('.qsr__stats').innerHTML = pages.length + " " + resultName;
        renderQsrHtml(buildResultHtml(pages, maxLines, resultName));

        if (maxLines < pages.length) {
            au.el('.qsr__list__show_all__icon', qsrList).addEventListener('click', showMoreResult);
            var btn = au.el('.qsr__list__show_all__btn', qsrList);
            btn.addEventListener('click', showMoreResult);
            btn.classList.add('action_toggle');
        }
    }

    function renderQsrHtml(html) {
        qsrList.innerHTML = html;
    }

    function showRecentlyVisit() {
        if (recentlyVisitPages == null) {
            var currentPath = au.pathname();
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
        showSearchResult(recentlyVisitPages, 'recently visited');
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
        var s = au.parseQueryString(queryString).s;
        if (s) {
            return 'Search: ' + s;
        }
        return path;
    }

    function buildResultHtml(pages, maxLength, resultName) {
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
            html += '<span class="qsr__list__show_all__btn">' + (pages.length - i) + ' more ' + resultName + '</span>';
            html += '</div>';
            html += '</li>';
        }
        return html;
    }

    function createResultLink(path, titlePreview, pathPreview) {
        if (titlePreview === '') {
            titlePreview = 'No Title';
        }
        var html = '<a href="' + ctx + path + '">';
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
    }

    function setupCt() {
        var categoryField = 'category';
        var tagField = 'tags';
        var selectedClassName = 'cat_selected';
        var ct = au.el('.search_box__ct');
        ct.innerHTML = buildHtml(allPages, categoryField, '分类') + buildHtml(allPages, tagField, '标签');
        var categoryList = au.el('.search_box__' + categoryField + '_list');
        var tagList = au.el('.search_box__' + tagField + '_list');
        const categoryTitle = au.el('.search_box__' + categoryField + '_list_title');
        const tagTitle = au.el('.search_box__' + tagField + '_list_title');
        var lastSyncS;
        var ctOpened = false;
        const unSelectedExpressionToOption = {};
        const selectedExpressionToOption = {};

        class Option {
            constructor(dom, value, expressionPrefix) {
                this.dom = dom;
                this.value = value;
                this.selected = false;
                this.expression = expressionPrefix + value;
            }

            select() {
                if (this.selected) {
                    return;
                }
                this.dom.classList.add(selectedClassName);
                this.selected = true;
            }

            unSelect() {
                if (!this.selected) {
                    return;
                }
                this.dom.classList.remove(selectedClassName);
                this.selected = false;
            }
        }

        class CategoryOption extends Option {
            constructor(dom, value) {
                super(dom, value, categoryPrefix);
            }
        }

        class TagOption extends Option {
            constructor(dom, value) {
                super(dom, value, tagPrefix);
            }
        }

        buildOptions();
        autoSyncFromInput();
        bindCtEvents();

        function bindCtEvents() {
            if (!hasCategoriesOrTags()) {
                return;
            }

            if (hasCategories()) {
                categoryTitle.addEventListener('click', function () {
                    cleanSelected(CategoryOption);
                });
                au.els('li', categoryList).forEach(li => li.addEventListener('click', toggleSelected));
            }

            if (hasTags()) {
                tagTitle.addEventListener('click', function () {
                    cleanSelected(TagOption);
                });
                au.els('li', tagList).forEach(li => li.addEventListener('click', toggleSelected));
            }

            /* not auto close
            document.addEventListener('click', function (event) {
                if (!event.isFromSearchForm && !qsOpened) {
                    closeCt();
                }
            });
            */

            ctToggle.addEventListener('click', function () {
                if (ctOpened) {
                    closeCt();
                } else {
                    openCt();
                }
                if (isSearchInputFocusing()) {
                    searchInput.focus();
                }
            });

            ctToggle.classList.add('show');
        }

        function hasCategoriesOrTags() {
            return hasCategories() || hasTags();
        }

        function hasCategories() {
            return !!categoryList;
        }

        function hasTags() {
            return !!tagList;
        }

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

            if (counters.length === 0) {
                return '';
            }

            counters.sort(function (counter1, counter2) {
                var v = counter2.count - counter1.count;
                if (v !== 0) {
                    return v;
                }
                return au.compareStringIgnoreCase(counter1[field], counter2[field]);
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

        function buildOptions() {
            if (hasCategories()) {
                au.els('li', categoryList).forEach(function (dom) {
                    var value = au.el('.category', dom).innerText;
                    var option = new CategoryOption(dom, value);
                    dom.ctOption = option;
                    unSelectedExpressionToOption[option.expression] = option;
                });
            }

            if (hasTags()) {
                au.els('li', tagList).forEach(function (dom) {
                    var value = au.el('.tag', dom).innerText;
                    var option = new TagOption(dom, value);
                    dom.ctOption = option;
                    unSelectedExpressionToOption[option.expression] = option;
                });
            }
        }

        function toggleSelected(event) {
            const option = event.currentTarget.ctOption;

            // 取消选中
            if (option.selected) {
                moveToUnSelected(option);
                syncToInput();
                focusAndQs();
                return;
            }

            // 选中
            const multiEnabled = multiSelectEnabled(event);
            au.each(selectedExpressionToOption, function (k, v) {
                if (multiEnabled) {
                    if (v instanceof TagOption) {
                        return;
                    }
                    if (v instanceof CategoryOption && option instanceof TagOption) {
                        return;
                    }
                }
                moveToUnSelected(v);
            });

            moveToSelected(option);
            syncToInput();
            focusAndQs();
        }

        function multiSelectEnabled(event) {
            return multipleSelectionEnabled || event.metaKey || event.ctrlKey;
        }

        function focusAndQs() {
            // 移动设备上点击标签或分类，搜索框不要自动获得焦点，
            // 因为获得焦点后会显示键盘浮层，页面显示空间变小，这可能不是用户期望的行为。
            if (!isMobi || isSearchInputFocusing()) {
                if (document.activeElement !== searchInput) {
                    searchInput.focus();
                }
            }
            qs();
        }

        function cleanSelected(optionFn) {
            let changed = false;
            au.each(selectedExpressionToOption, function (k, v) {
                if (v instanceof optionFn) {
                    moveToUnSelected(v);
                    changed = true;
                }
            });
            if (changed) {
                syncToInput();
                focusAndQs();
            }
        }

        function moveToSelected(option) {
            delete unSelectedExpressionToOption[option.expression];
            selectedExpressionToOption[option.expression] = option;
            option.select();
        }

        function moveToUnSelected(option) {
            delete selectedExpressionToOption[option.expression];
            unSelectedExpressionToOption[option.expression] = option;
            option.unSelect();
        }

        function openCt() {
            ctOpened = true;
            ct.classList.add('show');
            ctToggle.classList.add('unfolded');
            container.classList.add('show_ct');
            syncFromInput();
        }

        function closeCt() {
            if (!ctOpened) {
                return;
            }
            ctOpened = false;
            ct.classList.remove('show');
            ctToggle.classList.remove('unfolded');
            container.classList.remove('show_ct');
        }

        function syncToInput() {
            let s = searchInput.value;

            // 把没选中的和选中的全去掉
            s = sFilter(s, function (matcher) {
                return !unSelectedExpressionToOption[matcher.expression]
                    && !selectedExpressionToOption[matcher.expression];
            });

            // 把选中的全加上
            au.each(selectedExpressionToOption, function (k) {
                s = k + ' ' + s;
            });

            searchInput.value = s + ' ';
            lastSyncS = s;
            updateBtnClearSearchVisible();
        }

        function syncFromInput() {
            if (!ctOpened) {
                return;
            }
            const s = searchInput.value.trim();
            if (lastSyncS === s) {
                return;
            }
            lastSyncS = s;

            // 把选中的移到未选中
            au.each(selectedExpressionToOption, function (k, v) {
                unSelectedExpressionToOption[k] = v;
                delete selectedExpressionToOption[k];
            });

            // 把 input 里出现的移到选中，并执行 select
            parseS(s).forEach(function (matcher) {
                const exp = matcher.expression;
                if (unSelectedExpressionToOption[exp]) {
                    moveToSelected(unSelectedExpressionToOption[exp]);
                }
            });

            // 执行 unSelect
            au.each(unSelectedExpressionToOption, function (k, v) {
                v.unSelect();
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

        function autoSyncFromInput() {
            if (!hasCategoriesOrTags()) {
                return;
            }
            searchInput.addEventListener('keyup', syncFromInput);
            searchInput.addEventListener('input', syncFromInput);
            onSearchInputChangeListeners.push(syncFromInput);
            onQsCloseListeners.push(closeCt);
        }
    }
}

function checkLogout() {
    if (au.getCookie(logoutCookieName)) {
        localStorage.removeItem(lsRecentVisitKey);
        au.deleteCookie(logoutCookieName);
    }
}

function updateVisitList() {
    // 确定 currentPath
    var currentPath = au.pathname();
    if (ctx.length > 0) {
        currentPath = currentPath.substr(ctx.length);
    }
    if (currentPath === '/' || currentPath === '/login' || currentPath === '/help') {
        return;
    }
    if (au.el('.error_page')) {
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
