package com.example.shopping.promotion;

import com.example.shopping.promotion.PromotionCalculator.Line;
import com.example.shopping.promotion.PromotionCalculator.Result;
import com.example.shopping.promotion.PromotionCalculator.Rule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PromotionCalculatorTest {

    // 分類樹:1 女裝 > 11 上衣;2 3C
    private static Line top(String price, int qty) {
        return new Line(Set.of(11L, 1L), new BigDecimal(price), qty);
    }

    private static Line gadget(String price, int qty) {
        return new Line(Set.of(2L), new BigDecimal(price), qty);
    }

    @Test
    void appliesCategoryPromotionIncludingSubcategories_roundingDown() {
        Rule womenTwoFor10 = new Rule(1L, "女裝任選 2 件 9 折", 1L, 2, 10);

        Result result = PromotionCalculator.evaluate(List.of(womenTwoFor10),
                List.of(top("399", 1), top("455", 1), gadget("1000", 1)));

        // 只算女裝:(399 + 455) × 10% = 85.4 → 85
        assertThat(result.promotionId()).isEqualTo(1L);
        assertThat(result.discount()).isEqualByComparingTo("85");
        assertThat(result.hints()).isEmpty();
    }

    @Test
    void picksTheLargestDiscount_andHintsTheOnesNotYetReached() {
        Rule siteWide3 = new Rule(1L, "全站 3 件 85 折", null, 3, 15);
        Rule women2 = new Rule(2L, "女裝 2 件 9 折", 1L, 2, 10);
        Rule gadget2 = new Rule(3L, "3C 2 件 95 折", 2L, 2, 5);

        Result result = PromotionCalculator.evaluate(List.of(siteWide3, women2, gadget2),
                List.of(top("500", 2), gadget("300", 1)));

        // 全站:1300 × 15% = 195 > 女裝:1000 × 10% = 100
        assertThat(result.promotionName()).isEqualTo("全站 3 件 85 折");
        assertThat(result.discount()).isEqualByComparingTo("195");
        assertThat(result.hints()).singleElement()
                .satisfies(hint -> {
                    assertThat(hint.promotionId()).isEqualTo(3L);
                    assertThat(hint.missingQuantity()).isEqualTo(1);
                });
    }

    @Test
    void noDiscountWhenNothingQualifies_butStillHints() {
        Rule women2 = new Rule(2L, "女裝 2 件 9 折", 1L, 2, 10);

        Result result = PromotionCalculator.evaluate(List.of(women2), List.of(top("500", 1), gadget("300", 5)));

        assertThat(result.applied()).isFalse();
        assertThat(result.discount()).isZero();
        assertThat(result.hints()).extracting(PromotionCalculator.Hint::missingQuantity).containsExactly(1);
    }
}
