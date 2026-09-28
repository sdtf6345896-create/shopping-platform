package com.example.shopping;

import com.example.shopping.admin.note.AdminNote;
import com.example.shopping.admin.note.AdminNoteService;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** 備註依對象分開、新到舊;只能刪自己寫的;對象不存在回 404 */
@SpringBootTest
@ActiveProfiles("test")
class AdminNoteIntegrationTest {

    @Autowired
    private AdminNoteService noteService;
    @Autowired
    private MemberRepository memberRepository;

    private final AuthenticatedUser alice = new AuthenticatedUser(901L, "alice", Role.ADMIN);
    private final AuthenticatedUser bob = new AuthenticatedUser(902L, "bob", Role.ADMIN);

    @Test
    void notesPerTarget() {
        Member member = new Member();
        member.setEmail("note-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword("x");
        member.setName("被備註的會員");
        memberRepository.save(member);

        AdminNoteService.NoteResponse first = noteService.add(AdminNote.TargetType.MEMBER, member.getId(),
                "  客人偏好週末到貨  ", alice);
        noteService.add(AdminNote.TargetType.MEMBER, member.getId(), "曾反映包裝破損,已補寄", bob);

        assertThat(noteService.list(AdminNote.TargetType.MEMBER, member.getId()))
                .extracting(AdminNoteService.NoteResponse::adminUsername, AdminNoteService.NoteResponse::content)
                .containsExactly(
                        tuple("bob", "曾反映包裝破損,已補寄"),
                        tuple("alice", "客人偏好週末到貨"));

        assertThatThrownBy(() -> noteService.delete(first.id(), bob)).isInstanceOf(BusinessException.class);
        noteService.delete(first.id(), alice);
        assertThat(noteService.list(AdminNote.TargetType.MEMBER, member.getId())).hasSize(1);

        assertThatThrownBy(() -> noteService.add(AdminNote.TargetType.ORDER, -1L, "x", alice))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
