package com.example.order_service.config;

import com.example.order_service.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.lang.annotation.*;

public final class ApiErrors {
    private ApiErrors() {}

    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "400", description = ApiMessages.HTTP_400,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public @interface BadRequest {}

    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "404", description = ApiMessages.HTTP_404,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public @interface NotFound {}

    @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "500", description = ApiMessages.HTTP_500,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public @interface ServerError {}
}