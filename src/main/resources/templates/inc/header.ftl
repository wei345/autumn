<div class="header">
    <div class="row">
        <div class="headings">
            <h1>
                <a href="/">Autumn</a>
            </h1>
        </div>
        <div class="search-box">
            <form method="GET" action="/search">
                <input id="search-input" type="text" name="s" value="${s!}" autocomplete="off"/>
            </form>
        </div>
        <div class="right">
            <#if logged == true>
                <a class="logout" href="/logout">Logout</a>
            </#if>
        </div>
    </div>
    <div class="toolbar">
        <span class="sidebar-toggle no-selection">Sidebar</span>
        <span class="toc-toggle no-selection">Table of Contents</span>
    </div>
</div>