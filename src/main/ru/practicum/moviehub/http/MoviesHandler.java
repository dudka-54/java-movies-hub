package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.HttpStatusCode;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    MoviesStore moviesStore;
    Gson gson = new Gson();

    MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            handleGetMethod(ex);
        } else if (method.equalsIgnoreCase("POST")) {
            handlePostMethod(ex);
        } else if (method.equalsIgnoreCase("DELETE")) {
            handleDeleteMethod(ex);
        }
    }

    public void handleGetMethod(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        if (path.equals("/movies")) {
            if (query != null) {
                try {
                    int year = getYearFromExchange(ex);
                    if (moviesStore.getMoviesByYears(year).isEmpty()) {
                        sendJson(ex, HttpStatusCode.NOT_FOUND, new ErrorResponse("Not Found", "Фильмы не найдены"));
                    } else {
                        HashMap<Integer, Movie> filtered = moviesStore.getMoviesByYears(year);
                        List<Movie> filteredList = new ArrayList<>(filtered.values());
                        sendJson(ex, HttpStatusCode.OK, filteredList);
                    }
                } catch (NumberFormatException e) {
                    sendJson(ex, HttpStatusCode.BAD_REQUEST, new ErrorResponse("Bad Request", "Некорректный параметр запроса — 'year'"));
                }
            } else {
                if (moviesStore.getMovies().isEmpty()) {
                    sendJson(ex, HttpStatusCode.OK, List.of());
                } else {
                    List<Movie> moviesList = new ArrayList<>(moviesStore.getMovies().values());
                    sendJson(ex, HttpStatusCode.OK, moviesList);
                }
            }
        } else if (path.startsWith("/movies/")) {
            if (query != null) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST, new ErrorResponse("Bad Request",
                        "Query параметры недопустимы для этого URL"));
                return;
            }
            try {
                int id = getIdFromExchange(ex);
                if (moviesStore.getMovies().get(id) == null) {
                    sendJson(ex, HttpStatusCode.NOT_FOUND, new ErrorResponse("Not Found", "Фильм не найден"));
                } else {
                    sendJson(ex, HttpStatusCode.OK, moviesStore.getMovies().get(id));
                }
            } catch (NumberFormatException e) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST, new ErrorResponse("Bad Request", "Некорректный ID"));
            }
        }
    }

    public void handlePostMethod(HttpExchange ex) throws IOException {
        if (!"/movies".equals(ex.getRequestURI().getPath())) {
            sendJson(ex, HttpStatusCode.NOT_FOUND, new ErrorResponse("Not Found", "Объект не найден"));
            return;
        }

        String contentType = ex.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null || !contentType.contains("application/json")) {
            sendJson(ex, HttpStatusCode.UNSUPPORTED_MEDIA_TYPE,
                    new ErrorResponse("Unsupported Media Type", "Запрос с неправильным значением заголовка Content-Type"));
            return;
        }
        String body;

        try (InputStream inputStream = ex.getRequestBody()) {
            body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (body.trim().isEmpty()) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST,
                        new ErrorResponse("Bad Request", "Тело запроса пустое"));
                return;
            }

            JsonObject jsonObject;
            try {
                jsonObject = gson.fromJson(body, JsonObject.class);
            } catch (com.google.gson.JsonSyntaxException e) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST,
                        new ErrorResponse("Bad Request", "Некорректный JSON формат"));
                return;
            }

            if (!jsonObject.has("name") || !jsonObject.has("year")) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST,
                        new ErrorResponse("Bad Request", "Отсутствуют обязательные поля: name, year"));
                return;
            }
            String name = jsonObject.get("name").getAsString();
            int year = jsonObject.get("year").getAsInt();
            Movie newMovie = moviesStore.addMovie(name, year);
            ex.getResponseHeaders().set(
                    "Location", "/movies/" + newMovie.getId()
            );
            sendJson(ex, HttpStatusCode.CREATED, newMovie);
        } catch (IllegalArgumentException e) {
            sendJson(ex, HttpStatusCode.UNPROCESSABLE_ENTITY, new ErrorResponse("Unprocessable Entity", "Ошибка валидации \n" + e.getMessage()));
        }
    }

    public void handleDeleteMethod(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (path.startsWith("/movies/")) {
            try {
                int id = getIdFromExchange(ex);
                if (!moviesStore.containsMovie(id)) {
                    sendJson(ex, HttpStatusCode.NOT_FOUND, new ErrorResponse("Not Found", "Фильм не найден"));
                    return;
                }
                moviesStore.deleteMovie(id);
                sendNoContent(ex);
                ex.close();
            } catch (NumberFormatException e) {
                sendJson(ex, HttpStatusCode.BAD_REQUEST, new ErrorResponse("Bad Request", "Некорректный ID"));
            }
        } else {
            sendJson(ex, HttpStatusCode.NOT_FOUND, new ErrorResponse("Not Found", "Объект не найден"));
        }
    }
}