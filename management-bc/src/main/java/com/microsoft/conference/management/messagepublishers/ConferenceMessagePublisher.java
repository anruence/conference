package com.microsoft.conference.management.messagepublishers;

import com.microsoft.conference.common.management.message.*;
import com.microsoft.conference.management.domain.event.SeatsReservationCancelled;
import com.microsoft.conference.management.domain.event.SeatsReservationCommitted;
import com.microsoft.conference.management.domain.event.SeatsReserved;
import com.microsoft.conference.management.domain.publishableexception.SeatInsufficientException;
import org.enodeframework.annotation.Event;
import org.enodeframework.annotation.Subscribe;
import org.enodeframework.messaging.ApplicationMessage;
import org.enodeframework.messaging.MessagePublisher;
import org.enodeframework.queue.SendMessageResult;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * IMessageHandler<SeatsReserved>,
 * IMessageHandler<SeatsReservationCommitted>,
 * IMessageHandler<SeatsReservationCancelled>,
 * IMessageHandler<SeatInsufficientException>
 */
@Event
public class ConferenceMessagePublisher {

    @Autowired
    private MessagePublisher<ApplicationMessage> messagePublisher;

    @Subscribe
    public CompletableFuture<SendMessageResult> handleAsync(SeatsReserved evnt) {
        SeatsReservedMessage message = new SeatsReservedMessage();
        message.conferenceId = evnt.getAggregateRootId();
        message.reservationId = evnt.getReservationId();
        message.reservationItems = evnt.getReservationItems().stream().map(x -> {
            SeatReservationItem item = new SeatReservationItem();
            item.seatTypeId = x.getSeatTypeId();
            item.quantity = x.getQuantity();
            return item;
        }).collect(Collectors.toList());
        return (messagePublisher.publishAsync(message));
    }

    @Subscribe
    public CompletableFuture<SendMessageResult> handleAsync(SeatsReservationCommitted evnt) {
        SeatsReservationCommittedMessage message = new SeatsReservationCommittedMessage();
        message.conferenceId = evnt.getAggregateRootId();
        message.reservationId = evnt.getReservationId();
        return (messagePublisher.publishAsync(message));
    }

    @Subscribe
    public CompletableFuture<SendMessageResult> handleAsync(SeatsReservationCancelled evnt) {
        SeatsReservationCancelledMessage message = new SeatsReservationCancelledMessage();
        message.conferenceId = evnt.getAggregateRootId();
        message.reservationId = evnt.getReservationId();
        return (messagePublisher.publishAsync(message));
    }

    @Subscribe
    public CompletableFuture<SendMessageResult> handleAsync(SeatInsufficientException exception) {
        SeatInsufficientMessage message = new SeatInsufficientMessage();
        message.conferenceId = exception.conferenceId;
        message.reservationId = exception.reservationId;
        return (messagePublisher.publishAsync(message));
    }
}
