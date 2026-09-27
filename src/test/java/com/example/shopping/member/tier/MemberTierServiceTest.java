package com.example.shopping.member.tier;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberTierServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private MemberTierService memberTierService;

    private void stubSpending(String amount) {
        when(orderRepository.sumMerchandiseAmount(eq(1L), eq(OrderStatus.COMPLETED), any(LocalDateTime.class)))
                .thenReturn(amount == null ? null : new BigDecimal(amount));
    }

    @Test
    void tierBoundaries() {
        assertThat(MemberTier.forSpending(new BigDecimal("4999.99"))).isEqualTo(MemberTier.NORMAL);
        assertThat(MemberTier.forSpending(new BigDecimal("5000"))).isEqualTo(MemberTier.SILVER);
        assertThat(MemberTier.forSpending(new BigDecimal("20000"))).isEqualTo(MemberTier.GOLD);
    }

    @Test
    void describe_newMember_isNormalWithFullAmountToSilver() {
        stubSpending(null);

        MemberTierResponse response = memberTierService.describe(1L);

        assertThat(response.tier()).isEqualTo(MemberTier.NORMAL);
        assertThat(response.spending()).isEqualByComparingTo("0");
        assertThat(response.nextTier()).isEqualTo(MemberTier.SILVER);
        assertThat(response.amountToNext()).isEqualByComparingTo("5000");
    }

    @Test
    void describe_silverMember_showsDistanceToGold() {
        stubSpending("8000");

        MemberTierResponse response = memberTierService.describe(1L);

        assertThat(response.tier()).isEqualTo(MemberTier.SILVER);
        assertThat(response.pointsMultiplier()).isEqualByComparingTo("1.5");
        assertThat(response.amountToNext()).isEqualByComparingTo("12000");
    }

    @Test
    void describe_goldMember_hasNoNextTier() {
        stubSpending("30000");

        MemberTierResponse response = memberTierService.describe(1L);

        assertThat(response.tier()).isEqualTo(MemberTier.GOLD);
        assertThat(response.nextTier()).isNull();
        assertThat(response.amountToNext()).isEqualByComparingTo("0");
    }
}
