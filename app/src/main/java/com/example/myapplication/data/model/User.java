package com.example.myapplication.data.model;

public class User {
    private String uid;
    private String email;
    private String username;
    private String role;
    private long createdAt;

    /**
     * Empty constructor for user deserialization
     * @param None
     * @return void
     */
    public User() {}

    /**
     * Parameterized constructor for User
     * @param uid Unique identifier for the user
     * @param email Email address of the user
     * @param username Display name of the user
     * @param role Role of the user (e.g., "admin", "user")
     * @return void
     */
    public User(String uid, String email, String username, String role) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Get uid of the user.
     * @param None
     * @return uid as String
     */
    public String getUid() { return uid; }

    /**
     * Set uid of the user.
     * @param uid Unique identifier to set
     * @return void
     */
    public void setUid(String uid) { this.uid = uid; }

    /**
     * Get email of the user.
     * @param None
     * @return email as String
     */
    public String getEmail() { return email; }

    /**
     * Set email of the user.
     * @param email Email address to set
     * @return void
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Get username of the user.
     * @param None
     * @return username as String
     */
    public String getUsername() { return username; }

    /**
     * Set username of the user.
     * @param username Display name to set
     * @return void
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Get role of the user.
     * @param None
     * @return role as String
     */
    public String getRole() { return role; }

    /**
     * Set role of the user.
     * @param role Role to set (e.g., "admin", "user")
     * @return void
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Get account creation timestamp.
     * @param None
     * @return createdAt as long
     */
    public long getCreatedAt() { return createdAt; }

    /**
     * Set account creation timestamp.
     * @param createdAt Timestamp to set
     * @return void
     */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /**
     * Check if the user has admin role.
     * @param None
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(role);
    }

    /**
     * String representation of the User object.
     * @param None
     * @return String describing the user
     */
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

