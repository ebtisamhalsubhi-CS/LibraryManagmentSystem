package librarysystem;

// PrintedBook class
// inheritance
public class PrintedBook extends Book {

    // variable
    private int pages;

    // constructor
    public PrintedBook(int bookId, String title, String author, int pages) {

        super(bookId, title, author);

        this.pages = pages;
    }

    public int getPages() {
        return pages;
    }

    // display method
    public void display() {

        System.out.println(bookId + " - " + title + " - " +author + " - " + "Pages: " + pages +" - " + status);
    }
}
