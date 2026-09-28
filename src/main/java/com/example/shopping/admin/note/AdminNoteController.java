package com.example.shopping.admin.note;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/notes")
public class AdminNoteController {

    private final AdminNoteService noteService;

    public AdminNoteController(AdminNoteService noteService) {
        this.noteService = noteService;
    }

    public record NoteRequest(
            @NotNull(message = "請指定備註對象") AdminNote.TargetType targetType,
            @NotNull(message = "請指定備註對象") Long targetId,
            @NotBlank(message = "備註內容不可為空") @Size(max = 500, message = "備註最多 500 字") String content) {
    }

    @GetMapping
    public ApiResponse<List<AdminNoteService.NoteResponse>> list(@RequestParam AdminNote.TargetType targetType,
                                                                 @RequestParam Long targetId) {
        return ApiResponse.success(noteService.list(targetType, targetId));
    }

    @PostMapping
    public ApiResponse<AdminNoteService.NoteResponse> add(@Valid @RequestBody NoteRequest request) {
        return ApiResponse.success("已新增備註", noteService.add(request.targetType(), request.targetId(),
                request.content(), SecurityUtils.getCurrentUser()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noteService.delete(id, SecurityUtils.getCurrentUser());
        return ApiResponse.success("已刪除", null);
    }
}
