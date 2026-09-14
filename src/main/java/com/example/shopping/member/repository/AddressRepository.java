package com.example.shopping.member.repository;

import com.example.shopping.member.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByMemberIdOrderByIsDefaultDescIdDesc(Long memberId);

    Optional<Address> findByIdAndMemberId(Long id, Long memberId);
}
