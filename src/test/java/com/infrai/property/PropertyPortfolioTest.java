package com.infrai.property;

import java.time.LocalDate;
import java.util.List;

public final class PropertyPortfolioTest {
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 9, 26);
        var result = PropertyPortfolio.assess(
                List.of(
                        new PropertyPortfolio.MaintenanceRequest("m1", PropertyPortfolio.RequestState.OPEN),
                        new PropertyPortfolio.MaintenanceRequest("m2", PropertyPortfolio.RequestState.CLOSED)),
                List.of(new PropertyPortfolio.TenantDocument("d1", false)),
                List.of(
                        new PropertyPortfolio.InspectionReminder("i1", today.plusDays(7), false),
                        new PropertyPortfolio.InspectionReminder("i2", today.plusDays(8), false),
                        new PropertyPortfolio.InspectionReminder("i3", today.plusDays(2), true)),
                today);

        check(result.openMaintenance() == 1, "one maintenance request remains active");
        check(result.unverifiedDocuments() == 1, "one document needs verification");
        check(result.inspectionsDueWithinSevenDays() == 1, "only the incomplete seven-day inspection counts");
        check(result.attention().equals("REVIEW"), "compliance work requires review");
        System.out.println("PropertyPortfolioTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
