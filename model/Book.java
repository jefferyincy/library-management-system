package model;

import java.io.Serializable;
import java.util.UUID;

public class Book implements Serializable {

    private String id;
    private String title;
    private String author;
    private String isbn;
    private int quantity;
    private int availableQuantity;

    public Book(String title, String author, String isbn, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.quantity = quantity;
        this.availableQuantity = quantity;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getQuantity() { return quantity; }
    public int getAvailableQuantity() { return availableQuantity; }

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

    @Override
    public String toString() {
        return "ID: " + id +
                "\nTitle: " + title +
                "\nAuthor: " + author +
                "\nISBN: " + isbn +
                "\nAvailable: " + availableQuantity + "/" + quantity +
                "\n---------------------------";
    }
}
