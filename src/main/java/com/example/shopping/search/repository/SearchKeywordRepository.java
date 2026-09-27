package com.example.shopping.search.repository;

import com.example.shopping.search.entity.SearchKeyword;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchKeywordRepository extends JpaRepository<SearchKeyword, Long> {

    /** 次數 +1(相對更新,併發搜尋不會遺失計數),回傳 0 表示關鍵字尚未存在 */
    @Modifying
    @Query("update SearchKeyword k set k.searchCount = k.searchCount + 1, k.lastSearchedAt = :now "
            + "where k.keyword = :keyword")
    int increment(@Param("keyword") String keyword, @Param("now") LocalDateTime now);

    @Query("select k from SearchKeyword k where k.lastSearchedAt >= :since order by k.searchCount desc, k.keyword asc")
    List<SearchKeyword> findHot(@Param("since") LocalDateTime since, Pageable pageable);
}
