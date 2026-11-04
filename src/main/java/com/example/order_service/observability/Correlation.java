package com.example.order_service.observability;

import java.util.UUID;
import java.util.regex.Pattern;

public final class Correlation {
    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private Correlation(){

    }

    public static String validOrNew(String candidate){
        return candidate !=null && VALID.matcher(candidate).matches() ? candidate : UUID.randomUUID().toString();
    }
}
