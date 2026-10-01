package ru.practicum.moviehub.model;

public class Movie {
    private int id;
    private int year;
    private String title;

    public Movie(String title, int year) {
        this.title = title;
        this.year = year;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}