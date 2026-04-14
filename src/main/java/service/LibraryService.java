package service;

import model.Book;
import model.IssueRecord;
import model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import util.FileUtil;
import util.FineCalculator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LibraryService {
    private final Path dataDir;
    private final MailNotificationService mailNotificationService;
    private final List<Book> books;
    private final List<User> users;
    private final List<IssueRecord> issues;

    public LibraryService(@Value("${library.data-dir:.}") String dataDirProperty,
                          MailNotificationService mailNotificationService) {
        this.mailNotificationService = mailNotificationService;
        this.dataDir = Paths.get(normalizeDataDir(dataDirProperty)).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.dataDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create data directory: " + this.dataDir, e);
        }
        books = FileUtil.load(file("books.dat"));
        users = FileUtil.load(file("users.dat"));
        issues = FileUtil.load(file("issues.dat"));
    }

    private static String normalizeDataDir(String raw) {
        if (raw == null || raw.isBlank()) {
            return ".";
        }
        String p = raw.trim();
        while (p.endsWith("/") || p.endsWith("\\")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? "." : p;
    }

    private String file(String name) {
        return dataDir.resolve(name).toString();
    }

    public synchronized void addBook(Book book) {
        books.add(book);
        FileUtil.save(file("books.dat"), books);
    }

    public synchronized boolean registerUser(User user) {
        String role = user.getRole();
        if (role == null || (!"STUDENT".equals(role) && !"ADMIN".equals(role))) {
            return false;
        }
        boolean exists = users.stream().anyMatch(u -> u.getUsername().equals(user.getUsername()));
        if (exists) {
            return false;
        }
        users.add(user);
        FileUtil.save(file("users.dat"), users);
        return true;
    }

    /**
     * Admin-only: set a non-admin user's role to {@code STUDENT} or {@code EMPLOYEE}.
     */
    public synchronized String updateUserRoleByAdmin(String targetUserId, String newRole) {
        if (newRole == null || newRole.isBlank()) {
            return "Invalid role.";
        }
        String r = newRole.trim().toUpperCase();
        if (!"STUDENT".equals(r) && !"EMPLOYEE".equals(r)) {
            return "Only Student or Employee can be assigned here.";
        }
        User target = users.stream().filter(u -> u.getId().equals(targetUserId)).findFirst().orElse(null);
        if (target == null) {
            return "User not found.";
        }
        if ("ADMIN".equals(target.getRole())) {
            return "Cannot change an administrator's role.";
        }
        target.setRole(r);
        FileUtil.save(file("users.dat"), users);
        return "Role updated.";
    }

    public User login(String username, String password) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password) && !u.isBanned())
                .findFirst()
                .orElse(null);
    }

    /** True when credentials match but the account is suspended (for login messaging). */
    public synchronized boolean isSuspendedAccount(String username, String password) {
        return users.stream()
                .anyMatch(u -> u.getUsername().equals(username)
                        && u.getPassword().equals(password)
                        && u.isBanned());
    }

    public synchronized Optional<User> findUserById(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    /**
     * {@code ADMIN} may suspend {@code STUDENT} or {@code EMPLOYEE}.
     * {@code EMPLOYEE} may suspend {@code STUDENT} only.
     * Nobody may suspend {@code ADMIN}, themselves, or (for employees) staff accounts.
     */
    public synchronized String setUserBan(String actorRole, String actorId, String targetUserId, boolean banned) {
        if (actorRole == null || (!"ADMIN".equals(actorRole) && !"EMPLOYEE".equals(actorRole))) {
            return "Not allowed.";
        }
        User target = users.stream().filter(u -> u.getId().equals(targetUserId)).findFirst().orElse(null);
        if (target == null) {
            return "User not found.";
        }
        if (target.getId().equals(actorId)) {
            return "You cannot change your own suspension status here.";
        }
        if ("ADMIN".equals(target.getRole())) {
            return "Administrator accounts cannot be suspended.";
        }
        if ("EMPLOYEE".equals(actorRole)) {
            if (!"STUDENT".equals(target.getRole())) {
                return "Employees can only suspend student accounts.";
            }
        } else if ("ADMIN".equals(actorRole)) {
            if (!"STUDENT".equals(target.getRole()) && !"EMPLOYEE".equals(target.getRole())) {
                return "Invalid target account.";
            }
        }
        target.setBanned(banned);
        FileUtil.save(file("users.dat"), users);
        return banned ? "User suspended." : "User reinstated.";
    }

    /**
     * Permanently remove a non-admin account. Admin-only (enforced in controller).
     * Blocked if the user still has at least one book checked out.
     */
    public synchronized String deleteUserByAdmin(String adminId, String targetUserId) {
        User target = users.stream().filter(u -> u.getId().equals(targetUserId)).findFirst().orElse(null);
        if (target == null) {
            return "User not found.";
        }
        if (target.getId().equals(adminId)) {
            return "You cannot remove your own account.";
        }
        if ("ADMIN".equals(target.getRole())) {
            return "Administrator accounts cannot be removed.";
        }
        boolean activeLoan = issues.stream()
                .anyMatch(i -> i.getUserId().equals(targetUserId) && "ISSUED".equals(i.getStatus()));
        if (activeLoan) {
            return "Cannot remove user with active loans. Have them return books first.";
        }
        users.removeIf(u -> u.getId().equals(targetUserId));
        FileUtil.save(file("users.dat"), users);
        return "User removed.";
    }

    public synchronized String issueBook(String bookId, String userId) {
        User borrower = users.stream().filter(u -> u.getId().equals(userId)).findFirst().orElse(null);
        if (borrower != null && borrower.isBanned()) {
            return "This account cannot borrow books while suspended.";
        }

        Book book = books.stream().filter(b -> b.getId().equals(bookId)).findFirst().orElse(null);
        if (book == null || book.getAvailableQuantity() <= 0) {
            return "Book not available.";
        }

        book.decreaseQuantity();
        IssueRecord issue = new IssueRecord(bookId, userId, LocalDate.now(), LocalDate.now().plusDays(14));
        issues.add(issue);
        FileUtil.save(file("books.dat"), books);
        FileUtil.save(file("issues.dat"), issues);

        boolean emailed = mailNotificationService.sendBorrowConfirmation(borrower, book, issue.getDueDate());

        return emailed
                ? "Book issued successfully. A confirmation email was sent to your inbox."
                : "Book issued successfully.";
    }

    public synchronized String returnBook(String bookId, String userId) {
        for (IssueRecord issue : issues) {
            if (issue.getBookId().equals(bookId) && issue.getUserId().equals(userId) && issue.getStatus().equals("ISSUED")) {
                double fine = FineCalculator.calculateFine(issue.getDueDate(), LocalDate.now());
                issue.returnBook(LocalDate.now(), fine);
                books.stream().filter(b -> b.getId().equals(bookId)).findFirst().ifPresent(Book::increaseQuantity);
                FileUtil.save(file("books.dat"), books);
                FileUtil.save(file("issues.dat"), issues);
                return "Returned successfully. Fine: INR " + fine;
            }
        }
        return "No active issue found.";
    }

    public List<Book> getBooks() { return books; }
    public List<IssueRecord> getIssues() { return issues; }
    public List<User> getUsers() { return users; }
}
