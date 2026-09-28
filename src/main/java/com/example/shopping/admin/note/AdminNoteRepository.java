package com.example.shopping.admin.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNoteRepository extends JpaRepository<AdminNote, Long> {

    List<AdminNote> findByTargetTypeAndTargetIdOrderByIdDesc(AdminNote.TargetType targetType, Long targetId);
}
