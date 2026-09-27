package com.example.shopping.question.service;

import com.example.shopping.question.dto.request.AnswerRequest;
import com.example.shopping.question.dto.request.QuestionRequest;
import com.example.shopping.question.dto.response.AdminQuestionResponse;
import com.example.shopping.question.dto.response.QuestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {

    Page<QuestionResponse> listByProduct(Long productId, Pageable pageable);

    QuestionResponse ask(Long memberId, Long productId, QuestionRequest request);

    /** @param answered null = 全部,true = 已回覆,false = 待回覆 */
    Page<AdminQuestionResponse> listAdmin(Boolean answered, Pageable pageable);

    AdminQuestionResponse answer(Long questionId, AnswerRequest request);

    void delete(Long questionId);
}
