package com.example.myapplication.data.repo;

import androidx.annotation.NonNull;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ImageRepository {

    private static final String CLOUD_NAME = "dyb8t5n7k";
    private static final String UPLOAD_PRESET = "treees_images";
    private static final String ENDPOINT = "https://api.cloudinary.com/v1_1/dyb8t5n7k/image/upload";

    private final OkHttpClient client = new OkHttpClient();

    public interface UploadCallback{
        void onSuccess(String secureUrl);
        void onError(Exception e);
    }

    public void uploadImage(@NonNull File imageFile, @NonNull UploadCallback callback){
        RequestBody body = RequestBody.create(imageFile, MediaType.parse("image/*"));

        MultipartBody request = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", imageFile.getName(), body)
                .addFormDataPart("upload_preset", UPLOAD_PRESET)
                .build();

        Request request1 = new Request.Builder()
                .url(ENDPOINT)
                .post(request)
                .build();
    }
}
