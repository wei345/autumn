"use strict";
(function () {

    window.addEventListener('load', function () {
        checkLogout();
        updateVisitList();
    });

    var lsRecentVisitKey = 'autumn.recently_visit';
    var logoutCookieName = 'logout';

    function checkLogout() {
        if (autumn.getCookie(logoutCookieName)) {
            localStorage.removeItem(lsRecentVisitKey);
            autumn.deleteCookie(logoutCookieName);
        }
    }

    function updateVisitList() {
        // 确定 currentPath
        var currentPath = autumn.pathname();
        if (currentPath === '/' || currentPath === '/login') {
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
        var maxLength = 6;
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

    // 创建 tree 完成后调用此方法
    autumn.setupQuickSearch = function (root) {
        var searchInput = document.getElementsByClassName('header__row_1__search_input')[0];
        var cat = document.getElementsByClassName('search_box__category_and_tags')[0];
        var catToggle = document.getElementsByClassName('header__row_1__search_form__category_and_tags_toggle')[0];
        var qsr = document.getElementsByClassName('qsr')[0];
        var pathToPage; // path -> page
        var allPages = getAllPages();
        var categoryPrefix = 'category:';
        var tagPrefix = 'tag:';
        var foldedString = '+';
        var unfoldedString = '−';
        var currentQsrActivation = -1;
        var catToggleEnabled = false;

        renderCat();
        bindCatToggle();

        searchInput.addEventListener('focus', function () {
            openCat();
            qs(true);
        });

        searchInput.addEventListener('blur', function () {
            qs(true);
        });

        searchInput.addEventListener('keyup', function (evt) {
            qs();
        });

        searchInput.addEventListener('keydown', function (event) {
            var qsrList = document.getElementsByClassName('qsr__list')[0];
            if (!qsrList || qsrList.children.length === 0) {
                return;
            }

            if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
                var old = currentQsrActivation;
                if (event.key === 'ArrowDown') {
                    currentQsrActivation++;
                    if (currentQsrActivation >= qsrList.children.length) {
                        currentQsrActivation = 0;
                    }
                }
                if (event.key === 'ArrowUp') {
                    currentQsrActivation--;
                    if (currentQsrActivation < 0) {
                        currentQsrActivation = qsrList.children.length - 1;
                    }
                }
                if (old !== currentQsrActivation) {
                    if (old >= 0 && old < qsrList.children.length) {
                        qsrList.children[old].classList.remove('qsr_activation');
                    }
                    qsrList.children[currentQsrActivation].classList.add('qsr_activation');
                }
                event.preventDefault();
            } else if (event.key === 'Enter') {
                if (currentQsrActivation >= 0 && currentQsrActivation < qsrList.children.length) {
                    var href = qsrList.children[currentQsrActivation].getElementsByTagName('a')[0].href;
                    if (href) {
                        location.href = href;
                        event.preventDefault();
                    }
                }
            } else {
                if (currentQsrActivation >= 0 && currentQsrActivation < qsrList.children.length) {
                    qsrList.children[currentQsrActivation].classList.remove('qsr_activation');
                    currentQsrActivation = -1;
                }
            }

        });

        function getAllPages() {
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
            autumn.pages = pages; // for test
            return pages;
        }


        function bindCatToggle() {
            catToggle.addEventListener('click', function (event) {
                var show = cat.classList.toggle('show');
                catToggle.innerHTML = show ? unfoldedString : foldedString;
                if (show || searchInput.value.length > 0) {
                    searchInput.focus();
                }
            });
            catToggle.innerHTML = foldedString;
            catToggleEnabled = true;
        }

        function openCat() {
            if (!catToggleEnabled && !cat.classList.contains('show')) {
                cat.classList.toggle('show', true);
                catToggle.innerHTML = unfoldedString;
            }
        }

        function closeCat() {
            if (!catToggleEnabled && cat.classList.contains('show')) {
                cat.classList.toggle('show', false);
                catToggle.innerHTML = foldedString;
            }
        }

        function renderCat() {
            var html = mr(autumn.pages, 'category', '分类');
            html += mr(autumn.pages, 'tags', '标签');
            cat.innerHTML = html;

            bindClick('category', function (event) {
                handleClick(event, 'category');
            });

            bindClick('tags', function (event) {
                handleClick(event, 'tag');
            });

            var activateCategoryLi;

            function handleClick(event, categoryOrTag) {
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
                        if (activateCategoryLi) {
                            activateCategoryLi.classList.remove('cat_activation');
                        }
                        activateCategoryLi = li;
                    }
                }
                li.classList.toggle('cat_activation', add);
                searchInput.value = newValue;
                searchInput.focus();
                qs();
            }
        }


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

        function bindClick(f, onclick) {
            var els = document.getElementsByClassName('search_box__' + f + '_list')[0].getElementsByTagName('li');
            for (var i = 0; i < els.length; i++) {
                els[i].addEventListener('click', onclick);
            }
        }

        var lastS;
        var pendingQs = 0;
        var qsTimeoutId;

        function qs(immediately) {
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
            // var start = new Date().getTime();
            quickSearch();
            // console.log('quickSearch ' + (new Date().getTime() - start) + ' ms'); // 0 - 3 ms
        }

        function quickSearch() {
            var s = searchInput.value;

            if (s === '' && document.activeElement !== searchInput) {
                closeCat();
                clearResult();
                lastS = null;
                return;
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
                if (token.startsWith(categoryPrefix)) {
                    var category = token.substr(categoryPrefix.length);
                    if (category.length > 0) {
                        pages = searchCategory(pages, category);
                        return;
                    }
                }
                if (token.startsWith(tagPrefix)) {
                    var tag = token.substr(tagPrefix.length);
                    if (tag.length > 0) {
                        pages = searchTag(pages, tag);
                        return;
                    }
                }
                // 搜索 name, title, path
                pages = searchPages(pages, token);
            });
            sort(pages);
            qsr.innerHTML = buildResultHtml(pages);
            currentQsrActivation = -1;
        }

        function parseS(s) {
            s = s.trim();
            if (s.length === 0) {
                return [];
            }
            return s.split(/\s+/);
        }

        function searchCategory(pages, category) {
            return pages.filter(function (value) {
                return value.category === category;
            })
        }

        function searchTag(pages, tag) {
            return pages.filter(function (value) {
                return value.tags.includes(tag);
            })
        }

        function searchPages(pages, searchStr) {
            return pages.filter(function (page) {
                return searchPage(page, searchStr).hitCount > 0;
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
            if (empty(sourceStr) || empty(searchStr)) {
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

        function clearResult() {
            qsr.innerHTML = '';
        }

        function empty(str) {
            return str == null || str.length === 0;
        }

        function equalsIgnoreCase(str1, str2) {
            if (str1 == null && str2 == null) {
                return true;
            }
            if (str1 == null || str2 == null) {
                return false;
            }
            return str1.toLowerCase() === str2.toLowerCase();
        }

        // 用于 Array.sort
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

        function showRecentlyVisit() {
            var currentPath = autumn.pathname();
            var paths = getVisitList().filter(function (path) {
                return path !== currentPath;
            });

            var html = '<ul class="qsr__list">';
            for (var i = 0; i < paths.length; i++) {
                var path = paths[i];
                html += '<li><a href="' + autumn.ctx + path + '">';
                html += path;
                html += '</a></li>';
            }
            html += '</ul>';
            qsr.innerHTML = html;
        }

        function buildResultHtml(pages) {
            var html = '<ul class="qsr__list">';
            pages.forEach(function (page) {
                html += '<li><a href="' + autumn.ctx + page.path + '">';
                html += page.path;
                html += '</a></li>';
            });
            html += '</ul>';
            return html;
        }
    }

})();