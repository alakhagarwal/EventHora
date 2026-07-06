package com.eventHora.backend.Enum;

public enum EventStatus {
    DRAFT,        // Created but not visible to members yet
    PUBLISHED,    // Live — members can see and register
    CANCELLED,    // Event called off
    COMPLETED     // Event has ended
}
