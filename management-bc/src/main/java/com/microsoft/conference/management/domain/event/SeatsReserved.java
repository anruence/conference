package com.microsoft.conference.management.domain.event;

import com.microsoft.conference.management.domain.model.ReservationItem;
import com.microsoft.conference.management.domain.model.SeatAvailableQuantity;
import lombok.Getter;
import lombok.Setter;
import org.enodeframework.eventing.AbstractDomainEventMessage;

import java.util.List;

@Getter
@Setter
public class SeatsReserved extends AbstractDomainEventMessage {
    private String reservationId;
    private List<ReservationItem> reservationItems;
    private List<SeatAvailableQuantity> seatAvailableQuantities;

    public SeatsReserved() {
    }

    public SeatsReserved(String reservationId, List<ReservationItem> reservationItems, List<SeatAvailableQuantity> seatAvailableQuantities) {
        this.reservationId = reservationId;
        this.reservationItems = reservationItems;
        this.seatAvailableQuantities = seatAvailableQuantities;
    }
}