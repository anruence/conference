package com.microsoft.conference.registration.processmanager;

import com.microsoft.conference.common.management.commands.CancelSeatReservation;
import com.microsoft.conference.common.management.commands.CommitSeatReservation;
import com.microsoft.conference.common.management.commands.MakeSeatReservation;
import com.microsoft.conference.common.management.commands.SeatReservationItemInfo;
import com.microsoft.conference.common.management.message.SeatInsufficientMessage;
import com.microsoft.conference.common.management.message.SeatsReservationCancelledMessage;
import com.microsoft.conference.common.management.message.SeatsReservationCommittedMessage;
import com.microsoft.conference.common.management.message.SeatsReservedMessage;
import com.microsoft.conference.common.payment.message.PaymentCompletedMessage;
import com.microsoft.conference.common.payment.message.PaymentRejectedMessage;
import com.microsoft.conference.common.registration.commands.order.CloseOrder;
import com.microsoft.conference.common.registration.commands.order.ConfirmPayment;
import com.microsoft.conference.common.registration.commands.order.ConfirmReservation;
import com.microsoft.conference.common.registration.commands.order.MarkAsSuccess;
import com.microsoft.conference.common.registration.commands.seatassignment.CreateSeatAssignments;
import com.microsoft.conference.registration.domain.order.event.OrderExpired;
import com.microsoft.conference.registration.domain.order.event.OrderPaymentConfirmed;
import com.microsoft.conference.registration.domain.order.event.OrderPlaced;
import com.microsoft.conference.registration.domain.order.event.OrderSuccessed;
import com.microsoft.conference.registration.domain.order.model.OrderStatus;
import org.enodeframework.annotation.Event;
import org.enodeframework.annotation.Subscribe;
import org.enodeframework.commanding.CommandBus;
import org.enodeframework.common.io.Task;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * IMessageHandler<OrderPlaced>,                           //?????????????????????(Order)
 * IMessageHandler<SeatsReservedMessage>,                  //??????????????????????????????(Conference)
 * IMessageHandler<SeatInsufficientMessage>,               //????????????????????????????????????(Conference)
 * IMessageHandler<PaymentCompletedMessage>,               //?????????????????????(Payment)
 * IMessageHandler<PaymentRejectedMessage>,                //?????????????????????(Payment)
 * IMessageHandler<OrderPaymentConfirmed>,                 //?????????????????????(Order)
 * IMessageHandler<SeatsReservationCommittedMessage>,      //???????????????????????????(Conference)
 * IMessageHandler<SeatsReservationCancelledMessage>,      //???????????????????????????(Conference)
 * IMessageHandler<OrderSuccessed>,                        //???????????????????????????(Order)
 * IMessageHandler<OrderExpired>                           //???????????????(15????????????)??????(Order)
 */
@Event
public class RegistrationProcessManager {

    private final CommandBus commandService;

    public RegistrationProcessManager(CommandBus commandService) {
        this.commandService = commandService;
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(OrderPlaced evnt) {
        MakeSeatReservation reservation = new MakeSeatReservation(evnt.getConferenceId());
        reservation.reservationId = evnt.getAggregateRootId();
        reservation.seats = evnt.getOrderTotal().getOrderLines().stream().map(x -> {
            SeatReservationItemInfo itemInfo = new SeatReservationItemInfo();
            itemInfo.seatType = x.getSeatQuantity().getSeatType().getSeatTypeId();
            itemInfo.quantity = x.getSeatQuantity().getQuantity();
            return itemInfo;
        }).collect(Collectors.toList());
        return (commandService.sendAsync(reservation));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(SeatsReservedMessage message) {
        return (commandService.sendAsync(new ConfirmReservation(message.reservationId, true)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(SeatInsufficientMessage message) {
        return (commandService.sendAsync(new ConfirmReservation(message.reservationId, false)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(PaymentCompletedMessage message) {
        return (commandService.sendAsync(new ConfirmPayment(message.orderId, true)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(PaymentRejectedMessage message) {
        return (commandService.sendAsync(new ConfirmPayment(message.orderId, false)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(OrderPaymentConfirmed evnt) {
        if (OrderStatus.PaymentSuccess == evnt.getOrderStatus()) {
            return (commandService.sendAsync(new CommitSeatReservation(evnt.getConferenceId(), evnt.getAggregateRootId())));
        } else if (evnt.getOrderStatus() == OrderStatus.PaymentRejected) {
            return (commandService.sendAsync(new CancelSeatReservation(evnt.getConferenceId(), evnt.getAggregateRootId())));
        }
        return Task.completedTask;
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(SeatsReservationCommittedMessage message) {
        return (commandService.sendAsync(new MarkAsSuccess(message.reservationId)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(SeatsReservationCancelledMessage message) {
        return (commandService.sendAsync(new CloseOrder(message.reservationId)));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(OrderSuccessed evnt) {
        return (commandService.sendAsync(new CreateSeatAssignments(evnt.getAggregateRootId())));
    }

    @Subscribe
    public CompletableFuture<Boolean> handleAsync(OrderExpired evnt) {
        return (commandService.sendAsync(new CancelSeatReservation(evnt.getConferenceId(), evnt.getAggregateRootId())));
    }
}
        
