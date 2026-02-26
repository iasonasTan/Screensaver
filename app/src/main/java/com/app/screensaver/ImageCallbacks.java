package com.app.screensaver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ImageCallbacks implements View.OnClickListener, ActivityResultCallback<ActivityResult> {
    public static Path sImagePath = null;

    public static void initIfNotInitialized(Context context) throws IOException {
        if (sImagePath == null) {
            String filesDirStr = context.getFilesDir().getAbsolutePath();
            Path imageDirectory = Paths.get(filesDirStr, "background");
            sImagePath = Paths.get(filesDirStr, "background", "background.png");
            if(!Files.exists(imageDirectory))
                Files.createDirectory(imageDirectory);
            if(!Files.exists(sImagePath))
                Files.createFile(sImagePath);
        }
    }

    private final ActivityResultLauncher<Intent> mPickImageLauncher;
    private final Context context;

    public ImageCallbacks(Context context, ActivityResultCaller caller) {
        this.context = context;
        mPickImageLauncher = caller.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this);
    }

    @Override
    public void onClick(View v) {
        Log.d("debug_callback", "ViewID: "+v.getId());
        if (v.getId() == R.id.remove_image_button) {
            try {
                Files.deleteIfExists(sImagePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (v.getId() == R.id.select_image_button) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            mPickImageLauncher.launch(intent);
        }
    }

    @Override
    public void onActivityResult(ActivityResult result) {
        if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri imageUri=result.getData().getData();
            if(imageUri!=null) {
                // change image
                try {
                    Files.deleteIfExists(sImagePath);
                    Files.copy(context.getContentResolver().openInputStream(imageUri), sImagePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
