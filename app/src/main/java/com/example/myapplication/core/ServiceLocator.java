package com.example.myapplication.core;

import com.example.myapplication.data.firebase.FirebaseEventRepository;
import com.example.myapplication.data.repo.EventRepository;

public class ServiceLocator {
    private static EventRepository eventRepository;

    public static EventRepository getEventRepository() {
        if (eventRepository == null) {
            eventRepository = new FirebaseEventRepository();
        }
        return eventRepository;
    }
}
