package com.infrai.property;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public final class PropertyMetricsPublisher {
    private final InfraiClient infrai;

    public PropertyMetricsPublisher(InfraiClient infrai) { this.infrai = infrai; }

    public PropertyPortfolio.DashboardSnapshot publish(
            List<PropertyPortfolio.MaintenanceRequest> requests,
            List<PropertyPortfolio.TenantDocument> documents,
            List<PropertyPortfolio.InspectionReminder> reminders,
            LocalDate today) throws IOException, InterruptedException {
        PropertyPortfolio.DashboardSnapshot snapshot = PropertyPortfolio.assess(requests, documents, reminders, today);
        Object metrics = infrai.queryMetrics();
        infrai.publish(metrics, snapshot);
        return snapshot;
    }
}
