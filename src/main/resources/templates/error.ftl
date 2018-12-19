<#assign title = (status!) + " " + (error!)>
<#include "inc/content_open.ftl">
<div class="error_page center_position">
    <h1>${title!}</h1>
    <#if (message!) != (error!)><p>${message!}</p></#if>
</div>
<#include "inc/content_close.ftl">