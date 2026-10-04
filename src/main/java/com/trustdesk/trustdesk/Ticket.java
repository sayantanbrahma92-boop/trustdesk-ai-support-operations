package com.trustdesk.trustdesk;

public record Ticket(String id, String customerName, String issue, String status) {
}