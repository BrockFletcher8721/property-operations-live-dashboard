package com.infrai.property;

import java.time.LocalDate;
import java.util.List;

public final class PropertyDashboardRunner {
    private PropertyDashboardRunner() {}

    public static void main(String[] args) throws Exception {
        LocalDate today = LocalDate.now();
        PropertyMetricsPublisher publisher = new PropertyMetricsPublisher(
                new InfraiClient(ServiceConfig.fromEnvironment()));

        var snapshot = publisher.publish(
                List.of(
                        new PropertyPortfolio.MaintenanceRequest("mx-101", PropertyPortfolio.RequestState.OPEN),
                        new PropertyPortfolio.MaintenanceRequest("mx-102", PropertyPortfolio.RequestState.IN_PROGRESS),
                        new PropertyPortfolio.MaintenanceRequest("mx-103", PropertyPortfolio.RequestState.CLOSED)),
                List.of(
                        new PropertyPortfolio.TenantDocument("lease-44", true),
                        new PropertyPortfolio.TenantDocument("insurance-44", false)),
                List.of(new PropertyPortfolio.InspectionReminder("inspect-9", today.plusDays(3), false)),
                today);

        System.out.println(Json.stringify(snapshot.asMap()));
    }
}
