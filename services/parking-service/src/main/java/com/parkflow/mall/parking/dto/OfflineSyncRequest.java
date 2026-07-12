package com.parkflow.mall.parking.dto;

import java.util.List;

public record OfflineSyncRequest(String deviceId, List<OfflineSyncEventRequest> events) {
}
