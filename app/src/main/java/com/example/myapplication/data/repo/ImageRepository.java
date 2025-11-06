package com.example.myapplication.data.repo;

import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

        client.newCall(request1).enqueue(new Callback(){

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e){
                callback.onError(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException{
                if(!response.isSuccessful()){
                    callback.onError(new IOException("Upload failed: " + response.message()));
                    return;
                }

                String responseBody = response.body().string();
                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                String secureUrl = json.get("secure_url").getAsString();
                callback.onSuccess(secureUrl);
            }
        });
    }
}
