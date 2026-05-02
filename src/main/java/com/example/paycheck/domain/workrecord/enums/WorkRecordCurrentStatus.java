package com.example.paycheck.domain.workrecord.enums;

public enum WorkRecordCurrentStatus {
    IN_PROGRESS(0),
    UPCOMING(1),
    COMPLETED(2);

    private final int sortOrder;

    WorkRecordCurrentStatus(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
