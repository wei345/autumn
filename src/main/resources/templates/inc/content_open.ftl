<#include "head.ftl">
<div class="header">
    <div class="header__row_1 row">
        <div class="header__row_1__heading column">
            <h1>
                <a href="${ctx!}/">Autumn</a>
            </h1>
        </div>
        <div class="header__row_1__search_box column">
            <form class="header__row_1__search_form" method="GET" action="${ctx!}/search">
                <svg class="search_icon" focusable="false" height="24px" viewBox="0 0 24 24" width="24px"
                     xmlns="http://www.w3.org/2000/svg">
                    <path d="M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"></path>
                    <path d="M0 0h24v24H0z" fill="none"></path>
                </svg>
                <span class="search_box__category_and_tags_toggle no_selection"></span>
                <input class="header__row_1__search_input" type="search" name="s" value="${s!}" autocomplete="off"
                       placeholder="Search"/>
                <div class="search_box__category_and_tags no_selection"></div>
                <div class="qsr">
                    <span class="qsr__close no_selection">X</span>
                    <ul class="qsr__list"></ul>
                </div>
            </form>
        </div>
        <div class="header__row_1__right column">
            <ul class="header__row_1__tools no_selection">
            <#--<li><span class="sitemap_toggle action_toggle">Sitemap</span></li>-->
                <li><a href="${ctx!}/help">Help</a></li>
                <li><a href="https://github.com/wei345/autumn">GitHub</a></li>
            <#if logged == true>
                <li><a class="header__row_1__right__logout" href="${ctx!}/logout">Logout</a></li>
            </#if>
            </ul>
        </div>
    </div>
</div>
<div class="header__toolbar">
    <span class="sidebar_toggle no_selection action_toggle">Sidebar</span>
</div>
<#--<div class="sitemap center_position"></div>-->
<div class="main row">
    <div class="sidebar column">
        <div class="tree_box">
        </div>
    </div>
    <div class="content column">