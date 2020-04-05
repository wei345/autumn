<#include "head.ftl">
<div class="header">
    <div class="header__row_1 row">
        <div class="header__row_1__heading column">
            <h1>
                <a href="${ctx!}/">${title!}</a>
            </h1>
        </div>
        <div class="header__row_1__search_box column">
            <form class="header__row_1__search_form" method="GET" action="${ctx!}/search">
                <span class="search_icon btn"></span>
                <span class="search_box__ct_toggle btn"></span>
                <span class="btn_clear_search btn"></span>
                <input class="header__row_1__search_input" type="search" name="s" value="${s!}" autocomplete="off"
                       placeholder="Search"/>
                <div class="search_box__ct no_selection"></div>
                <div class="qsr">
                    <span class="qsr__close btn"></span>
                    <div class="qsr__stats"></div>
                    <ul class="qsr__list"></ul>
                </div>
            </form>
        </div>
        <div class="header__row_1__right column">
            <ul class="header__row_1__tools no_selection">
                <#-- sitemap 移至单独的页面
                <li><span class="sitemap_toggle action_toggle">Sitemap</span></li>-->
                <li><a href="${ctx!}/sitemap#${ctx!}${path!}">Sitemap</a></li>
                <li><a href="${ctx!}/help">Help</a></li>
            <#if logged?? && logged == true>
                <li><a class="header__row_1__right__logout" href="${ctx!}/logout">Logout</a></li>
            <#else>
                <li><a class="header__row_1__right__logout" href="${ctx!}/login">Login</a></li>
            </#if>
            </ul>
        </div>
    </div>
</div>
<div class="header__toolbar">
    <#--<span class="sidebar_toggle no_selection action_toggle">Sidebar</span>-->
</div>
<#-- sitemap 移至单独的页面
<div class="sitemap">
    <div class="tree_box">
    </div>
</div>-->
<div class="main row">
    <div class="sidebar column">
    </div>
    <div class="content column">
