package com.microsoft.conference.management.domain.event;

import com.microsoft.conference.management.domain.model.SeatQuantity;
import org.enodeframework.eventing.AbstractDomainEventMessage;

import java.util.List;

public class SeatsReservationCommitted extends AbstractDomainEventMessage {
    private String reservationId;
    private List<SeatQuantity> seatQuantities;

    public SeatsReservationCommitted() {
    }

    public SeatsReservationCommitted(String reservationId, List<SeatQuantity> seatQuantities) {
        this.reservationId = reservationId;
        this.seatQuantities = seatQuantities;
    }

    public String getReservationId() {
        return this.reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public List<SeatQuantity> getSeatQuantities() {
        return this.seatQuantities;
    }

    public void setSeatQuantities(List<SeatQuantity> seatQuantities) {
        this.seatQuantities = seatQuantities;
    }
}