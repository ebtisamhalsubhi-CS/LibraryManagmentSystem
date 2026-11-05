package librarysystem;

// Book class
// Parent class
public class Book {

    // variables
    protected int bookId;
    protected String title;
    protected String author;
    protected BookStatus status;

    // constructor
    public Book(int bookId, String title, String author) {

        this.bookId = bookId;
        this.title = title;
        this.author = author;

        status = BookStatus.AVAILABLE;
    }

    // setter methods
    public void setAvailable(boolean available) {

        if (available == true) {
            status = BookStatus.AVAILABLE;
        }
        else {
            status = BookStatus.BORROWED;
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    // getter methods
    public int getBookId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public BookStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {

        if (status == BookStatus.AVAILABLE) {
            return true;
        }

        return false;
    }

    // normal display method
    public void display() {

        System.out.println(bookId + " - " + title + " - " + author + " - " + status);
    }

    // overloaded display method
    public void display(boolean detailed) {

        if (detailed == true) {

            System.out.println("Book ID: " + bookId);
            System.out.println("Title: " + title);
            System.out.println("Author: " + author);
            System.out.println("Status: " + status);
        }
        else {

            display();
        }
    }
}