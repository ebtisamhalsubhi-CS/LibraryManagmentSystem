package librarysystem;

import java.util.Scanner;
import java.util.InputMismatchException;

public class Main {

    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);

        // create account
        System.out.println("====Create Account====");

        System.out.print("Enter username: \n");
        String u = in.nextLine();

        System.out.print("Enter password: \n");
        String p = in.nextLine();

        User user = new User(u, p);

        // login
        System.out.println("===Login===");

        System.out.print("Username: \n");
        String lu = in.nextLine();

        System.out.print("Password: \n");
        String lp = in.nextLine();

        if (user.login(lu, lp)) {

            System.out.println("\nLogin successful");

            LibrarySystem system = new LibrarySystem();

            int choice = 0;

            do {

                try {

                    System.out.println("\n===== Library Menu =====");
                    System.out.println("1. Add Book");
                    System.out.println("2. View Books");
                    System.out.println("3. Add Member");
                    System.out.println("4. View Members");
                    System.out.println("5. Borrow Book");
                    System.out.println("6. Return Book");
                    System.out.println("7. Delete Book");
                    System.out.println("8. Search Book");
                    System.out.println("9. Update Book");
                    System.out.println("10.Exit");

                    System.out.print("Enter your choice: ");
                    choice = in.nextInt();
                    in.nextLine();

                    switch (choice) {

                        case 1:

                            System.out.print("Enter Book ID: ");
                            int id = in.nextInt();
                            in.nextLine();

                            System.out.print("Enter Title: ");
                            String title = in.nextLine();

                            System.out.print("Enter Author: ");
                            String author = in.nextLine();

                            System.out.print("Enter Pages: ");
                            int pages = in.nextInt();
                            in.nextLine();

                            PrintedBook book =
                                    new PrintedBook(id, title, author, pages);

                            system.addBook(book);

                            break;

                        case 2:

                            system.viewBooks();

                            break;

                        case 3:

                            System.out.print("Enter Member ID: ");
                            int memberId = in.nextInt();
                            in.nextLine();

                            System.out.print("Enter Name: ");
                            String name = in.nextLine();

                            System.out.print("Enter Phone: ");
                            String phone = in.nextLine();

                            Member member =
                                    new Member(memberId, name, phone);

                            system.addMember(member);

                            break;

                        case 4:

                            system.viewMembers();
                         

                            break;

                        case 5:

                        	   System.out.print("Enter Book ID: ");
                               int borrowBookId = in.nextInt();

                               System.out.print("Enter Member ID: ");
                               int borrowMemberId = in.nextInt();
                               in.nextLine();

                               system.borrowBook(borrowBookId, borrowMemberId);
                           
                            break;

                        case 6:

                        	 System.out.print("Enter Book ID: ");
                             int returnId = in.nextInt();
                             in.nextLine();

                             system.returnBook(returnId);

                            break;

                        case 7:

                            System.out.print("Enter Book ID: ");
                            int deleteId = in.nextInt();
                            in.nextLine();
                            
                            system.deleteBook(deleteId);
                            
                            break;

                        case 8:
                        	
                            System.out.println("1. Search by ID");
                            System.out.println("2. Search by Title");
                            System.out.println("3. Search by Author");

                            System.out.print("Choice: ");
                            int searchChoice = in.nextInt();
                            in.nextLine();

                            switch (searchChoice) {

                                case 1:

                                    System.out.print("Enter ID: ");
                                    int searchId = in.nextInt();
                                    in.nextLine();

                                    Book b = system.searchById(searchId);

                                    if (b != null) {
                                        b.display(true);
                                    }
                                    else {
                                        System.out.println("Book not found");
                                    }

                                    break;

                                case 2:

                                    System.out.print("Enter Title: ");
                                    String searchTitle = in.nextLine();

                                    system.searchByTitle(searchTitle);

                                    break;

                                case 3:

                                    System.out.print("Enter Author: ");
                                    String searchAuthor = in.nextLine();

                                    system.searchByAuthor(searchAuthor);

                                    break;

                                default:
                                    System.out.println("Wrong choice");
                            }

                            break;

                        case 9:

                        	 System.out.print("Enter Book ID: ");
                             int updateId = in.nextInt();
                             in.nextLine();
  
                             System.out.print("Enter New Title: ");
                             String newTitle = in.nextLine();
  
                             System.out.print("Enter New Author: ");
                             String newAuthor = in.nextLine();
  
                             system.updateBook(updateId, newTitle, newAuthor);

                            break;

                        case 10:

                            System.out.println("ThankYou. See you next time");

                            break;

                        default:

                            System.out.println("Invalid choice");
                    }

                }
                catch (InputMismatchException e) {

                    System.out.println("Please enter correct input");
                    in.nextLine();
                }

            } while (choice != 10);

        }
        else {

            System.out.println("Wrong username or password");
        }

    }
}
