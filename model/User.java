package model;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {

    private String id;
    private String name;
    private String username;
    private String password;
    private String role; // ADMIN or STUDENT

    public User(String name, String username, String password, String role) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getName() { return name; }
}
