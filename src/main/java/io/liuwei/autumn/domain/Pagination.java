package io.liuwei.autumn.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 前页，显示当前页附近 10 个页码，后页
 *
 * @author liuwei
 * Created by liuwei on 2019/1/10.
 */
@SuppressWarnings("WeakerAccess")
public class Pagination {

    private static int DEFAULT_PAGE_LIST_SIZE = 10;

    private int offset;

    private int page;

    private int pageSize;

    private int totalElements;

    private int totalPage;

    private List<PageNumber> pageList;

    private UrlFactory urlFactory;

    private PageNumber previous;

    private PageNumber next;

    public Pagination(int offset, int pageSize, int totalElements, UrlFactory urlFactory) {
        this(offset, pageSize, totalElements, DEFAULT_PAGE_LIST_SIZE, urlFactory);
    }

    public Pagination(int offset, int pageSize, int totalElements, int pageListSize, UrlFactory urlFactory) {
        this.offset = offset;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.urlFactory = urlFactory;
        this.page = calcCurrentPage();
        this.totalPage = calcTotalPage();
        this.pageList = calcPageList(pageListSize);
        if (hasPrevious()) {
            this.previous = newPageNumber(Math.min(page - 1, totalPage));
        }
        if (hasNext()) {
            this.next = newPageNumber(page + 1);
        }
    }

    private List<PageNumber> calcPageList(int count) {
        int from = Math.max(1, Math.min(page - count / 2, totalPage - count + 1));
        int to = Math.min(from + count - 1, totalPage);
        List<PageNumber> list = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            list.add(newPageNumber(i));
        }
        return list;
    }

    private PageNumber newPageNumber(int pageNumber) {
        int offset = pageSize * (pageNumber - 1);
        return new PageNumber(pageNumber, offset, urlFactory.getUrl(pageNumber, offset));
    }

    private int calcTotalPage() {
        int count = totalElements / pageSize;
        if (totalElements % pageSize > 0) {
            count++;
        }
        return count;
    }

    private int calcCurrentPage() {
        int pageNumber = offset / pageSize + 1;
        if (offset % pageSize > 0) {
            pageNumber++;
        }
        return pageNumber;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPage;
    }

    public PageNumber getPrevious() {
        return previous;
    }

    public boolean hasMore() {
        if (pageList.size() == 0) {
            return false;
        }
        if (pageList.size() > 1) {
            return true;
        }
        return pageList.get(0).page != page;
    }

    public PageNumber getNext() {
        return next;
    }

    public List<PageNumber> getPageList() {
        return pageList;
    }

    public int getPage() {
        return page;
    }

    public static class PageNumber {
        private int page;
        private int offset;
        private String url;

        PageNumber(int page, int offset, String url) {
            this.page = page;
            this.offset = offset;
            this.url = url;
        }

        public int getPage() {
            return page;
        }

        public int getOffset() {
            return offset;
        }

        public String getUrl() {
            return url;
        }
    }

    public interface UrlFactory {
        String getUrl(int pageNumber, int offset);
    }
}