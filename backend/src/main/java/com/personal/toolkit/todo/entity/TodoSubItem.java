package com.personal.toolkit.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 表示主任务下的一条单层子任务记录，用于 checklist 展示与进度统计。
 */
@Entity
@Table(name = "todo_sub_item")
public class TodoSubItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_sub_item_seq")
    @SequenceGenerator(name = "todo_sub_item_seq", sequenceName = "todo_sub_item_seq", allocationSize = 1)
    private Long id;

    @Column(name = "todo_id", nullable = false)
    private Long todoId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 返回子任务主键。
     *
     * @return 子任务主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置子任务主键。
     *
     * @param id 子任务主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回所属主任务主键。
     *
     * @return 主任务主键
     */
    public Long getTodoId() {
        return todoId;
    }

    /**
     * 设置所属主任务主键。
     *
     * @param todoId 主任务主键
     */
    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    /**
     * 返回子任务标题。
     *
     * @return 子任务标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置子任务标题。
     *
     * @param title 子任务标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回子任务状态。
     *
     * @return 子任务状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置子任务状态。
     *
     * @param status 子任务状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回子任务排序值。
     *
     * @return 子任务排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置子任务排序值。
     *
     * @param sortOrder 子任务排序值
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 返回子任务创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置子任务创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 返回子任务最后更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置子任务最后更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 在实体首次入库前写入默认状态、排序值和时间字段。
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
        this.createTime = now;
        this.updateTime = now;
    }

    /**
     * 在实体更新前刷新最后更新时间。
     */
    @PreUpdate
    public void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
