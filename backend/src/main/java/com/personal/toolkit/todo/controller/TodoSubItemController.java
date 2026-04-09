package com.personal.toolkit.todo.controller;

import com.personal.toolkit.common.api.ApiResponse;
import com.personal.toolkit.todo.dto.TodoSubItemRequest;
import com.personal.toolkit.todo.dto.TodoSubItemResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.service.TodoSubItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 暴露主任务下 checklist 子任务的嵌套 REST 接口。
 */
@RestController
@RequestMapping("/api/todos/{todoId}/sub-items")
public class TodoSubItemController {

    private final TodoSubItemService todoSubItemService;

    public TodoSubItemController(TodoSubItemService todoSubItemService) {
        this.todoSubItemService = todoSubItemService;
    }

    /**
     * 查询指定主任务下的所有子任务列表。
     *
     * @param todoId 主任务主键
     * @return 子任务列表响应
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoSubItemResponse>>> findByTodoId(@PathVariable Long todoId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo sub items fetched successfully",
                todoSubItemService.findByTodoId(todoId)
        ));
    }

    /**
     * 在指定主任务下创建新的子任务记录。
     *
     * @param todoId 主任务主键
     * @param request 子任务请求体
     * @return 创建后的子任务响应
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TodoSubItemResponse>> create(@PathVariable Long todoId,
                                                                   @Valid @RequestBody TodoSubItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Todo sub item created successfully",
                todoSubItemService.create(todoId, request)
        ));
    }

    /**
     * 更新指定主任务下的目标子任务。
     *
     * @param todoId 主任务主键
     * @param subItemId 子任务主键
     * @param request 子任务请求体
     * @return 更新后的子任务响应
     */
    @PutMapping("/{subItemId}")
    public ResponseEntity<ApiResponse<TodoSubItemResponse>> update(@PathVariable Long todoId,
                                                                   @PathVariable Long subItemId,
                                                                   @Valid @RequestBody TodoSubItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo sub item updated successfully",
                todoSubItemService.update(todoId, subItemId, request)
        ));
    }

    /**
     * 删除指定主任务下的目标子任务。
     *
     * @param todoId 主任务主键
     * @param subItemId 子任务主键
     * @return 删除成功响应
     */
    @DeleteMapping("/{subItemId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long todoId,
                                                    @PathVariable Long subItemId) {
        todoSubItemService.delete(todoId, subItemId);
        return ResponseEntity.ok(ApiResponse.success("Todo sub item deleted successfully"));
    }

    /**
     * 查询指定主任务下 checklist 的聚合摘要信息。
     *
     * @param todoId 主任务主键
     * @return checklist 聚合摘要响应
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TodoSubItemSummaryResponse>> getSummary(@PathVariable Long todoId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo sub item summary fetched successfully",
                todoSubItemService.getSummary(todoId)
        ));
    }
}
