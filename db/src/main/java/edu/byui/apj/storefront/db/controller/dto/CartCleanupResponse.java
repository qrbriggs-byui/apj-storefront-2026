package edu.byui.apj.storefront.db.controller.dto;

import java.time.Instant;

/**
 * Summary returned after running cart cleanup. Used by the work module for reporting.
 */
public record CartCleanupResponse(
        int removedCount,
        int maxAgeMinutes,
        Instant cutoffTime,
        Instant runAt
) {}
