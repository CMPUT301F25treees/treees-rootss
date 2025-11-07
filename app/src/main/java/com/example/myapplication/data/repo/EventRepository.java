package com.example.myapplication.data.repo;

import android.content.Context;
import android.net.Uri;

import com.example.myapplication.data.model.Event;
import com.example.myapplication.features.user.UserEvent;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public interface EventRepository {
    void createEvent(Context context, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure);

    void updateEvent(String eventId, UserEvent event, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure);
}
