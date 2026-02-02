

package ru.practicum.moviehub.model;

import java.time.LocalDate;

public class Movie {
    private String title;
    private int year;

    public int getId() {
        return id;
    }

    private int id;
    private final int today = LocalDate.now().getYear();

    public Movie(String title, int year, int id) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Название не может быть пустым");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("Название не может превышать 100 символов");
        }
        this.title = title;
        if (year < 1888 || year > today + 1) {
            throw new IllegalArgumentException("Год должен быть между 1888 и " + (today + 1));
        }
        this.year = year;
        this.id = id;
    }

    public int getYear() {
        return year;
    }
}