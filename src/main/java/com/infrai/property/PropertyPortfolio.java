package com.infrai.property;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class PropertyPortfolio {
    public enum RequestState { OPEN, IN_PROGRESS, CLOSED }

    public record MaintenanceRequest(String id, RequestState state) {}
    public record TenantDocument(String id, boolean verified) {}
    public record InspectionReminder(String id, LocalDate dueOn, boolean completed) {}

    public record DashboardSnapshot(
            int openMaintenance,
            int unverifiedDocuments,
            int inspectionsDueWithinSevenDays,
            String attention) {
        public Map<String, Object> asMap() {
            return Map.of(
                    "openMaintenance", openMaintenance,
                    "unverifiedDocuments", unverifiedDocuments,
                    "inspectionsDueWithinSevenDays", inspectionsDueWithinSevenDays,
                    "attention", attention);
        }
    }

    private PropertyPortfolio() {}

    public static DashboardSnapshot assess(
            List<MaintenanceRequest> requests,
            List<TenantDocument> documents,
            List<InspectionReminder> reminders,
            LocalDate today) {
        int open = (int) requests.stream().filter(r -> r.state() != RequestState.CLOSED).count();
        int unverified = (int) documents.stream().filter(d -> !d.verified()).count();
        LocalDate horizon = today.plusDays(7);
        int dueSoon = (int) reminders.stream()
                .filter(r -> !r.completed() && !r.dueOn().isBefore(today) && !r.dueOn().isAfter(horizon))
                .count();
        String attention = open >= 3 || unverified > 0 || dueSoon > 0 ? "REVIEW" : "CLEAR";
        return new DashboardSnapshot(open, unverified, dueSoon, attention);
    }
}
