<#if pagination?? && pagination.hasMore() == true>
    <div class="pagination">
        <#if pagination.hasPrevious() == true>
        <a class="previous" href="${ctx!}${pagination.previous.url}">
            <span class="pn">Previous</span>
        </a>
        </#if>

        <#if pagination.pageList?has_content>
            <#list pagination.pageList as p>
                <#if p.page == pagination.page>
                    <span class="pn">${p.page}</span>
                <#else>
                    <a href="${ctx!}${p.url}">
                        <span class="pn">${p.page}</span>
                    </a>
                </#if>
            </#list>
        </#if>

        <#if pagination.hasNext() == true>
        <a class="next" href="${ctx!}${pagination.next.url}">
            <span class="pn">Next</span>
        </a>
        </#if>
    </div>
</#if>
