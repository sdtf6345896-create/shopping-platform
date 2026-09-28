package com.example.shopping.member.referral;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReferralServiceTest {

    @Test
    void maskName_keepsOnlyTheFirstCharacter() {
        assertThat(ReferralService.maskName("王小明")).isEqualTo("王**");
        assertThat(ReferralService.maskName("歐陽娜娜")).isEqualTo("歐**");
        assertThat(ReferralService.maskName("林")).isEqualTo("林*");
        assertThat(ReferralService.maskName("")).isEqualTo("好友");
    }
}
