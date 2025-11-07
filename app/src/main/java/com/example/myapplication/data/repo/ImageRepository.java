package com.example.myapplication.data.repo;

import android.content.Context;
import android.net.Uri;
import android.telecom.Call;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class ImageRepository {

    public interface UploadCallback{
        void onSuccess(String secureUrl);
        void onError(String e);
    }

    public void uploadImage( Uri imageUri, UploadCallback callback){
        MediaManager.get().upload(imageUri)
                .callback(new com.cloudinary.android.callback.UploadCallback(){

                    @Override
                    public void onStart(String requestId) {

                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = resultData.get("secure_url").toString();
                        callback.onSuccess((secureUrl));
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }
                })
                .dispatch();
    }
}
