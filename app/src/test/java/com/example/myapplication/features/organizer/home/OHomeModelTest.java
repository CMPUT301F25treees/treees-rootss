package com.example.myapplication.features.organizer.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.features.user.UserEvent;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class OHomeModelTest {

    @Test
    public void getEvents_filtersUpcomingCorrectly() {
        long now = System.currentTimeMillis();
        
        UserEvent future = new UserEvent();
        future.setStartTimeMillis(now + 10000);
        future.setEndTimeMillis(now + 20000);
        
        UserEvent past = new UserEvent();
        past.setStartTimeMillis(now - 20000);
        past.setEndTimeMillis(now - 10000);

        OHomeModel model = new OHomeModel();
        model.setEvents(Arrays.asList(future, past));
        
        // Default is UPCOMING
        List<UserEvent> upcoming = model.getEvents();
        assertEquals(1, upcoming.size());
        assertEquals(future, upcoming.get(0));
        
        // Explicitly set UPCOMING
        model.setFilter(OHomeModel.FilterType.UPCOMING);
        upcoming = model.getEvents();
        assertEquals(1, upcoming.size());
        assertEquals(future, upcoming.get(0));
    }

    @Test
    public void getEvents_filtersPastCorrectly() {
        long now = System.currentTimeMillis();
        
        UserEvent future = new UserEvent();
        future.setStartTimeMillis(now + 10000);
        future.setEndTimeMillis(now + 20000);
        
        UserEvent past = new UserEvent();
        past.setStartTimeMillis(now - 20000);
        past.setEndTimeMillis(now - 10000);

        OHomeModel model = new OHomeModel();
        model.setEvents(Arrays.asList(future, past));
        model.setFilter(OHomeModel.FilterType.PAST);
        
        List<UserEvent> pastEvents = model.getEvents();
        assertEquals(1, pastEvents.size());
        assertEquals(past, pastEvents.get(0));
    }
    
    @Test
    public void getEvents_handlesOngoingEventsAsUpcoming() {
        long now = System.currentTimeMillis();
        
        UserEvent ongoing = new UserEvent();
        ongoing.setStartTimeMillis(now - 5000);
        ongoing.setEndTimeMillis(now + 5000);

        OHomeModel model = new OHomeModel();
        model.setEvents(Arrays.asList(ongoing));
        model.setFilter(OHomeModel.FilterType.UPCOMING);
        
        List<UserEvent> events = model.getEvents();
        assertEquals(1, events.size());
        assertEquals(ongoing, events.get(0));
    }

    @Test
    public void getEvents_clearsFilterCorrectly() {
        long now = System.currentTimeMillis();

        UserEvent future = new UserEvent();
        future.setStartTimeMillis(now + 10000);
        future.setEndTimeMillis(now + 20000);

        UserEvent past = new UserEvent();
        past.setStartTimeMillis(now - 20000);
        past.setEndTimeMillis(now - 10000);

        OHomeModel model = new OHomeModel();
        model.setEvents(Arrays.asList(future, past));

        // Set to PAST filter
        model.setFilter(OHomeModel.FilterType.PAST);
        List<UserEvent> pastEvents = model.getEvents();
        assertEquals(1, pastEvents.size());
        assertEquals(past, pastEvents.get(0));

        // "Clear Filter" should reset to UPCOMING
        model.setFilter(OHomeModel.FilterType.UPCOMING);
        List<UserEvent> upcomingEvents = model.getEvents();
        assertEquals(1, upcomingEvents.size());
        assertEquals(future, upcomingEvents.get(0));
    }
}
