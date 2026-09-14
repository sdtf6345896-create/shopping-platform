package com.example.shopping.member.repository;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.member.entity.Member;
import org.springframework.data.jpa.domain.Specification;

public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    public static Specification<Member> hasStatus(AccountStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Member> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword + "%";
            return cb.or(cb.like(root.get("email"), pattern), cb.like(root.get("name"), pattern));
        };
    }
}
