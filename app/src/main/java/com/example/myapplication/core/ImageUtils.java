package com.example.myapplication.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.example.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {

    /**
     * Creates a temporary file from the default profile drawable and returns its Uri.
     * This is used as a fallback poster image.
     *
     * @param context The application context.
     * @return Uri of the temporary default image file, or null if an error occurs.
     */
    public static Uri createDefaultPosterUri(Context context) {
        try {
            // Get the drawable as a Bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.profile);

            // Create a file in the cache directory
            File file = new File(context.getCacheDir(), "default_event_poster.png");

            // Write the bitmap to the file
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
