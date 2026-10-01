package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final int PORT = 8080;
    private static final String BASE = "http://localhost:" + PORT;
    private static final int MAX_YEAR = Year.now().getValue() + 1;
    private static final int OVER_MAX_YEAR = MAX_YEAR + 1;
    private static final int MIN_YEAR = 1888;
    private static final int UNDER_MIN_YEAR = MIN_YEAR - 1;
    private static final String TITLE_IS_LONGER_THAN_100_CHARACTERS = "A".repeat(101);
    private static final int CODE_200 = 200;
    private static final int CODE_201 = 201;
    private static final int CODE_204 = 204;
    private static final int CODE_400 = 400;
    private static final int CODE_404 = 404;
    private static final int CODE_405 = 405;
    private static final int CODE_415 = 415;
    private static final int CODE_422 = 422;
    private MoviesServer server;
    private HttpClient client;
    private MoviesStore mStore;

    @BeforeEach
    void beforeEach() {
        mStore = new MoviesStore();
        server = new MoviesServer(mStore, PORT);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    @AfterEach
    void afterEach() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        List<Movie> movies = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(CODE_200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertTrue(movies.isEmpty(), "Список фильмов должен быть пустым");
    }

    @Test
    void postMovies_withValidMovie_createsMovie() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        Movie responseMovie = new Gson().fromJson(resp.body(), Movie.class);

        assertEquals(CODE_201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue);

        assertEquals(1, responseMovie.getId(), "Должно вернуться id добавленного фильма");
        assertEquals("Heroes", responseMovie.getTitle(), "Должно вернуться title добавленного фильма");
        assertEquals(1980, responseMovie.getYear(), "Должен вернуться Year добавленного фильма");

    }

    @Test
    void getMovies_whenMovieExists_returnsMovie() throws Exception {
        mStore.addMovie(new Movie("Мать дракона", 2026));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        List<Movie> movies = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(CODE_200, resp.statusCode(), "Должен вернуться код 200");
        assertEquals(1, movies.size(), "Должна вернуться длина списка 1");
        Movie mov = movies.getFirst();
        assertEquals("Мать дракона", mov.getTitle());
        assertEquals(2026, mov.getYear());
        assertEquals(1, mov.getId());
    }

    @Test
    void postMovies_withEmptyTitle_returns422() throws Exception {
        String json = "{\"title\":\"\",\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode(), "Должен вернуться код 422");
        assertEquals("Ошибка валидации", errorResponse.getError(), "Должен вернуть название ошибки");
        assertEquals("Название не должно быть пустым", errorResponse.getDetails().getFirst());
    }

    @Test
    void postMovies_withTooEarlyYear_returns422() throws Exception {
        String json = "{\"title\":\"Крысиные бега\",\"year\":" + UNDER_MIN_YEAR + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode(), "Должен вернуть код 422");
        assertEquals("Ошибка валидации", errorResponse.getError(), "Должен вернуть название ошибки");
        assertEquals("Год должен быть между " + MIN_YEAR + " и " + MAX_YEAR, errorResponse
                .getDetails().getFirst());
    }

    @Test
    void postMovies_withTooLateYear_returns422() throws Exception {
        String json = "{\"title\":\"Крысиные бега\",\"year\":" + OVER_MAX_YEAR + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode(), "Должен вернуть код 422");
        assertEquals("Ошибка валидации", errorResponse.getError(), "Должен вернуть название ошибки");
        assertEquals("Год должен быть между " + MIN_YEAR + " и " + MAX_YEAR, errorResponse
                .getDetails().getFirst());
    }

    @Test
    void postMovies_withTitleLongerThan100Characters_returns422() throws Exception {
        String json = "{\"title\": \"" + TITLE_IS_LONGER_THAN_100_CHARACTERS + "\",\"year\":" + MAX_YEAR + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode());
        assertEquals("Ошибка валидации", errorResponse.getError());
        assertEquals("Название не должно быть длиннее 100 символов.", errorResponse.getDetails().getFirst());
    }

    @Test
    void postMovies_withUnsupportedContentType_returns415() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_415, resp.statusCode());
    }

    @Test
    void getMovieById_whenMovieExists_returnsMovie() throws Exception {
        mStore.addMovie(new Movie("Король и Шут", 2021));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie movie = new Gson().fromJson(resp.body(), Movie.class);

        assertEquals(CODE_200, resp.statusCode());
        assertEquals("Король и Шут", movie.getTitle());
        assertEquals(2021, movie.getYear());
        assertEquals(1, movie.getId(), "Должно вернуться id первого фильма");
    }

    @Test
    void getMovieById_whenMovieNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/999")).GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_404, resp.statusCode());
        assertEquals("Фильм не найден", message);
    }

    @Test
    void getMovieById_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/abc")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_400, resp.statusCode());
        assertEquals("Некорректный ID", message);
    }

    @Test
    void deleteMovieById_whenMovieExists_returns204() throws Exception {
        mStore.addMovie(new Movie("Золотая рыбка", 2010));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_204, resp.statusCode());
        assertTrue(mStore.getAllMovies().isEmpty());
    }

    @Test
    void deleteMovieById_whenMovieNotFound_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/3")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_404, resp.statusCode());
        assertEquals("Фильм не найден", message);
    }

    @Test
    void deleteMovieById_whenIdIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/abc")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_400, resp.statusCode());
        assertEquals("Некорректный ID", message);
    }

    @Test
    void getMovieByYear_returns200() throws Exception {
        mStore.addMovie(new Movie("Кто ходит в гости по утрам", 2022));
        mStore.addMovie(new Movie("Колобок", 2026));
        mStore.addMovie(new Movie("Человек- паук", 2026));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2026")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        List<Movie> mov = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(CODE_200, resp.statusCode());
        assertEquals(2, mov.size(), "Должна вернуться длина списка фильмов за 2026 год");
        assertEquals(2026, mov.getFirst().getYear());
    }

    @Test
    void getMovies_whenYearIsNotNumber_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=abc")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_400, resp.statusCode());
        assertEquals("Некорректный параметр запроса — year", message);
    }

    @Test
    void getMovies_whenNoMoviesForYear_returnsEmptyList() throws Exception {
        mStore.addMovie(new Movie("Кто ходит в гости по утрам", 2022));
        mStore.addMovie(new Movie("Колобок", 2026));
        mStore.addMovie(new Movie("Человек- паук", 2026));
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2000")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        List<Movie> movie = new Gson().fromJson(resp.body(), new ListOfMoviesTypeToken());

        assertEquals(CODE_200, resp.statusCode());
        assertTrue(movie.isEmpty());
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .method("PUT", HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_405, resp.statusCode());
    }

    @Test
    void postMovies_withInvalidJson_returns400() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\": }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_400, resp.statusCode());
    }

    @Test
    void deleteMovies_withoutId_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_400, resp.statusCode());
    }

    @Test
    void getMovies_whenQueryParameterIsNotYear_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?name=2026")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_400, resp.statusCode());
        assertEquals("Некорректный параметр запроса — year", message);
    }

    @Test
    void getMovies_whenQueryParameterYearIsEmpty_returns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String message = new Gson().fromJson(resp.body(), String.class);

        assertEquals(CODE_400, resp.statusCode());
        assertEquals("Некорректный параметр запроса — year", message);
    }

    @Test
    void postMovies_withoutTitle_returns422() throws Exception {
        String json = "{\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode(), "Должен вернуться код 422");
        assertEquals("Ошибка валидации", errorResponse.getError(), "Должен вернуть название ошибки");
        assertEquals("Название не должно быть пустым", errorResponse.getDetails().getFirst());
    }

    @Test
    void postMovies_withBlankTitle_returns422() throws Exception {
        String json = "{\"title\":\"   \",\"year\":1980}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse errorResponse = new Gson().fromJson(resp.body(), ErrorResponse.class);

        assertEquals(CODE_422, resp.statusCode(), "Должен вернуться код 422");
        assertEquals("Ошибка валидации", errorResponse.getError(), "Должен вернуть название ошибки");
        assertEquals("Название не должно быть пустым", errorResponse.getDetails().getFirst());
    }

    @Test
    void postMovies_withNullJson_returns400() throws Exception {
        String json = "null";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_400, resp.statusCode());
    }

    @Test
    void postMovies_withInvalidContentType_returns415() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json123; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(CODE_415, resp.statusCode());
    }

    @Test
    void getMovies_withInvalidPath_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/3/test")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_404, resp.statusCode());
    }

    @Test
    void postMovies_withInvalidPath_returns404() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\":1980 }";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/123"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json; charset=UTF-8").build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_404, resp.statusCode());
    }

    @Test
    void deleteMovies_withInvalidPath_returns404() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1/test")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_404, resp.statusCode());
    }

    @Test
    void getMovies_withWrongBasePath_returns404() throws Exception {
        Movie movie = new Movie("Heroes", 1980);
        mStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/moviesSomething/1/")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_404, resp.statusCode());
    }

    @Test
    void deleteMovies_withWrongBasePath_returns404() throws Exception {
        Movie movie = new Movie("Heroes", 1980);
        mStore.addMovie(movie);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/moviesSomething/1/")).DELETE().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_404, resp.statusCode());
    }

    @Test
    void postMovies_withoutContentType_returns415() throws Exception {
        String json = "{\"title\":\"Heroes\",\"year\":1980}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(CODE_415, resp.statusCode());
    }

    @Test
    void postMovies_withMinYear_returns201() throws Exception {
        String json = "{\"title\":\"Movie\",\"year\":" + MIN_YEAR + "}";
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie movie = new Gson().fromJson(resp.body(), Movie.class);

        assertEquals(CODE_201, resp.statusCode());
        assertEquals(MIN_YEAR, movie.getYear());
    }

    @Test
    void postMovies_withTitleExactly100Characters_returns201() throws Exception {
        String title = "A".repeat(100);
        String json = "{\"title\":\"" + title + "\",\"year\":1980}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies")).header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Movie movie = new Gson().fromJson(resp.body(), Movie.class);

        assertEquals(CODE_201, resp.statusCode());
        assertEquals(100, movie.getTitle().length());
        assertEquals(title, movie.getTitle());
    }
}
