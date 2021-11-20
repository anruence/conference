package com.microsoft.conference.payments.messagepublisher;

import com.microsoft.conference.common.payment.message.PaymentCompletedMessage;
import com.microsoft.conference.common.payment.message.PaymentRejectedMessage;
import com.microsoft.conference.payments.domain.event.PaymentCompleted;
import com.microsoft.conference.payments.domain.event.PaymentRejected;
import org.enodeframework.annotation.Event;
import org.enodeframework.annotation.Subscribe;
import org.enodeframework.messaging.IApplicationMessage;
import org.enodeframework.messaging.IMessagePublisher;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Event
public class PaymentMessagePublisher {

    @Resource
    private final IMessagePublisher<IApplicationMessage> messagePublisher;

    public PaymentMessagePublisher(IMessagePublisher<IApplicationMessage> messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(PaymentCompleted evnt) {
        PaymentCompletedMessage message = new PaymentCompletedMessage();
        message.paymentId = evnt.getAggregateRootId();
        message.conferenceId = evnt.getConferenceId();
        message.orderId = evnt.getOrderId();
        return (messagePublisher.publishAsync(message));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(PaymentRejected evnt) {
        PaymentRejectedMessage message = new PaymentRejectedMessage();
        message.paymentId = evnt.getAggregateRootId();
        message.conferenceId = evnt.getConferenceId();
        message.orderId = evnt.getOrderId();
        return (messagePublisher.publishAsync(message));
    }
}
