package com.example.myapplication.core;

import com.example.myapplication.data.model.User;

/**
 * Singleton class to manage the current user session
 * This holds the logged-in user throughout the app lifecycle
 */
public class UserSession {
    private static UserSession instance;
    private User currentUser;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    /**
     * Set the current logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    /**
     * Get the current logged-in user
     * @return User object or null if not logged in
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is currently logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Clear the current user session (logout)
     */
    public void clearSession() {
        this.currentUser = null;
    }
}


// user@example.com password123