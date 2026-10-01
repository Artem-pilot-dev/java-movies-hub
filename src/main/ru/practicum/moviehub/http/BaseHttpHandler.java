package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final int HTTP_OK = 200;
    protected static final int HTTP_CREATED = 201;
    protected static final int HTTP_BAD_REQUEST = 400;
    protected static final int HTTP_NO_CONTENT = 204;
    protected static final int HTTP_NOT_FOUND = 404;
    protected static final int HTTP_METHOD_NOT_ALLOWED = 405;
    protected static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
    protected static final int HTTP_UNPROCESSABLE_ENTITY = 422;

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {

        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, response.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(HTTP_NO_CONTENT, -1);
    }
}