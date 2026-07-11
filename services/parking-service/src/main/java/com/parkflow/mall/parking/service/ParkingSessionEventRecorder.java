package com.parkflow.mall.parking.service;

import com.parkflow.mall.parking.model.ParkingSessionEvent;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

@Component
public class ParkingSessionEventRecorder {
    private final ConcurrentLinkedQueue<ParkingSessionEvent> events = new ConcurrentLinkedQueue<>();

    public void record(ParkingSessionEvent event) {
        events.add(event);
    }

    List<ParkingSessionEvent> snapshot() {
        return List.copyOf(events);
    }
}
