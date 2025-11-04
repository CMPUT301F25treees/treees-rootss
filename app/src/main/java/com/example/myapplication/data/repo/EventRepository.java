package com.example.myapplication.data.repo;

import android.net.Uri;

import com.example.myapplication.data.model.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public interface EventRepository {
    void createEvent(Event event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure);

    void uploadPoster(Uri imageUri, OnSuccessListener<String> onSuccess, OnFailureListener onFailure);
}
