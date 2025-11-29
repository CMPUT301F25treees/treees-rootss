package com.example.myapplication.data.model;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class UserTest {

    @Test
    public void testUserRatingInitialization() {
        User user = new User("uid123", "test@example.com", "testuser", "User");
        
        assertEquals(0.0, user.getRating(), 0.001);
        assertEquals(0, user.getRatingCount());
    }

    @Test
    public void testSetAndGetRating() {
        User user = new User();
        user.setRating(4.5);
        assertEquals(4.5, user.getRating(), 0.001);
    }

    @Test
    public void testSetAndGetRatingCount() {
        User user = new User();
        user.setRatingCount(10);
        assertEquals(10, user.getRatingCount());
    }
}
