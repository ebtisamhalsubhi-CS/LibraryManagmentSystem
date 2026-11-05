package librarysystem;

// User class
public class User {

    // variables
    private String username;
    private String password;

    // constructor
    public User(String username, String password) {

        this.username = username;
        this.password = password;
    }

    // login method
    public boolean login(String u, String p) {

        if (username.equals(u) && password.equals(p)) {
            return true;
        }

        return false;
    }
}