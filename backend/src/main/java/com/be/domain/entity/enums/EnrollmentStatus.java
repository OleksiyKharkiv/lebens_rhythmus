package com.be.domain.entity.enums;

public enum EnrollmentStatus {
    PENDING,    // awaiting payment / confirmation
    CONFIRMED,  // confirmed (free or paid)
    CANCELLED,
    // LR-084 — auto-set by the 7-day TTL cleanup job when a PENDING (paid,
    // awaiting payment) enrollment's payment never arrived. Distinct from
    // CANCELLED (a deliberate user/admin action) so the two are never
    // conflated in reporting/dashboard display.
    EXPIRED
}