package com.example.shopping.order.shipping;

import com.example.shopping.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final ShippingPolicy shippingPolicy;

    public ShippingController(ShippingPolicy shippingPolicy) {
        this.shippingPolicy = shippingPolicy;
    }

    /** 公開的運費規則,前台購物車 / 結帳頁用來顯示「再買多少免運」 */
    @GetMapping("/policy")
    public ApiResponse<ShippingPolicyResponse> policy() {
        return ApiResponse.success(new ShippingPolicyResponse(shippingPolicy.getFee(), shippingPolicy.getFreeThreshold()));
    }
}
