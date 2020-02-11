package dev.ginyai.flatitemeditor.util;

public class Warp<T> {
    private T t;

    public Warp(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }
}
