package ru.twentyoneh.api.error;

import io.javalin.Javalin;

import java.util.Map;

public final class ApiErrorHandler {
    public static void install(Javalin app) {
        app.exception(BadRequest.class, (e, ctx) -> {
            ctx.status(400).json(Map.of("error", e.getMessage()));
        });
        app.exception(NotFound.class, (e, ctx) -> {
            ctx.status(404).json(Map.of("error", e.getMessage()));
        });
        app.exception(PayloadTooLargeException.class, (e, ctx) -> {
            ctx.status(413).json(Map.of("error", e.getMessage()));
        });
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(Map.of("error", "internal_error"));
        });
    }
}
