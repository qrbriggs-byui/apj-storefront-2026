package edu.byui.apj.storefront.jms.service;

import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import edu.byui.apj.storefront.jms.dto.OrderDetailsItemDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderConfirmationMessageServiceTest {

    @Test
    void buildConfirmationMessage_formatsOrder() {
        OrderConfirmationMessageService svc = new OrderConfirmationMessageService();
        OrderDetailsDto dto = new OrderDetailsDto(
                1L,
                Instant.parse("2026-01-01T12:00:00Z"),
                "COMPLETED",
                49.97,
                10L,
                List.of(
                        new OrderDetailsItemDto("p1", "Blue Eyes White Dragon", 1, 19.99),
                        new OrderDetailsItemDto("p2", "Dark Magician", 1, 29.98)));
        String msg = svc.buildConfirmationMessage(dto);
        assertThat(msg).contains("Order ID: 1");
        assertThat(msg).contains("COMPLETED");
        assertThat(msg).contains("$49.97");
        assertThat(msg).contains("Blue Eyes White Dragon");
        assertThat(msg).contains("Thank you for your order!");
    }
}
