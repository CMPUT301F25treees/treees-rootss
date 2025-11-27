package com.example.myapplication.core;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.repo.EventRepository;

/**
 * This class is for getting single instances of a service for the application.
 */
public class ServiceLocator {
    private static EventRepository eventRepository;

    /**
     * This method returns a single instance of the EventRepository.
     * @return the EventRepository instance.
     */
    public static EventRepository getEventRepository() {
        if (eventRepository == null) {
            eventRepository = new FirebaseEventRepository();
        }
        return eventRepository;
    }
}
