package edu.byui.apj.storefront.jms.service;

import edu.byui.apj.storefront.jms.dto.OrderDetailsDto;
import edu.byui.apj.storefront.jms.dto.OrderDetailsItemDto;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class OrderConfirmationMessageService {

    public String buildConfirmationMessage(OrderDetailsDto details) {
        StringBuilder sb = new StringBuilder();
        sb.append("Order Confirmation\n");
        sb.append("Order ID: ").append(details.orderId()).append('\n');
        sb.append("Status: ").append(details.status()).append('\n');
        sb.append(String.format(Locale.US, "Total: $%.2f\n", details.totalAmount()));
        sb.append("Items:\n");
        for (OrderDetailsItemDto line : details.items()) {
            double lineTotal = line.price() * line.quantity();
            sb.append(String.format(Locale.US, "- %s x%d - $%.2f\n",
                    line.productName(), line.quantity(), lineTotal));
        }
        sb.append("Thank you for your order!");
        return sb.toString();
    }
}
