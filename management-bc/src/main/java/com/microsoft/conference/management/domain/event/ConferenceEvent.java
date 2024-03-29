package com.microsoft.conference.management.domain.event;

import com.microsoft.conference.management.domain.model.ConferenceInfo;
import org.enodeframework.eventing.AbstractDomainEventMessage;

public abstract class ConferenceEvent extends AbstractDomainEventMessage {
    private ConferenceInfo info;

    public ConferenceEvent() {
    }

    public ConferenceEvent(ConferenceInfo info) {
        this.info = info;
    }

    public ConferenceInfo getInfo() {
        return info;
    }

    public void setInfo(ConferenceInfo info) {
        this.info = info;
    }
}
