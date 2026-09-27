package com.example.shopping.question.service;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.question.dto.request.AnswerRequest;
import com.example.shopping.question.dto.request.QuestionRequest;
import com.example.shopping.question.dto.response.AdminQuestionResponse;
import com.example.shopping.question.dto.response.QuestionResponse;
import com.example.shopping.question.entity.ProductQuestion;
import com.example.shopping.question.mail.QuestionNotifier;
import com.example.shopping.question.repository.ProductQuestionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {

    /** 同一會員對同一商品,一小時內最多提問次數 */
    static final int MAX_QUESTIONS_PER_HOUR = 5;

    private final ProductQuestionRepository questionRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final QuestionNotifier questionNotifier;

    public QuestionServiceImpl(ProductQuestionRepository questionRepository,
                               ProductRepository productRepository,
                               MemberRepository memberRepository,
                               QuestionNotifier questionNotifier) {
        this.questionRepository = questionRepository;
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
        this.questionNotifier = questionNotifier;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> listByProduct(Long productId, Pageable pageable) {
        return questionRepository.findByProductId(productId, pageable).map(QuestionResponse::from);
    }

    @Override
    public QuestionResponse ask(Long memberId, Long productId, QuestionRequest request) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        long recent = questionRepository.countByMemberIdAndProductIdAndCreatedAtAfter(
                memberId, productId, LocalDateTime.now().minusHours(1));
        if (recent >= MAX_QUESTIONS_PER_HOUR) {
            throw new BusinessException("提問太頻繁,請稍後再試");
        }

        ProductQuestion question = new ProductQuestion();
        question.setProduct(product);
        question.setMember(memberRepository.getReferenceById(memberId));
        question.setContent(request.getContent().trim());
        return QuestionResponse.from(questionRepository.save(question));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminQuestionResponse> listAdmin(Boolean answered, Pageable pageable) {
        Page<ProductQuestion> page;
        if (answered == null) {
            page = questionRepository.findAll(pageable);
        } else if (answered) {
            page = questionRepository.findByAnsweredAtIsNotNull(pageable);
        } else {
            page = questionRepository.findByAnsweredAtIsNull(pageable);
        }
        return page.map(AdminQuestionResponse::from);
    }

    @Override
    public AdminQuestionResponse answer(Long questionId, AnswerRequest request) {
        ProductQuestion question = findOrThrow(questionId);
        boolean firstAnswer = question.getAnswer() == null;

        question.setAnswer(request.getAnswer().trim());
        question.setAnsweredAt(LocalDateTime.now());
        // 修改既有回覆不再重複寄信
        if (firstAnswer) {
            questionNotifier.notifyAnswered(question);
        }
        return AdminQuestionResponse.from(question);
    }

    @Override
    public void delete(Long questionId) {
        questionRepository.delete(findOrThrow(questionId));
    }

    private ProductQuestion findOrThrow(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("提問不存在"));
    }
}
