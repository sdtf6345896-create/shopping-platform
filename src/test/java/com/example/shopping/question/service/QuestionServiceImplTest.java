package com.example.shopping.question.service;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.question.dto.request.AnswerRequest;
import com.example.shopping.question.dto.request.QuestionRequest;
import com.example.shopping.question.dto.response.AdminQuestionResponse;
import com.example.shopping.question.dto.response.QuestionResponse;
import com.example.shopping.question.entity.ProductQuestion;
import com.example.shopping.question.mail.QuestionMailSender;
import com.example.shopping.question.repository.ProductQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private ProductQuestionRepository questionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private QuestionMailSender questionMailSender;

    @InjectMocks
    private QuestionServiceImpl questionService;

    private Member member;
    private Product product;

    @BeforeEach
    void setUp() {
        member = new Member();
        member.setId(1L);
        member.setName("王小明");
        member.setEmail("member@example.com");

        product = new Product();
        product.setId(10L);
        product.setName("經典圓領T恤");
        product.setStatus(ProductStatus.ON_SALE);
    }

    private QuestionRequest request(String content) {
        QuestionRequest request = new QuestionRequest();
        request.setContent(content);
        return request;
    }

    private ProductQuestion question(String answer) {
        ProductQuestion question = new ProductQuestion();
        question.setId(5L);
        question.setProduct(product);
        question.setMember(member);
        question.setContent("有其他顏色嗎?");
        question.setAnswer(answer);
        return question;
    }

    @Test
    void ask_savesTrimmedQuestion_withMaskedMemberName() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(memberRepository.getReferenceById(1L)).thenReturn(member);
        when(questionRepository.save(any(ProductQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionResponse response = questionService.ask(1L, 10L, request("  有其他顏色嗎?  "));

        assertThat(response.getContent()).isEqualTo("有其他顏色嗎?");
        assertThat(response.getMemberName()).isEqualTo("王**");
        assertThat(response.getAnswer()).isNull();
    }

    @Test
    void ask_throws_whenProductOffShelf() {
        product.setStatus(ProductStatus.OFF_SHELF);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> questionService.ask(1L, 10L, request("請問?")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(questionRepository, never()).save(any());
    }

    @Test
    void ask_throws_whenAskingTooOften() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(questionRepository.countByMemberIdAndProductIdAndCreatedAtAfter(eq(1L), eq(10L), any(LocalDateTime.class)))
                .thenReturn((long) QuestionServiceImpl.MAX_QUESTIONS_PER_HOUR);

        assertThatThrownBy(() -> questionService.ask(1L, 10L, request("請問?")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("太頻繁");
        verify(questionRepository, never()).save(any());
    }

    @Test
    void answer_setsAnswerAndNotifiesMember_onFirstAnswer() {
        ProductQuestion question = question(null);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        AnswerRequest request = new AnswerRequest();
        request.setAnswer(" 目前只有黑色 ");

        AdminQuestionResponse response = questionService.answer(5L, request);

        assertThat(response.getAnswer()).isEqualTo("目前只有黑色");
        assertThat(response.getAnsweredAt()).isNotNull();
        verify(questionMailSender).notifyAnswered(question);
    }

    @Test
    void answer_doesNotEmailAgain_whenEditingExistingAnswer() {
        ProductQuestion question = question("舊回覆");
        when(questionRepository.findById(5L)).thenReturn(Optional.of(question));
        AnswerRequest request = new AnswerRequest();
        request.setAnswer("新回覆");

        questionService.answer(5L, request);

        assertThat(question.getAnswer()).isEqualTo("新回覆");
        verify(questionMailSender, never()).notifyAnswered(any());
    }

    @Test
    void listAdmin_filtersByAnsweredFlag() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(questionRepository.findByAnsweredAtIsNull(pageable)).thenReturn(new PageImpl<>(List.of(question(null))));

        assertThat(questionService.listAdmin(false, pageable).getContent())
                .singleElement()
                .satisfies(q -> assertThat(q.getProductName()).isEqualTo("經典圓領T恤"));
        verify(questionRepository, never()).findAll(any(PageRequest.class));
    }

    @Test
    void delete_throws_whenQuestionMissing() {
        when(questionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.delete(99L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
