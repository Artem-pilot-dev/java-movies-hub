package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {
    private final Map<Integer, Movie> moviesStore;
    private int nextId = 1;

    public MoviesStore() {
        moviesStore = new HashMap<>();
    }

    public void addMovie(Movie movie) {
        movie.setId(nextId);
        nextId++;
        moviesStore.put(movie.getId(), movie);
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(moviesStore.values());

    }

    public Optional<Movie> findMovieById(int id) {
        return Optional.ofNullable(moviesStore.get(id));
    }

    public void removeMovieById(int id) {
        moviesStore.remove(id);
    }

}




