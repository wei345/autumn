<#include "inc/head.ftl">
<#include "inc/header.ftl">
    <div class="row">
        <div id="sidebar" class="column column-25">
            <div class="tree"></div>
        </div>
        <div class="sr column column-75">
            <div class="stats">${sr.pages?size!} results (${sr.timeCost!} ms)</div>
            <div class="pages">
            <#list sr.pages as sp>
                <div class="page">
                    <div>
                        <a href="${sp.page.path!}">
                            <span class="title">${sp.page.title!"No Title"}</span>
                            <br/>
                            <span class="path">${sp.page.path!}</span>
                        </a>
                    </div>
                    <div class="info">
                        <span class="hit">${sp.hitCount!} Hits, </span>
                        <span class="date"> Last modified: ${sp.page.modified?string["yyyy-MM-dd"]!}</span>
                    </div>
                    <div class="preview"></div>
                </div>
            </#list>
            </div>
        </div>
    </div>
<#include "inc/foot.ftl">

