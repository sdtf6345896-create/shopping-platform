package com.example.shopping.admin.repository;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.common.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 啟用中後台帳號的目前角色;停用或不存在回傳 empty */
    @Query("select a.role from Admin a where a.id = :id"
            + " and a.status = com.example.shopping.common.enums.AccountStatus.ACTIVE")
    Optional<String> findActiveRoleById(@Param("id") Long id);

    long countByRoleAndStatus(String role, AccountStatus status);
}
