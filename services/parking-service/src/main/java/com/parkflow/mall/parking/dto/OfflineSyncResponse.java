package com.parkflow.mall.parking.dto;

import java.util.List;

public record OfflineSyncResponse(String syncBatchId, String deviceId, List<OfflineSyncResult> results) {
}
