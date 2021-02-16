package org.example.metrics;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

/**
 * Represents the metadata of a published event.
 * At the moment, it is very simple.
 * It can be enhanced to have more fields as needed.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class EventMetadata implements Serializable {

    private EventType eventType;

    @JsonCreator
    public EventMetadata(@JsonProperty("eventType") final EventType eventType) {
        this.eventType = eventType;
    }
}
