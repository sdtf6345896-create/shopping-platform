package com.example.shopping.browsinghistory.service;

import com.example.shopping.browsinghistory.dto.response.BrowsingHistoryItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrowsingHistoryService {

    Page<BrowsingHistoryItemResponse> list(Long memberId, Pageable pageable);

    void recordView(Long memberId, Long productId);

    void remove(Long memberId, Long productId);

    void clear(Long memberId);
}
