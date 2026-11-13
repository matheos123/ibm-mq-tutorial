package com.example.consumer;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AuditService {
    private static final int MAX_HISTORY = 100;
    private final List<AuditRecord> records = new CopyOnWriteArrayList<>();
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger dlqCount = new AtomicInteger(0);

    public void recordSuccess(AppMessage message, int attempts) {
        AuditRecord record = AuditRecord.success(message, attempts);
        addRecord(record);
        successCount.incrementAndGet();
    }

    public void recordDeadLetter(AppMessage message, String error, int attempts) {
        AuditRecord record = AuditRecord.deadLetter(message, error, attempts);
        addRecord(record);
        dlqCount.incrementAndGet();
    }

    private void addRecord(AuditRecord record) {
        records.add(0, record); // newest first
        if (records.size() > MAX_HISTORY) {
            records.remove(records.size() - 1);
        }
    }

    public List<AuditRecord> getRecords(String status, String sender) {
        return records.stream()
            .filter(r -> status == null || status.isBlank() || r.status().equalsIgnoreCase(status))
            .filter(r -> sender == null || sender.isBlank() || r.sender().equalsIgnoreCase(sender))
            .toList();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReceived", successCount.get() + dlqCount.get());
        stats.put("processedSuccessfully", successCount.get());
        stats.put("deadLetterCount", dlqCount.get());
        stats.put("historySize", records.size());
        return stats;
    }

    public void clear() {
        records.clear();
        successCount.set(0);
        dlqCount.set(0);
    }
}

