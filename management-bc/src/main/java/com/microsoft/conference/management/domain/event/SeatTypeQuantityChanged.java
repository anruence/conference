package com.microsoft.conference.management.domain.event;

import lombok.Getter;
import lombok.Setter;
import org.enodeframework.eventing.AbstractDomainEventMessage;

@Getter
@Setter
public class SeatTypeQuantityChanged extends AbstractDomainEventMessage {
    private String seatTypeId;
    private int quantity;
    private int availableQuantity;

    public SeatTypeQuantityChanged() {
    }

    public SeatTypeQuantityChanged(String seatTypeId, int quantity, int availableQuantity) {
        this.seatTypeId = seatTypeId;
        this.quantity = quantity;
        this.availableQuantity = availableQuantity;
    }
}
