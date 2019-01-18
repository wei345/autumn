<#if breadcrumb?? && breadcrumb?size &gt; 1>
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