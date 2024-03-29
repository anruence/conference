package com.microsoft.conference.payments.domain.event;

import com.microsoft.conference.payments.domain.model.PaymentItem;
import lombok.Getter;
import lombok.Setter;
import org.enodeframework.eventing.AbstractDomainEventMessage;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PaymentInitiated extends AbstractDomainEventMessage {
    private String orderId;
    private String conferenceId;
    private String description;
    private BigDecimal totalAmount;
    private List<PaymentItem> paymentItems;

    public PaymentInitiated() {
    }

    public PaymentInitiated(String orderId, String conferenceId, String description, BigDecimal totalAmount, List<PaymentItem> paymentItems) {
        this.orderId = orderId;
        this.conferenceId = conferenceId;
        this.description = description;
        this.totalAmount = totalAmount;
        this.paymentItems = paymentItems;
    }
}
