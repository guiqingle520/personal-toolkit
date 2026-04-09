package com.personal.toolkit.todo.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 统一封装分页查询结果，向前端暴露列表内容与分页元数据。
 *
 * @param <T> 列表元素类型
 */
public class PageResponse<T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;
    private final boolean first;
    private final boolean last;

    private PageResponse(List<T> content,
                         long totalElements,
                         int totalPages,
                         int page,
                         int size,
                         boolean first,
                         boolean last) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
        this.first = first;
        this.last = last;
    }

    /**
     * 将 Spring Data Page 转换为前端友好的分页响应对象。
     *
     * @param page Spring Data 分页对象
     * @param <T> 列表元素类型
     * @return 标准分页响应对象
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast()
        );
    }

    /**
     * 返回当前页的数据列表。
     *
     * @return 数据列表
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 返回符合条件的数据总数。
     *
     * @return 总记录数
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * 返回分页总页数。
     *
     * @return 总页数
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * 返回当前页码，基于 0 开始计数。
     *
     * @return 当前页码
     */
    public int getPage() {
        return page;
    }

    /**
     * 返回每页条数。
     *
     * @return 每页大小
     */
    public int getSize() {
        return size;
    }

    /**
     * 返回当前页是否为第一页。
     *
     * @return 是否第一页
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * 返回当前页是否为最后一页。
     *
     * @return 是否最后一页
     */
    public boolean isLast() {
        return last;
    }
}
