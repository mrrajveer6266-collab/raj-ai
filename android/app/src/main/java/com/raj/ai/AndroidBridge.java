package com.raj.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.Intent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AndroidBridge {

    private final Activity activity;
    private Uri cameraOutputUri;

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

    public void openCamera(int requestCode) {
        if (!hasCameraPermission()) {
            requestCameraPermission(requestCode);
            return;
        }

        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            return;
        }

        try {
            String timestamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                Locale.US
            ).format(new Date());

            ContentValues values = new ContentValues();
            values.put(
                android.provider.MediaStore.Images.Media.DISPLAY_NAME,
                "RAJ_AI_" + timestamp + ".jpg"
            );
            values.put(
                android.provider.MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(
                    android.provider.MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/RAJ AI"
                );
            }

            cameraOutputUri = activity.getContentResolver().insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            );

            if (cameraOutputUri == null) {
                return;
            }

            intent.putExtra(
                android.provider.MediaStore.EXTRA_OUTPUT,
                cameraOutputUri
            );

            intent.addFlags(
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            activity.startActivityForResult(intent, requestCode);

        } catch (Exception e) {
            cameraOutputUri = null;
        }
    }

    public Uri getCameraOutputUri() {
        return cameraOutputUri;
    }

    public void clearCameraOutputUri() {
        cameraOutputUri = null;
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
