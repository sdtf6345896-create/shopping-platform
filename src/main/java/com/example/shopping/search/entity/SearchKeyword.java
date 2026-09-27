package com.example.shopping.search.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_keyword")
@Getter
@Setter
@NoArgsConstructor
public class SearchKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String keyword;

    @Column(name = "search_count", nullable = false)
    private long searchCount;

    @Column(name = "last_searched_at", nullable = false)
    private LocalDateTime lastSearchedAt;
}
