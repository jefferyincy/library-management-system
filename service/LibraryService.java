package service;

import model.*;
import util.FileUtil;
import util.FineCalculator;

import java.time.LocalDate;
import java.util.List;

public class LibraryService {

    private List<Book> books;
    private List<User> users;
    private List<IssueRecord> issues;

    public LibraryService() {
        books = FileUtil.load("books.dat");
        users = FileUtil.load("users.dat");
        issues = FileUtil.load("issues.dat");
    }

    public void addBook(Book book) {
        books.add(book);
        FileUtil.save("books.dat", books);
    }

    public void registerUser(User user) {
        users.add(user);
        FileUtil.save("users.dat", users);
    }

    public User login(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username)
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    public void issueBook(String bookId, String userId) {
        Book book = books.stream()
                .filter(b -> b.getId().equals(bookId))
                .findFirst()
                .orElse(null);

        if (book != null && book.getAvailableQuantity() > 0) {
            book.decreaseQuantity();
            IssueRecord issue = new IssueRecord(
                    bookId,
                    userId,
                    LocalDate.now(),
                    LocalDate.now().plusDays(14)
            );
            issues.add(issue);

            FileUtil.save("books.dat", books);
            FileUtil.save("issues.dat", issues);
        } else {
            System.out.println("Book not available!");
        }
    }

    public void returnBook(String bookId, String userId) {
        for (IssueRecord issue : issues) {
            if (issue.getBookId().equals(bookId)
                    && issue.getUserId().equals(userId)
                    && issue.getStatus().equals("ISSUED")) {

                double fine = FineCalculator.calculateFine(
                        issue.getDueDate(),
                        LocalDate.now()
                );

                issue.returnBook(LocalDate.now(), fine);

                books.stream()
                        .filter(b -> b.getId().equals(bookId))
                        .findFirst()
                        .ifPresent(Book::increaseQuantity);

                FileUtil.save("books.dat", books);
                FileUtil.save("issues.dat", issues);

                System.out.println("Returned successfully. Fine: ₹" + fine);
                return;
            }
        }
        System.out.println("No active issue found.");
    }

    public List<Book> getBooks() { return books; }
    public List<IssueRecord> getIssues() { return issues; }
}
