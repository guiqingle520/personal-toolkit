package com.personal.toolkit.todo.controller;

import com.personal.toolkit.common.api.ApiResponse;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoBatchRequest;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoQueryRequest;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 暴露 Todo 资源的 REST 接口，负责接收请求并返回统一响应结构。
 */
@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /**
     * 按筛选条件分页查询待办事项列表。
     *
     * @param queryRequest 筛选条件对象
     * @param page 页码，从 0 开始
     * @param size 每页条数
     * @param sortBy 排序字段
     * @param sortDir 排序方向
     * @return 包含分页待办列表的统一响应体
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TodoItem>>> findAll(
            @ModelAttribute TodoQueryRequest queryRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return ResponseEntity.ok(ApiResponse.success(
                "Todo list fetched successfully",
                todoService.findAll(queryRequest, page, size, sort)
        ));
    }

    /**
     * 查询前端筛选器所需的分类与标签候选项。
     *
     * @return 分类与标签候选项响应体
     */
    @GetMapping("/options")
    public ResponseEntity<ApiResponse<TodoOptionResponse>> getOptions() {
        return ResponseEntity.ok(ApiResponse.success("Todo options fetched successfully", todoService.getOptions()));
    }

    /**
     * 按主键查询单个待办事项。
     *
     * @param id 待办事项主键
     * @return 包含待办详情的统一响应体
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoItem>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Todo item fetched successfully", todoService.findById(id)));
    }

    /**
     * 新增一条待办事项记录。
     *
     * @param request 待办事项创建请求
     * @return 包含新建结果的统一响应体
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TodoItem>> create(@Valid @RequestBody TodoItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Todo item created successfully", todoService.create(request)));
    }

    /**
     * 更新指定主键的待办事项。
     *
     * @param id 待办事项主键
     * @param request 待办事项更新请求
     * @return 包含更新结果的统一响应体
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoItem>> update(@PathVariable Long id,
                                                        @Valid @RequestBody TodoItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Todo item updated successfully", todoService.update(id, request)));
    }

    /**
     * 删除指定主键的待办事项。
     *
     * @param id 待办事项主键
     * @return 表示删除成功的统一响应体
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        todoService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Todo item deleted successfully"));
    }

    /**
     * 批量将待办事项标记为完成。
     *
     * @param request 批量操作请求
     * @return 批量更新结果
     */
    @PostMapping("/batch/complete")
    public ResponseEntity<ApiResponse<List<TodoItem>>> batchComplete(@Valid @RequestBody TodoBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo items completed successfully",
                todoService.batchComplete(request.getIds())
        ));
    }

    /**
     * 批量将待办事项移动到回收站。
     *
     * @param request 批量操作请求
     * @return 表示软删除成功的统一响应体
     */
    @PostMapping("/batch/delete")
    public ResponseEntity<ApiResponse<Void>> batchDelete(@Valid @RequestBody TodoBatchRequest request) {
        todoService.batchDelete(request.getIds());
        return ResponseEntity.ok(ApiResponse.success("Todo items deleted successfully"));
    }

    /**
     * 查询回收站中的待办事项。
     *
     * @param queryRequest 筛选条件对象
     * @param page 页码，从 0 开始
     * @param size 每页条数
     * @param sortBy 排序字段
     * @param sortDir 排序方向
     * @return 包含回收站分页数据的统一响应体
     */
    @GetMapping("/recycle-bin")
    public ResponseEntity<ApiResponse<PageResponse<TodoItem>>> recycleBin(
            @ModelAttribute TodoQueryRequest queryRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updateTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        queryRequest.setIncludeDeleted(true);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortBy);
        return ResponseEntity.ok(ApiResponse.success(
                "Recycle bin fetched successfully",
                todoService.findAll(queryRequest, page, size, sort)
        ));
    }

    /**
     * 恢复单个回收站中的待办事项。
     *
     * @param id 待办事项主键
     * @return 恢复后的待办事项统一响应体
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<TodoItem>> restore(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Todo item restored successfully", todoService.restore(id)));
    }

    /**
     * 批量恢复回收站中的待办事项。
     *
     * @param request 批量操作请求
     * @return 批量恢复结果
     */
    @PostMapping("/batch/restore")
    public ResponseEntity<ApiResponse<List<TodoItem>>> batchRestore(@Valid @RequestBody TodoBatchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo items restored successfully",
                todoService.batchRestore(request.getIds())
        ));
    }
}
