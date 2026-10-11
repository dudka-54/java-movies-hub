package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;

    @BeforeAll
    static void beforeAll() throws Exception {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        Thread.sleep(100);

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clearMap();
    }


    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.contains("application/json"),
                "Content-Type должен быть application/json");

        String body = resp.body().trim();
        assertEquals("[]", body, "Для пустой коллекции должен возвращаться []");
    }

    @Test
    void getMovies_whenHasMovies_returnsMoviesList() throws Exception {
        moviesStore.addMovie("Inception", 2010);
        moviesStore.addMovie("The Matrix", 1999);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("Inception"));
        assertTrue(body.contains("The Matrix"));
        assertTrue(body.contains("2010"));
        assertTrue(body.contains("1999"));
    }


    @Test
    void postMovies_withValidData_createsMovie() throws Exception {
        String json = "{\"name\":\"Interstellar\",\"year\":2014}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "Должен вернуть 201 Created");

        // Проверяем Location header
        String location = resp.headers().firstValue("Location").orElse("");
        assertTrue(location.matches("/movies/\\d+"),
                "Location должен содержать URL созданного фильма");

        // Проверяем тело ответа
        String body = resp.body();
        assertTrue(body.contains("Interstellar"));
        assertTrue(body.contains("2014"));
        assertTrue(body.contains("\"id\":"));
    }

    @Test
    void postMovies_withEmptyTitle_returnsError() throws Exception {
        String json = "{\"name\":\"\",\"year\":2023}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "Должен вернуть 422 Unprocessable Entity");
        assertTrue(resp.body().contains("Ошибка валидации"));
    }

    @Test
    void postMovies_withTooLongTitle_returnsError() throws Exception {
        String longTitle = "A".repeat(101);
        String json = String.format("{\"name\":\"%s\",\"year\":2023}", longTitle);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("Ошибка валидации"));
    }

    @Test
    void postMovies_withInvalidYear_returnsError() throws Exception {
        String json = "{\"name\":\"Test\",\"year\":1800}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
        assertTrue(resp.body().contains("Ошибка валидации"));
    }

    @Test
    void postMovies_withWrongContentType_returnsError() throws Exception {
        String json = "{\"name\":\"Test\",\"year\":2023}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "text/plain")  // Неправильный Content-Type
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "Должен вернуть 415 Unsupported Media Type");
        assertTrue(resp.body().contains("Unsupported Media Type"));
    }

    @Test
    void postMovies_withInvalidJson_returnsError() throws Exception {
        String invalidJson = "name: Test, year: 2023";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
    }

    @Test
    void postMovies_withEmptyBody_returnsError() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Тело запроса пустое"));
    }


    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        // Создаем фильм
        moviesStore.addMovie("Inception", 2010);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertTrue(resp.body().contains("Inception"));
        assertTrue(resp.body().contains("2010"));
    }

    @Test
    void getMovieById_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void getMovieById_withInvalidId_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void getMovieById_withQueryParams_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1?year=2023"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Query параметры недопустимы"));
    }


    @Test
    void deleteMovie_whenExists_returnsNoContent() throws Exception {
        // Создаем фильм
        moviesStore.addMovie("Test Movie", 2023);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "Должен вернуть 204 No Content");

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, getResp.statusCode());
    }

    @Test
    void deleteMovie_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovie_withInvalidId_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный ID"));
    }


    @Test
    void getMoviesByYear_whenHasMovies_returnsMovies() throws Exception {
        // Добавляем фильмы разных годов
        moviesStore.addMovie("Movie 2022", 2022);
        moviesStore.addMovie("Movie 2023", 2023);
        moviesStore.addMovie("Another 2023", 2023);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2023"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("Movie 2023"));
        assertTrue(body.contains("Another 2023"));
        assertFalse(body.contains("Movie 2022"));
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsNotFound() throws Exception {
        moviesStore.addMovie("Movie 2022", 2022);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2023"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode());
        assertTrue(resp.body().contains("Фильмы не найдены"));
    }

    @Test
    void getMoviesByYear_withInvalidYear_returnsBadRequest() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("Некорректный параметр запроса"));
    }

    @Test
    void getMoviesByYear_withMultipleParams_handlesCorrectly() throws Exception {
        moviesStore.addMovie("Test Movie", 2023);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2023&other=param"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));


    }

    @Test
    void postAndGetMovie_workTogether() throws Exception {
        String json = "{\"name\":\"The Shawshank Redemption\",\"year\":1994}";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, postResp.statusCode());

        String location = postResp.headers().firstValue("Location").orElse("");
        assertFalse(location.isEmpty());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + location))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("The Shawshank Redemption"));
        assertTrue(getResp.body().contains("1994"));
    }

    @Test
    void postDeleteGet_workTogether() throws Exception {
        String json = "{\"name\":\"Temporary Movie\",\"year\":2023}";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> postResp = client.send(postReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());

        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1"))
                .DELETE()
                .build();

        HttpResponse<String> deleteResp = client.send(deleteReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(204, deleteResp.statusCode());

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/1"))
                .GET()
                .build();

        HttpResponse<String> getResp = client.send(getReq,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(404, getResp.statusCode());
    }
}