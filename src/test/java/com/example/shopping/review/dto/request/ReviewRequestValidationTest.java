package com.example.shopping.review.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<ReviewRequest>> validate(List<String> images) {
        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setImages(images);
        return validator.validate(request);
    }

    @Test
    void acceptsImagesUploadedToThisSite() {
        assertThat(validate(List.of("/uploads/3f2a9c1e-1111-2222-3333-444455556666.jpg",
                "/uploads/abc-def.webp"))).isEmpty();
    }

    @Test
    void rejectsExternalUrlsAndPathTricks() {
        assertThat(validate(List.of("https://evil.example.com/track.png"))).isNotEmpty();
        assertThat(validate(List.of("/uploads/../application.yml"))).isNotEmpty();
        assertThat(validate(List.of("/uploads/x.svg"))).isNotEmpty();
    }

    @Test
    void rejectsMoreThanFiveImages() {
        List<String> six = List.of("/uploads/a.jpg", "/uploads/b.jpg", "/uploads/c.jpg", "/uploads/d.jpg",
                "/uploads/e.jpg", "/uploads/f.jpg");

        assertThat(validate(six)).extracting(v -> v.getMessage()).contains("評論照片最多 5 張");
    }
}
