package com.raj.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class AndroidBridge {

    private final Activity activity;

    public AndroidBridge(Activity activity) {
        this.activity = activity;
    }

    public boolean hasPermission(String permission) {
        return activity.checkSelfPermission(permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean hasMicrophonePermission() {
        return hasPermission(Manifest.permission.RECORD_AUDIO);
    }

    public boolean hasCameraPermission() {
        return hasPermission(Manifest.permission.CAMERA);
    }

    public boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }

        return hasPermission(Manifest.permission.POST_NOTIFICATIONS);
    }

    public boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return hasPermission(Manifest.permission.READ_MEDIA_IMAGES);
        }

        return hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public void requestMicrophonePermission(int requestCode) {
        requestPermissionsIfNeeded(
                new String[]{Manifest.permission.RECORD_AUDIO},
                requestCode
        );
    }

    public void requestCameraPermission(int requestCode) {
        requestPermissionsIfNeeded(
                new String[]{Manifest.permission.CAMERA},
                requestCode
        );
    }

    public void requestNotificationPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionsIfNeeded(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    requestCode
            );
        }
    }

    public void requestMediaPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionsIfNeeded(
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    requestCode
            );
        } else {
            requestPermissionsIfNeeded(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    requestCode
            );
        }
    }

    private void requestPermissionsIfNeeded(
            String[] permissions,
            int requestCode
    ) {
        java.util.ArrayList<String> missing =
                new java.util.ArrayList<>();

        for (String permission : permissions) {
            if (!hasPermission(permission)) {
                missing.add(permission);
            }
        }

        if (!missing.isEmpty()) {
            activity.requestPermissions(
                    missing.toArray(new String[0]),
                    requestCode
            );
        }
    }

    public String getPermissionStatus() {
        return "microphone="
                + (hasMicrophonePermission() ? "granted" : "not_granted")
                + ", camera="
                + (hasCameraPermission() ? "granted" : "not_granted")
                + ", notifications="
                + (hasNotificationPermission() ? "granted" : "not_granted")
                + ", media="
                + (hasMediaPermission() ? "granted" : "not_granted");
    }

    public Context getContext() {
        return activity;
    }
}
