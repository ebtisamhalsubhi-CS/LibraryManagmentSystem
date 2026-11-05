package librarysystem;

import java.time.LocalDate;

// BorrowRecord class
public class BorrowRecord {

    private int bookId;
    private int memberId;

    private LocalDate borrowDate;
    private LocalDate dueDate;
    private LocalDate returnedAt;

    // constructor
    public BorrowRecord(int bookId, int memberId,
                        LocalDate borrowDate, LocalDate dueDate) {

        this.bookId = bookId;
        this.memberId = memberId;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;

        returnedAt = null;
    }

    public int getBookId() {

        return bookId;
    }

    public int getMemberId() {
        return memberId;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public boolean isReturned() {

        if (returnedAt != null) {
            return true;
        }

        return false;
    }

    public void returnBook() {

        returnedAt = LocalDate.now();
    }
}