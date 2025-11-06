package com.example.myapplication.core;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dyb8t5n7k");
        config.put("upload_preset", "treees_images");
        MediaManager.init(this, config);
    }
}
