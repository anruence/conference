package com.microsoft.conference.payments.domain.model;

import com.microsoft.conference.common.exception.InvalidOperationException;
import com.microsoft.conference.payments.domain.event.PaymentCompleted;
import com.microsoft.conference.payments.domain.event.PaymentInitiated;
import com.microsoft.conference.payments.domain.event.PaymentRejected;
import org.enodeframework.domain.AbstractAggregateRoot;

import java.math.BigDecimal;
import java.util.List;

public class Payment extends AbstractAggregateRoot {
    private String orderId;
    private String conferenceId;
    private int state;
    private String description;
    private BigDecimal totalAmount;
    private List<PaymentItem> paymentItems;

    public Payment() {
    }

    public Payment(String id, String orderId, String conferenceId, String description, BigDecimal totalAmount, List<PaymentItem> items) {
        super(id);
        applyEvent(new PaymentInitiated(orderId, conferenceId, description, totalAmount, items));
    }

    public void complete() {
        if (state != PaymentState.Initiated) {
            throw new InvalidOperationException();
        }
        applyEvent(new PaymentCompleted(this, orderId, conferenceId));
    }

    public void cancel() {
        if (state != PaymentState.Initiated) {
            throw new InvalidOperationException();
        }
        applyEvent(new PaymentRejected(orderId, conferenceId));
    }

    private void handle(PaymentInitiated evnt) {
        id = evnt.getAggregateRootId();
        orderId = evnt.getOrderId();
        conferenceId = evnt.getConferenceId();
        description = evnt.getDescription();
        totalAmount = evnt.getTotalAmount();
        state = PaymentState.Initiated;
        paymentItems = evnt.getPaymentItems();
    }

    private void handle(PaymentCompleted evnt) {
        state = PaymentState.Completed;
    }

    private void handle(PaymentRejected evnt) {
        state = PaymentState.Rejected;
    }
}
