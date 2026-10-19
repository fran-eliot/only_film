package com.demo.dto;

/** Aggregated demonstration sales; no personal or payment-provider data. */
public record DashboardSalesSummary(long paidTickets, double ticketRevenue, double snackRevenue) {
    public double totalRevenue() {
        return ticketRevenue + snackRevenue;
    }
}
