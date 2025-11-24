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

/**
 * This class holds methods that upload images to Cloudinary storage.
 */
public class ImageRepository {

    public interface UploadCallback{
        void onSuccess(String secureUrl);
        void onError(String e);
    }

    /**
     * This method is in charge of uploading images to Cloudinary.
     *
     * The image that gets added by the user is saved to Cloudinary storage, on success
     * the callback returns a secure URL which in another file gets saved to the specified events
     * imageUrl field.
     *
     * @param imageUri The image Uri that the user uploads
     * @param callback callback on success returns the securUrl of the image, on failure returns an error.
     */
    public void uploadImage( Uri imageUri, UploadCallback callback){
        MediaManager.get().upload(imageUri)
                .callback(new com.cloudinary.android.callback.UploadCallback(){

                    /** Empty overridden methods
                        @params requestId - the unique ID of the upload request
                        @return void
                     */
                    @Override
                    public void onStart(String requestId) {

                    }

                    /** Empty overridden methods
                        @params requestId - the unique ID of the upload request
                        @params bytes - the number of bytes uploaded so far
                        @params totalBytes - the total number of bytes to be uploaded
                        @return void
                     */
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {

                    }

                    /** Handles successful image upload
                        @params requestId - the unique ID of the upload request
                        @params resultData - a map containing details about the uploaded image
                        @return void
                     */
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = resultData.get("secure_url").toString();
                        callback.onSuccess((secureUrl));
                    }

                    /** Handles image upload errors
                        @params requestId - the unique ID of the upload request
                        @params error - an ErrorInfo object containing error details
                        @return void
                     */
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        callback.onError(error.getDescription());
                    }

                    /** Empty overridden methods
                        @params requestId - the unique ID of the upload request
                        @params error - an ErrorInfo object containing error details
                        @return void
                     */
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {

                    }
                })
                .dispatch();
    }
}
