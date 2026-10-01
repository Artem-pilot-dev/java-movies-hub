package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private static final int MIN_YEAR = 1888;
    private static final int MAX_TITLE_LENGTH = 100;
    private final MoviesStore mStore;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore mStore) {
        this.mStore = mStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        switch (method) {
            case "GET":
                handleGet(ex);
                break;
            case "POST":
                handlePost(ex);
                break;
            case "DELETE":
                handleDelete(ex);
                break;
            default:
                ex.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, -1);
                break;
        }
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] partOfPath = path.split("/");
        String query = ex.getRequestURI().getQuery();

        if (path.equals("/movies") && query != null) {
            String[] partsOfQuery = query.split("=");
            if (partsOfQuery.length == 2 && partsOfQuery[0].equals("year")) {
                try {
                    int year = Integer.parseInt(partsOfQuery[1]);
                    List<Movie> mov = mStore.getAllMovies();
                    List<Movie> moviesForResponse = mov.stream()
                            .filter(movie -> movie.getYear() == year).toList();
                    String listOfMovies = gson.toJson(moviesForResponse);
                    sendJson(ex, HTTP_OK, listOfMovies);
                    return;
                } catch (NumberFormatException e) {
                    sendMessage(ex, HTTP_BAD_REQUEST, "Некорректный параметр запроса — year");
                    return;
                }
            } else {
                sendMessage(ex, HTTP_BAD_REQUEST, "Некорректный параметр запроса — year");
                return;
            }
        } else {
            if (path.equals("/movies")) {
                List<Movie> listOfMovie = mStore.getAllMovies();
                String gsonListOfMovies = gson.toJson(listOfMovie);
                sendJson(ex, HTTP_OK, gsonListOfMovies);
                return;
            } else if (partOfPath.length == 3 && partOfPath[1].equals("movies")) {
                try {
                    int pathOfId = Integer.parseInt(partOfPath[2]);
                    Optional<Movie> movieOptional = mStore.findMovieById(pathOfId);
                    if (movieOptional.isPresent()) {
                        Movie movie = movieOptional.get();
                        String json = gson.toJson(movie);
                        sendJson(ex, HTTP_OK, json);
                        return;
                    }
                    if (movieOptional.isEmpty()) {
                        sendMessage(ex, HTTP_NOT_FOUND, "Фильм не найден");
                        return;
                    }
                } catch (NumberFormatException exp) {
                    sendMessage(ex, HTTP_BAD_REQUEST, "Некорректный ID");
                    return;
                }
            }
        }
        sendMessage(ex, HTTP_NOT_FOUND, "Фильм не найден");
    }

    private void handlePost(HttpExchange ex) throws IOException {
        int maxYear = Year.now().getValue() + 1;
        String path = ex.getRequestURI().getPath();
        if (!path.equals("/movies")) {
            ex.sendResponseHeaders(HTTP_NOT_FOUND, -1);
            return;
        }
        String headerContentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (headerContentType != null) {
            String[] partsOfHeaderContentType = headerContentType.split(";");
            if (!partsOfHeaderContentType[0].trim().equals("application/json")) {
                sendJson(ex, HTTP_UNSUPPORTED_MEDIA_TYPE, "");
                return;
            }
        } else {
            sendJson(ex, HTTP_UNSUPPORTED_MEDIA_TYPE, "");
            return;
        }
        String request = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie movie;
        try {
            movie = gson.fromJson(request, Movie.class);

        } catch (JsonSyntaxException e) {
            ex.sendResponseHeaders(HTTP_BAD_REQUEST, -1);
            return;
        }
        if (movie == null) {
            ex.sendResponseHeaders(HTTP_BAD_REQUEST, -1);
            return;
        }
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            sendValidationError(ex, "Название не должно быть пустым");
            return;
        }
        if (movie.getYear() < MIN_YEAR || movie.getYear() > maxYear) {
            sendValidationError(ex, "Год должен быть между " + MIN_YEAR + " и " + maxYear);
            return;
        }
        if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            sendValidationError(ex, "Название не должно быть длиннее " + MAX_TITLE_LENGTH + " символов.");
            return;
        }
        mStore.addMovie(movie);
        String gsonMovie = gson.toJson(movie);
        sendJson(ex, HTTP_CREATED, gsonMovie);
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String pathForDelete = ex.getRequestURI().getPath();
        String[] partOfPathForDelete = pathForDelete.split("/");
        if (partOfPathForDelete.length == 3 && partOfPathForDelete[1].equals("movies")) {
            try {
                int idForDelete = Integer.parseInt(partOfPathForDelete[2]);
                Optional<Movie> movieForDelete = mStore.findMovieById(idForDelete);
                if (movieForDelete.isEmpty()) {
                    sendMessage(ex, HTTP_NOT_FOUND, "Фильм не найден");
                    return;
                } else {
                    mStore.removeMovieById(idForDelete);
                    sendNoContent(ex);
                    return;
                }
            } catch (NumberFormatException e) {
                sendMessage(ex, HTTP_BAD_REQUEST, "Некорректный ID");
                return;
            }
        } else if (partOfPathForDelete.length < 3) {
            ex.sendResponseHeaders(HTTP_BAD_REQUEST, -1);
            return;
        } else {
            ex.sendResponseHeaders(HTTP_NOT_FOUND, -1);
            return;
        }
    }

    private void sendMessage(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, gson.toJson(message));
    }

    private void sendValidationError(HttpExchange ex, String detail) throws IOException {
        ErrorResponse errorResponse =
                new ErrorResponse("Ошибка валидации", List.of(detail));

        sendJson(ex, HTTP_UNPROCESSABLE_ENTITY, gson.toJson(errorResponse));
    }
}
