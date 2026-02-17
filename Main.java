import java.util.Scanner;
import model.*;
import service.LibraryService;

public class Main {

    public static void main(String[] args) {

        LibraryService library = new LibraryService();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n==== LIBRARY SYSTEM ====");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    System.out.print("Name: ");
                    String name = sc.nextLine();

                    System.out.print("Username: ");
                    String username = sc.nextLine();

                    System.out.print("Password: ");
                    String password = sc.nextLine();

                    System.out.print("Role (ADMIN/STUDENT): ");
                    String role = sc.nextLine().toUpperCase();

                    library.registerUser(new User(name, username, password, role));
                    System.out.println("User registered successfully!");
                    break;

                case 2:
                    System.out.print("Username: ");
                    String loginUser = sc.nextLine();

                    System.out.print("Password: ");
                    String loginPass = sc.nextLine();

                    User user = library.login(loginUser, loginPass);

                    if (user != null) {
                        System.out.println("Login successful! Welcome " + user.getName());

                        if (user.getRole().equals("ADMIN")) {
                            adminMenu(sc, library);
                        } else {
                            studentMenu(sc, library, user);
                        }

                    } else {
                        System.out.println("Invalid credentials!");
                    }
                    break;

                case 3:
                    System.exit(0);
            }
        }
    }

    private static void adminMenu(Scanner sc, LibraryService library) {
        while (true) {
            System.out.println("\n---- ADMIN MENU ----");
            System.out.println("1. Add Book");
            System.out.println("2. View Books");
            System.out.println("3. Logout");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Title: ");
                    String title = sc.nextLine();
                    System.out.print("Author: ");
                    String author = sc.nextLine();
                    System.out.print("ISBN: ");
                    String isbn = sc.nextLine();
                    System.out.print("Quantity: ");
                    int qty = sc.nextInt();

                    library.addBook(new Book(title, author, isbn, qty));
                    System.out.println("Book added!");
                    break;

                case 2:
                    library.getBooks().forEach(System.out::println);
                    break;

                case 3:
                    return;
            }
        }
    }

    private static void studentMenu(Scanner sc, LibraryService library, User user) {
        while (true) {
            System.out.println("\n---- STUDENT MENU ----");
            System.out.println("1. View Books");
            System.out.println("2. Issue Book");
            System.out.println("3. Return Book");
            System.out.println("4. Logout");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1:
                    library.getBooks().forEach(System.out::println);
                    break;

                case 2:
                    System.out.print("Enter Book ID to issue: ");
                    String issueId = sc.nextLine();
                    library.issueBook(issueId, user.getId());
                    break;

                case 3:
                    System.out.print("Enter Book ID to return: ");
                    String returnId = sc.nextLine();
                    library.returnBook(returnId, user.getId());
                    break;

                case 4:
                    return;
            }
        }
    }
}
