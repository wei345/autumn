<#include "inc/content_open.ftl">
<div class="page">
    <#if breadcrumb?has_content>
    <ul class="breadcrumb"><#t>
        <#list breadcrumb as link>
            <li><#t>
                <span class="separator">/</span><#t>
            <#if link.href??>
                <a href="${ctx!}${link.href!}">${link.text!}</a><#t>
            <#else>
                <span>${link.text!}</span><#t>
            </#if>
            </li><#t>
        </#list>
    </ul><#lt>
    </#if>
${pageHtml!}
</div>
<#include "inc/content_close.ftl">