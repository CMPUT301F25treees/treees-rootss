package com.example.myapplication.data.model;

public class User {
    private String uid;
    private String email;
    private String username;
    private String role;
    private long createdAt;

    // Empty constructor for Firebase
    public User() {}

    public User(String uid, String email, String username, String role) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "uid='" + uid + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}

