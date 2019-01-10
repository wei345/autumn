package xyz.liuw.autumn.domain;

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

    private static int DEFAULT_PAGE_NUMBER_COUNT = 10;

    private int currentOffset;

    private int pageSize;

    private int total;

    private int currentPageNumber;

    private int maxPageNumber;

    private List<PageNumberOffset> pageNumbers;

    private UrlFactory urlFactory;

    private PageNumberOffset previous;

    private PageNumberOffset next;

    public Pagination(int currentOffset, int pageSize, int total, UrlFactory urlFactory) {
        this(currentOffset, pageSize, total, DEFAULT_PAGE_NUMBER_COUNT, urlFactory);
    }

    public Pagination(int currentOffset, int pageSize, int total, int pageNumberCount, UrlFactory urlFactory) {
        this.currentOffset = currentOffset;
        this.pageSize = pageSize;
        this.total = total;
        this.urlFactory = urlFactory;

        this.currentPageNumber = calcCurrentPageNumber();
        this.maxPageNumber = calcMaxPageNumber();
        this.pageNumbers = calcPageNumbers(pageNumberCount);
        if (hasPrevious()) {
            this.previous = newPageNumberOffset(currentPageNumber - 1);
        }
        if (hasNext()) {
            this.next = newPageNumberOffset(currentPageNumber + 1);
        }
    }

    private List<PageNumberOffset> calcPageNumbers(int pageNumberCount) {
        int fromPn = Math.max(currentPageNumber - pageNumberCount / 2, 1);
        int toPn = Math.min(fromPn + pageNumberCount - 1, maxPageNumber);
        List<PageNumberOffset> pageNumbers = new ArrayList<>(toPn - fromPn + 1);
        for (int i = fromPn; i <= toPn; i++) {
            pageNumbers.add(newPageNumberOffset(i));
        }
        return pageNumbers;
    }

    private PageNumberOffset newPageNumberOffset(int pageNumber) {
        int offset = pageSize * (pageNumber - 1);
        return new PageNumberOffset(pageNumber, offset, urlFactory.getUrl(pageNumber, offset));
    }

    private int calcMaxPageNumber() {
        int max = total / pageSize;
        if (total % pageSize > 0) {
            max++;
        }
        return max;
    }

    private int calcCurrentPageNumber() {
        int pageNumber = currentOffset / pageSize + 1;
        if (currentOffset % pageSize > 0) {
            pageNumber++;
        }
        return pageNumber;
    }

    public boolean hasPrevious() {
        return currentPageNumber > 1;
    }

    public boolean hasNext() {
        return currentPageNumber < maxPageNumber;
    }

    public PageNumberOffset getPrevious() {
        return previous;
    }

    public PageNumberOffset getNext() {
        return next;
    }

    public List<PageNumberOffset> getPageNumbers() {
        return pageNumbers;
    }

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public static class PageNumberOffset {
        private int pageNumber;
        private int offset;
        private String url;

        PageNumberOffset(int pageNumber, int offset, String url) {
            this.pageNumber = pageNumber;
            this.offset = offset;
            this.url = url;
        }

        public int getPageNumber() {
            return pageNumber;
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
