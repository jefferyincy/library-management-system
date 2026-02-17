package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public class IssueRecord implements Serializable {

    private String id;
    private String bookId;
    private String userId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private String status; // ISSUED / RETURNED

    public IssueRecord(String bookId, String userId,
                       LocalDate issueDate, LocalDate dueDate) {
        this.id = UUID.randomUUID().toString();
        this.bookId = bookId;
        this.userId = userId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = "ISSUED";
        this.fineAmount = 0;
    }

    public String getBookId() { return bookId; }
    public String getUserId() { return userId; }
    public LocalDate getDueDate() { return dueDate; }
    public String getStatus() { return status; }

    public void returnBook(LocalDate returnDate, double fine) {
        this.returnDate = returnDate;
        this.fineAmount = fine;
        this.status = "RETURNED";
    }

    @Override
    public String toString() {
        return "Issue ID: " + id +
                "\nBook ID: " + bookId +
                "\nUser ID: " + userId +
                "\nIssue Date: " + issueDate +
                "\nDue Date: " + dueDate +
                "\nReturn Date: " + returnDate +
                "\nFine: ₹" + fineAmount +
                "\nStatus: " + status +
                "\n---------------------------";
    }
}
