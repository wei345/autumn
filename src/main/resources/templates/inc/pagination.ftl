<#if pagination??>
    <div class="pagination">
        <#if pagination.hasPrevious() == true>
        <a class="previous" href="${ctx!}${pagination.previous.url}">
            <span class="pn">Previous</span>
        </a>
        </#if>

        <#if pagination.pageNumbers?has_content>
            <#list pagination.pageNumbers as pn>
                <#if pn.pageNumber == pagination.currentPageNumber>
         <span class="pn">${pn.pageNumber}</span>
                <#else>
         <a href="${ctx!}${pn.url}">
             <span class="pn">${pn.pageNumber}</span>
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
