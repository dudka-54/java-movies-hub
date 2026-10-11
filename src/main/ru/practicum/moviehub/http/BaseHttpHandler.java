package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.HttpStatusCode;

import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";
    protected static final Gson gson = new Gson();

    protected void sendJson(HttpExchange ex, int status, Object obj) throws IOException {
        String json;
        if (obj instanceof String) {
            json = (String) obj;
        } else {
            json = gson.toJson(obj);
        }

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, 0);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(HttpStatusCode.NO_CONTENT, -1);
    }

    protected int getIdFromExchange(HttpExchange ex) {
        URI uri = ex.getRequestURI();
        String path = uri.getPath();
        return Integer.parseInt(path.split("/")[2]);
    }

    protected int getYearFromExchange(HttpExchange ex) {
        URI uri = ex.getRequestURI();
        String query = uri.getQuery();
        return Integer.parseInt(query.split("=")[1]);
    }
}

