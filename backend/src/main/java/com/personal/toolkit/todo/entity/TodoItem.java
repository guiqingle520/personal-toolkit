package com.personal.toolkit.todo.entity;

import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

/**
 * 表示一条持久化的待办事项记录，映射数据库中的 todo_item 表。
 */
@Entity
@Table(name = "todo_item")
public class TodoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "todo_item_seq")
    @SequenceGenerator(name = "todo_item_seq", sequenceName = "todo_item_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(length = 100)
    private String category;

    @Column(length = 500)
    private String tags;

    @Column(name = "recurrence_type", nullable = false, length = 32)
    private String recurrenceType;

    @Column(name = "recurrence_interval", nullable = false)
    private Integer recurrenceInterval;

    @Column(name = "recurrence_end_time")
    private LocalDateTime recurrenceEndTime;

    @Column(name = "next_trigger_time")
    private LocalDateTime nextTriggerTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Transient
    private TodoSubItemSummaryResponse subItemSummary;

    /**
     * 返回待办事项主键。
     *
     * @return 主键标识
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置待办事项主键。
     *
     * @param id 主键标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回待办事项标题。
     *
     * @return 待办标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置待办事项标题。
     *
     * @param title 待办标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回待办事项状态。
     *
     * @return 待办状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置待办事项状态。
     *
     * @param status 待办状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回待办事项优先级。
     *
     * @return 优先级，1 最低，5 最高
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 设置待办事项优先级。
     *
     * @param priority 优先级，1 最低，5 最高
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 返回待办事项截止时间。
     *
     * @return 截止时间
     */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /**
     * 设置待办事项截止时间。
     *
     * @param dueDate 截止时间
     */
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * 返回待办事项分类。
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置待办事项分类。
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 返回待办事项标签字符串，使用逗号分隔。
     *
     * @return 标签字符串
     */
    public String getTags() {
        return tags;
    }

    /**
     * 设置待办事项标签字符串，使用逗号分隔。
     *
     * @param tags 标签字符串
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * 返回重复任务类型。
     *
     * @return 重复任务类型
     */
    public String getRecurrenceType() {
        return recurrenceType;
    }

    /**
     * 设置重复任务类型。
     *
     * @param recurrenceType 重复任务类型
     */
    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    /**
     * 返回重复任务间隔。
     *
     * @return 重复任务间隔
     */
    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    /**
     * 设置重复任务间隔。
     *
     * @param recurrenceInterval 重复任务间隔
     */
    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    /**
     * 返回重复任务截止时间。
     *
     * @return 重复任务截止时间
     */
    public LocalDateTime getRecurrenceEndTime() {
        return recurrenceEndTime;
    }

    /**
     * 设置重复任务截止时间。
     *
     * @param recurrenceEndTime 重复任务截止时间
     */
    public void setRecurrenceEndTime(LocalDateTime recurrenceEndTime) {
        this.recurrenceEndTime = recurrenceEndTime;
    }

    /**
     * 返回当前这条重复任务实例的计划触发时间锚点。
     *
     * @return 当前实例的计划触发时间
     */
    public LocalDateTime getNextTriggerTime() {
        return nextTriggerTime;
    }

    /**
     * 设置当前这条重复任务实例的计划触发时间锚点。
     * 该值用于计算下一条重复实例，而不是直接表示“未来另一条实例”的时间。
     *
     * @param nextTriggerTime 当前实例的计划触发时间
     */
    public void setNextTriggerTime(LocalDateTime nextTriggerTime) {
        this.nextTriggerTime = nextTriggerTime;
    }

    /**
     * 返回任务完成时间。
     *
     * @return 完成时间
     */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /**
     * 设置任务完成时间。
     *
     * @param completedAt 完成时间
     */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * 返回软删除时间。
     *
     * @return 软删除时间，未删除时为 null
     */
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * 设置软删除时间。
     *
     * @param deletedAt 软删除时间，恢复时可置为 null
     */
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 返回记录创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置记录创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 返回记录最后更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置记录最后更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 返回子任务 checklist 的聚合摘要信息。
     *
     * @return 子任务聚合摘要
     */
    public TodoSubItemSummaryResponse getSubItemSummary() {
        return subItemSummary;
    }

    /**
     * 设置子任务 checklist 的聚合摘要信息。
     *
     * @param subItemSummary 子任务聚合摘要
     */
    public void setSubItemSummary(TodoSubItemSummaryResponse subItemSummary) {
        this.subItemSummary = subItemSummary;
    }

    /**
     * 在实体首次入库前写入创建时间和更新时间。
     */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.priority == null) {
            this.priority = 3;
        }
        if (this.recurrenceType == null) {
            this.recurrenceType = "NONE";
        }
        if (this.recurrenceInterval == null) {
            this.recurrenceInterval = 1;
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
