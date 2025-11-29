package com.example.myapplication.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.example.myapplication.data.model.User;

/**
 * Persists the last user who signed in on this device so we can restore their
 * session without prompting for credentials again.
 */
public class DeviceLoginStore {

    private static final String PREF_NAME = "device_login_store";
    private static final String KEY_UID = "uid";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_LOGGED_OUT = "logged_out";

    /** Private constructor to prevent instantiation
     * */
    private DeviceLoginStore() {}

    /**
     * Remember the given user as the last signed-in user on this device.
     * @param context The application context
     * @param user The user to remember
     */
    public static void rememberUser(Context context, User user) {
        if (context == null || user == null || user.getUid() == null) {
            return;
        }
        SharedPreferences prefs = prefs(context);
        prefs.edit()
                .putString(KEY_UID, user.getUid())
                .putString(KEY_EMAIL, user.getEmail())
                .putString(KEY_USERNAME, user.getUsername())
                .putString(KEY_ROLE, user.getRole())
                .putString(KEY_DEVICE_ID, deviceId(context))
                .putBoolean(KEY_LOGGED_OUT, false)
                .apply();
    }

    /**
     * Marks the user as logged out on this device.
     * @param context The application context
     */
    public static void markLoggedOut(Context context) {
        if (context == null) {
            return;
        }
        prefs(context).edit()
                .putBoolean(KEY_LOGGED_OUT, true)
                .remove(KEY_UID)
                .remove(KEY_EMAIL)
                .remove(KEY_USERNAME)
                .remove(KEY_ROLE)
                .apply();
    }

    /**
     * Retrieves the remembered user if they are still considered logged in on this device.
     * @param context The application context
     * @return The remembered user, or null if no valid user is remembered
     */
    @Nullable
    public static User getRememberedUser(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(KEY_LOGGED_OUT, false)) {
            return null;
        }
        String savedDeviceId = prefs.getString(KEY_DEVICE_ID, null);
        String currentDeviceId = deviceId(context);
        if (savedDeviceId == null || currentDeviceId == null || !savedDeviceId.equals(currentDeviceId)) {
            return null;
        }

        String uid = prefs.getString(KEY_UID, null);
        if (uid == null) {
            return null;
        }

        User user = new User();
        user.setUid(uid);
        user.setEmail(prefs.getString(KEY_EMAIL, null));
        user.setUsername(prefs.getString(KEY_USERNAME, null));
        String role = prefs.getString(KEY_ROLE, null);
        user.setRole(role != null ? role : "user");
        return user;
    }

    /**
     * Gets the SharedPreferences instance for device login storage.
     * @param context The application context
     * @return The SharedPreferences instance
     */
    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Retrieves the unique device ID for this Android device.
     * @param context The application context
     * @return The device ID string
     */
    private static String deviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}
