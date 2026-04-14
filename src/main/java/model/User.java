package model;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
    private static final long serialVersionUID = 2L;

    private String id;
    private String name;
    private String username;
    private String password;
    private String role;
    /** Borrow / account notifications (e.g. confirmation when issuing a book). */
    private String email;
    /** When true, user cannot log in or borrow (staff may still allow returns). */
    private boolean banned;

    public User() {
    }

    public User(String name, String username, String password, String role, String email) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email != null ? email.trim() : null;
        this.banned = false;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    /** Used when an administrator assigns Staff (Employee) or Student. */
    public void setRole(String role) {
        this.role = role;
    }

    public String getEmail() { return email; }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}
