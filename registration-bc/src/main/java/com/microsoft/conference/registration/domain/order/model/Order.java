package com.microsoft.conference.registration.domain.order.model;

import com.microsoft.conference.common.exception.InvalidOperationException;
import com.microsoft.conference.registration.domain.SeatQuantity;
import com.microsoft.conference.registration.domain.order.PricingService;
import com.microsoft.conference.registration.domain.order.event.*;
import com.microsoft.conference.registration.domain.seatassigning.model.OrderSeatAssignments;
import org.enodeframework.common.utils.Assert;
import org.enodeframework.common.utils.IdGenerator;
import org.enodeframework.domain.AbstractAggregateRoot;

import java.util.Date;
import java.util.List;

public class Order extends AbstractAggregateRoot {
    private OrderTotal total;
    private String conferenceId;
    private OrderStatus status;
    private Registrant registrant;
    private String accessCode;

    public Order() {

    }

    public Order(String id, String conferenceId, List<SeatQuantity> seats, PricingService pricingService) {
        super(id);
        Assert.nonNull(id, "id");
        Assert.nonNull(conferenceId, "conferenceId");
        Assert.nonNull(seats, "seats");
        Assert.nonNull(pricingService, "pricingService");
        if (seats.isEmpty()) {
            throw new IllegalArgumentException("The seats of order cannot be empty.");
        }
        OrderTotal orderTotal = pricingService.calculateTotal(conferenceId, seats);
        applyEvent(new OrderPlaced(conferenceId, orderTotal, new Date(), IdGenerator.id()));
    }

    public void assignRegistrant(String firstName, String lastName, String email) {
        applyEvent(new OrderRegistrantAssigned(conferenceId, new Registrant(firstName, lastName, email)));
    }

    public void confirmReservation(boolean isReservationSuccess) {
        if (status != OrderStatus.Placed) {
            throw new InvalidOperationException("Invalid order status:" + status);
        }
        if (isReservationSuccess) {
            applyEvent(new OrderReservationConfirmed(conferenceId, OrderStatus.ReservationSuccess));
        } else {
            applyEvent(new OrderReservationConfirmed(conferenceId, OrderStatus.ReservationFailed));
        }
    }

    public void confirmPayment(boolean isPaymentSuccess) {
        if (status != OrderStatus.ReservationSuccess) {
            throw new InvalidOperationException("Invalid order status:" + status);
        }
        if (isPaymentSuccess) {
            applyEvent(new OrderPaymentConfirmed(conferenceId, OrderStatus.PaymentSuccess));
        } else {
            applyEvent(new OrderPaymentConfirmed(conferenceId, OrderStatus.PaymentRejected));
        }
    }

    public void markAsSuccess() {
        if (status != OrderStatus.PaymentSuccess) {
            throw new InvalidOperationException("Invalid order status:" + status);
        }
        applyEvent(new OrderSuccessed(conferenceId));
    }

    public void markAsExpire() {
        if (status == OrderStatus.ReservationSuccess) {
            applyEvent(new OrderExpired(conferenceId));
        }
    }

    public void close() {
        if (status != OrderStatus.ReservationSuccess && status != OrderStatus.PaymentRejected) {
            throw new InvalidOperationException("Invalid order status:" + status);
        }
        applyEvent(new OrderClosed(conferenceId));
    }

    public OrderSeatAssignments createSeatAssignments() {
        if (status != OrderStatus.Success) {
            throw new InvalidOperationException("Cannot create seat assignments for an order that isn't success yet.");
        }
        return new OrderSeatAssignments(id, total.getOrderLines());
    }

    private void handle(OrderPlaced evnt) {
        id = evnt.getAggregateRootId();
        conferenceId = evnt.getConferenceId();
        total = evnt.getOrderTotal();
        accessCode = evnt.getAccessCode();
        status = OrderStatus.Placed;
    }

    private void handle(OrderRegistrantAssigned evnt) {
        registrant = evnt.getRegistrant();
    }

    private void handle(OrderReservationConfirmed evnt) {
        status = evnt.getOrderStatus();
    }

    private void handle(OrderPaymentConfirmed evnt) {
        status = evnt.getOrderStatus();
    }

    private void handle(OrderSuccessed evnt) {
        status = OrderStatus.Success;
    }

    private void handle(OrderExpired evnt) {
        status = OrderStatus.Expired;
    }

    private void handle(OrderClosed evnt) {
        status = OrderStatus.Closed;
    }
}
