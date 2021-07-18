package io.liuwei.autumn.model;

import lombok.Getter;

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

    private static final int DEFAULT_PAGE_LIST_SIZE = 10;

    private final int offset;

    @Getter
    private final int pageNumber;

    private final int pageSize;

    private final int totalElements;

    private final int totalPage;

    @Getter
    private final List<Page> pageList;

    private final UrlFactory urlFactory;

    @Getter
    private Page previous;

    @Getter
    private Page next;

    public Pagination(int offset, int pageSize, int totalElements, UrlFactory urlFactory) {
        this(offset, pageSize, totalElements, DEFAULT_PAGE_LIST_SIZE, urlFactory);
    }

    public Pagination(int offset, int pageSize, int totalElements, int pageListSize, UrlFactory urlFactory) {
        this.offset = offset;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.urlFactory = urlFactory;
        this.pageNumber = calcCurrentPageNumber();
        this.totalPage = calcTotalPage();
        this.pageList = calcPageList(pageListSize);
        if (hasPrevious()) {
            this.previous = newPage(Math.min(pageNumber - 1, totalPage));
        }
        if (hasNext()) {
            this.next = newPage(pageNumber + 1);
        }
    }

    private List<Page> calcPageList(int count) {
        int from = Math.max(1, Math.min(pageNumber - count / 2, totalPage - count + 1));
        int to = Math.min(from + count - 1, totalPage);
        List<Page> list = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            list.add(newPage(i));
        }
        return list;
    }

    private Page newPage(int pageNumber) {
        int offset = pageSize * (pageNumber - 1);
        return new Page(pageNumber, offset, urlFactory.getUrl(pageNumber, offset));
    }

    private int calcTotalPage() {
        int count = totalElements / pageSize;
        if (totalElements % pageSize > 0) {
            count++;
        }
        return count;
    }

    private int calcCurrentPageNumber() {
        int pageNumber = offset / pageSize + 1;
        if (offset % pageSize > 0) {
            pageNumber++;
        }
        return pageNumber;
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    public boolean hasNext() {
        return pageNumber < totalPage;
    }

    public boolean hasMore() {
        if (pageList.size() == 0) {
            return false;
        }
        if (pageList.size() > 1) {
            return true;
        }
        return pageList.get(0).pageNumber != pageNumber;
    }

    public interface UrlFactory {
        String getUrl(int pageNumber, int offset);
    }

    @Getter
    public static class Page {
        private final int pageNumber;
        private final int offset;
        private final String url;

        Page(int pageNumber, int offset, String url) {
            this.pageNumber = pageNumber;
            this.offset = offset;
            this.url = url;
        }
    }
}
