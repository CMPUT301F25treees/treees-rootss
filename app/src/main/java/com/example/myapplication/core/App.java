package com.example.myapplication.core;

import android.app.Application;

import com.cloudinary.android.MediaManager;
import com.google.firebase.FirebaseApp;

import java.util.HashMap;
import java.util.Map;

/**
 * This is the base Application class.
 * Responsible for initiating Firebase and Cloudinary services.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        initCloudinary();
    }

    /**
     * This method initializes the cloudinary SDK.
     */
    private void initCloudinary(){

        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dyb8t5n7k");
        config.put("api_key", "991229776395468");
        config.put("api_secret", "M2iXfmDthI_d1FKAw_ZMw651Fbo");
        config.put("upload_preset", "treees_images");
        try{
            MediaManager.init(this, config);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
