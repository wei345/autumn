<#include "inc/head.ftl">
<#include "inc/header.ftl">
    <div class="main">
        <div class="sidebar">
            <div class="tree_box"></div>
        </div>
        <div class="sr">
            <#if (sr)??>
                <div class="sr_stats">${sr.pages?size!} results (${sr.timeCost!} ms)</div>
                <div class="pages">
                    <#list sr.pages as sp>
                        <div class="sr_page">
                            <div>
                                <a href="${sp.page.path!}<#if sp.highlightString??>?</#if>${sp.highlightString!}">
                                    <span class="sr_page_title">${sp.titlePreview!"No Title"}</span><br/>
                                    <span class="sr_page_path">${sp.pathPreview!}</span>
                                </a>
                            </div>
                            <div class="sr_page_body">${sp.bodyPreview!}</div>
                            <div class="sr_page_info">
                                <span class="sr_page_hit">${sp.hitCount!} Hits, </span>
                                <span class="sr_page_date">${sp.page.modified?string["yyyy-MM-dd"]!}</span>
                            </div>
                        </div>
                    </#list>
                </div>
            <#else>
                <p>${message!}</p>
            </#if>
        </div>
    </div>
<#include "inc/foot.ftl">

