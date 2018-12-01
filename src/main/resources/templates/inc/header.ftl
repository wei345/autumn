<div class="header">
    <div class="header__1">
        <div class="header__1__headings">
            <h1>
                <a href="/">Autumn</a>
            </h1>
        </div>
        <div class="header__1__search_box">
            <form method="GET" action="/search">
                <input class="header__1__search_input" type="text" name="s" value="${s!}" autocomplete="off"/>
            </form>
        </div>
        <div class="header__1__right">
            <#if logged == true>
                <a class="header__1__right__logout" href="/logout">Logout</a>
            </#if>
        </div>
    </div>
    <div class="header__toolbar">
        <span class="header__toolbar__sidebar_toggle no_selection">Sidebar</span>
    </div>
</div>