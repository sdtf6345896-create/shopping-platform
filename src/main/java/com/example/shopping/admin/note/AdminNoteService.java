package com.example.shopping.admin.note;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AdminNoteService {

    private final AdminNoteRepository noteRepository;
    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    public AdminNoteService(AdminNoteRepository noteRepository,
                            OrderRepository orderRepository,
                            MemberRepository memberRepository) {
        this.noteRepository = noteRepository;
        this.orderRepository = orderRepository;
        this.memberRepository = memberRepository;
    }

    public record NoteResponse(Long id, String adminUsername, String content, LocalDateTime createdAt) {

        static NoteResponse from(AdminNote note) {
            return new NoteResponse(note.getId(), note.getAdminUsername(), note.getContent(), note.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(AdminNote.TargetType type, Long targetId) {
        requireTarget(type, targetId);
        return noteRepository.findByTargetTypeAndTargetIdOrderByIdDesc(type, targetId).stream()
                .map(NoteResponse::from).toList();
    }

    public NoteResponse add(AdminNote.TargetType type, Long targetId, String content, AuthenticatedUser admin) {
        requireTarget(type, targetId);
        AdminNote note = new AdminNote();
        note.setTargetType(type);
        note.setTargetId(targetId);
        note.setAdminId(admin.id());
        note.setAdminUsername(admin.subject());
        note.setContent(content.trim());
        return NoteResponse.from(noteRepository.save(note));
    }

    /** 只能刪自己寫的備註,避免誤刪同事的紀錄 */
    public void delete(Long noteId, AuthenticatedUser admin) {
        AdminNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("備註不存在"));
        if (!note.getAdminId().equals(admin.id())) {
            throw new BusinessException("只能刪除自己寫的備註");
        }
        noteRepository.delete(note);
    }

    private void requireTarget(AdminNote.TargetType type, Long targetId) {
        boolean exists = switch (type) {
            case ORDER -> orderRepository.existsById(targetId);
            case MEMBER -> memberRepository.existsById(targetId);
        };
        if (!exists) {
            throw new ResourceNotFoundException(type == AdminNote.TargetType.ORDER ? "訂單不存在" : "會員不存在");
        }
    }
}
