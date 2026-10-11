package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.Map;

public class MoviesStore {
    private final HashMap<Integer, Movie> movieMap = new HashMap<>();
    private int id = 0;

    public HashMap<Integer, Movie> getMovies() {
        return movieMap;
    }

    public Movie addMovie(String title, int year) {
        id++;
        Movie movie = new Movie(title, year, id);
        movieMap.put(id, movie);
        return movie;
    }

    public void deleteMovie(int id) {
            movieMap.remove(id);
    }

    public boolean containsMovie(int id) {
        return movieMap.containsKey(id);
    }

    public HashMap<Integer, Movie> getMoviesByYears(int year) {
        boolean yearIsExist = false;
        for (Movie movie : movieMap.values()) {
            if (movie.getYear() == year) {
                yearIsExist = true;
                break;
            }
        }
        if (yearIsExist) {
            HashMap<Integer, Movie> yearList = new HashMap<>(movieMap);
            for (Map.Entry<Integer, Movie> movie : movieMap.entrySet()) {
                if (movie.getValue().getYear() != year) {
                    yearList.remove(movie.getKey());
                }
            }
            return yearList;
        } else {
            return new HashMap<>();
        }
    }

    public void clearMap() {
        movieMap.clear();
        id = 0;
    }
}