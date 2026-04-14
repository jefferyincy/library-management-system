package model;

import java.io.Serializable;
import java.util.UUID;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String author;
    private String isbn;
    private int quantity;
    private int availableQuantity;
    /** Short description shown when a student selects the book in the catalog. */
    private String summary = "";

    public Book() {
    }

    public Book(String title, String author, String isbn, int quantity) {
        this(title, author, isbn, quantity, "");
    }

    public Book(String title, String author, String isbn, int quantity, String summary) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.quantity = quantity;
        this.availableQuantity = quantity;
        setSummary(summary);
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getQuantity() { return quantity; }
    public int getAvailableQuantity() { return availableQuantity; }

    public String getSummary() {
        return summary != null ? summary : "";
    }

    public void setSummary(String summary) {
        this.summary = summary != null ? summary.trim() : "";
    }

    public void decreaseQuantity() {
        if (availableQuantity > 0) {
            availableQuantity--;
        }
    }

    public void increaseQuantity() {
        if (availableQuantity < quantity) {
            availableQuantity++;
        }
    }
}
