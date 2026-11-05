package librarysystem;

import java.util.ArrayList;
import java.time.LocalDate;

// LibrarySystem class
public class LibrarySystem {

    private ArrayList<Book> books = new ArrayList<>();
    private ArrayList<Member> members = new ArrayList<>();
    private ArrayList<BorrowRecord> records = new ArrayList<>();

    // constructor
    public LibrarySystem() {

        books.add(new PrintedBook(101, "The Great Gatsby", "F. Scott Fitzgerald", 180));
        books.add(new PrintedBook(102, "Pride and Prejudice", "Jane Austen", 432));
        books.add(new PrintedBook(103, "The Alchemist", "Paulo Coelho", 208));
        books.add(new PrintedBook(104, "Harry Potter and the Philosopher's Stone", "J.K. Rowling", 223));
        books.add(new PrintedBook(105, "The Hobbit", "J.R.R. Tolkien", 310));
        books.add(new PrintedBook(106, "The Fault in Our Stars", "John Green", 313)); 
        books.add(new PrintedBook(107, "It Ends with Us", "Colleen Hoover", 384));
        books.add(new PrintedBook(108, "The Book Thief", "Markus Zusak", 584));
        books.add(new PrintedBook(109, "Little Women", "Louisa May Alcott", 449));
        books.add(new PrintedBook(110, "The Silent Patient", "Alex Michaelides", 336));
        books.add(new PrintedBook(111, "Jane Eyre", "Charlotte Brontë", 532));
        books.add(new PrintedBook(112, "The Hunger Games", "Suzanne Collins", 374));
        books.add(new PrintedBook(113, "Twilight", "Stephenie Meyer", 498));
        books.add(new PrintedBook(114, "Verity", "Colleen Hoover", 336));
        books.add(new PrintedBook(115, "The Midnight Library", "Matt Haig", 304));
        books.add(new PrintedBook(116, "The Seven Husbands of Evelyn Hugo", "Taylor Jenkins Reid", 400));
        books.add(new PrintedBook(117, "The Kite Runner", "Khaled Hosseini", 371));
        books.add(new PrintedBook(118, "The Diary of a Young Girl", "Anne Frank", 283));


        members.add(new Member(202, "Omer", "966835367"));
        members.add(new Member(303, "Marwan", "96685363"));
        members.add(new Member(404, "Luke", "966285425"));
        members.add(new Member(505, "Emily", "966987638"));
        members.add(new Member(606, "Farah", "966987402"));
        members.add(new Member(707, "Sara", "966876493"));
    }

    public ArrayList<Book> getBooks() {
        return books;
    }

    public ArrayList<Member> getMembers() {
        return members;
    }

    public ArrayList<BorrowRecord> getRecords() {
        return records;
    }

    public Member searchMemberById(int id) {

        for (Member m : members) {
            if (m.getMemberId() == id) {
                return m;
            }
        }

        return null;
    }

    public ArrayList<Book> findByTitle(String title) {

        ArrayList<Book> result = new ArrayList<>();

        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(title.toLowerCase())) {
                result.add(b);
            }
        }

        return result;
    }

    public ArrayList<Book> findByAuthor(String author) {

        ArrayList<Book> result = new ArrayList<>();

        for (Book b : books) {
            if (b.getAuthor().toLowerCase().contains(author.toLowerCase())) {
                result.add(b);
            }
        }

        return result;
    }

    public void addBook(Book b) {

        books.add(b);
        System.out.println("Book added successfully");
    }

    public void viewBooks() {

        if (books.isEmpty()) {
            System.out.println("No books found");
        }
        else {
            System.out.println("\n===== Books List =====");

            for (Book b : books) {
                b.display();
            }
        }
    }

    public Book searchById(int id) {

        for (Book b : books) {
            if (b.getBookId() == id) {
                return b;
            }
        }

        return null;
    }

    public void searchByTitle(String title) {

        boolean found = false;

        for (Book b : books) {
            if (b.getTitle().toLowerCase().contains(title.toLowerCase())) {
                b.display(true);
                found = true;
            }
        }

        if (found == false) {
            System.out.println("Book not found");
        }
    }

    public void searchByAuthor(String author) {

        boolean found = false;

        for (Book b : books) {
            if (b.getAuthor().equalsIgnoreCase(author)) {
                b.display(true);
                found = true;
            }
        }

        if (found == false) {
            System.out.println("Book not found");
        }
    }

    public boolean updateBook(int id, String newTitle, String newAuthor) {

        Book b = searchById(id);

        if (b != null) {
            b.setTitle(newTitle);
            b.setAuthor(newAuthor);

            System.out.println("Book updated successfully");
            return true;
        }
        else {
            System.out.println("Book not found");
            return false;
        }
    }

    public boolean deleteBook(int id) {

        Book b = searchById(id);

        if (b != null) {
            books.remove(b);

            System.out.println("Book deleted successfully");
            return true;
        }
        else {
            System.out.println("Book not found");
            return false;
        }
    }

    public void addMember(Member m) {

        members.add(m);
        System.out.println("Member added successfully");
    }

    public void viewMembers() {

        if (members.isEmpty()) {
            System.out.println("No members found");
        }
        else {
            System.out.println("\n===== Members List =====");

            for (Member m : members) {
                m.display();
                System.out.println();
            }
        }
    }

    public boolean borrowBook(int bookId, int memberId) {

        Book b = searchById(bookId);

        if (b != null && b.isAvailable()) {

            BorrowRecord record =
                    new BorrowRecord(
                            bookId,
                            memberId,
                            LocalDate.now(),
                            LocalDate.now().plusDays(14)
                    );

            records.add(record);

            b.setAvailable(false);

            System.out.println("Book borrowed successfully");
            System.out.println("Due Date: " + LocalDate.now().plusDays(14));
            return true;
        }
        else {
            System.out.println("Book is not available");
            return false;
        }
    }

    public boolean returnBook(int bookId) {

        for (BorrowRecord r : records) {

            if (r.getBookId() == bookId && !r.isReturned()) {

                r.returnBook();

                Book b = searchById(bookId);

                if (b != null) {
                    b.setAvailable(true);
                }

                System.out.println("Book returned successfully");
                return true;
            }
        }

        System.out.println("Borrow record not found");
        return false;
    }
}