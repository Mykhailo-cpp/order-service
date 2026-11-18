package com.example.order_service.config;

public final class ApiMessages {
    public static final String HTTP_200 = "OK";
    public static final String HTTP_202 = "Order accepted for processing";
    public static final String HTTP_400 = "Invalid or malformed request";
    public static final String HTTP_404 = "Order not found";
    public static final String HTTP_500 = "Unexpected server error";

    private ApiMessages() {}
}