package com.app.screensaver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.Fragment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {
    private ActivityResultLauncher<Intent> mPickImageLauncher;
    private CheckBox mClockCB, mTimerCB, mBatteryCB;
    private boolean isUserSelection = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBatteryCB = view.findViewById(R.id.battery_visible);
        mClockCB = view.findViewById(R.id.text_clock_visible);
        mTimerCB = view.findViewById(R.id.timer_visible);

        view.findViewById(R.id.back_button).setOnClickListener(ignored -> {
            Intent intent = new Intent("CHANGE_FRAGMENT").setPackage(requireContext().getPackageName());
            intent.putExtra("fragmentClass", ScreensaverFragment.class.getName());
            sendVisibilityConfiguration();
            requireContext().sendBroadcast(intent);
        });

        Spinner fontSpinner = view.findViewById(R.id.font_spinner);
        Field[] fontResourceFields=R.font.class.getFields();
        int[] fontIDs=new int[fontResourceFields.length];
        for (int i = 0; i < fontResourceFields.length; i++) {
            try {
                fontIDs[i] = fontResourceFields[i].getInt(null);
                Log.d("dev-test", "Added font "+fontIDs[i]);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        List<ContainerView> items = new ArrayList<>();
        for (int id : fontIDs) {
            ContainerView containerView = new ContainerView(requireContext(), id);
            containerView.setText(getResources().getResourceEntryName(id)); // show font name
            items.add(containerView);
        }
        ArrayAdapter<ContainerView> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setAdapter(adapter);

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!isUserSelection) {
                    isUserSelection = true;
                    return;
                }

                ContainerView containerView = (ContainerView) parent.getItemAtPosition(position);
                Intent intent = new Intent("CHANGE_FONT").setPackage(requireContext().getPackageName());
                intent.putExtra("fontID", containerView.getKey());
                requireContext().sendBroadcast(intent);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        mPickImageLauncher =registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri=result.getData().getData();
                if(imageUri!=null) {
                    // change fragment
                    Intent changeFragmentIntent=new Intent("CHANGE_FRAGMENT").setPackage(requireContext().getPackageName());
                    changeFragmentIntent.putExtra("fragmentClass", ScreensaverFragment.class.getName());
                    requireContext().sendBroadcast(changeFragmentIntent);
                    // change image
                    Intent changeImageIntent=new Intent("CHANGE_IMAGE").setPackage(requireContext().getPackageName());
                    changeImageIntent.putExtra("imageUri", imageUri.toString());
                    Log.d("dev-test", "Sending new image uri "+imageUri);
                    requireContext().sendBroadcast(changeImageIntent);
                }
            }
        });

        view.findViewById(R.id.select_image_button).setOnClickListener(v -> {
            Intent intent=new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            mPickImageLauncher.launch(intent);
        });

        view.findViewById(R.id.remove_image_button).setOnClickListener(v -> {
            Intent intent=new Intent("REMOVE_IMAGE").setPackage(requireContext().getPackageName());
            requireContext().sendBroadcast(intent);
        });

        view.findViewById(R.id.change_color_button).setOnClickListener(v -> {
            int initialColor=Color.WHITE;
            new yuku.ambilwarna.AmbilWarnaDialog(requireContext(), initialColor, new yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onOk(yuku.ambilwarna.AmbilWarnaDialog dialog, int color) {
                    Intent intent=new Intent("CHANGE_COLOR").setPackage(requireContext().getPackageName());
                    intent.putExtra("color", color);
                    requireContext().sendBroadcast(intent);
                }

                @Override
                public void onCancel(yuku.ambilwarna.AmbilWarnaDialog dialog) {
                    // nothing
                }
            }).show();
        });

        VisibilityConfiguration vc = ScreensaverFragment.mVisibilityConfig;
        if(vc!=null) {
            mTimerCB.setChecked(vc.timer);
            mClockCB.setChecked(vc.clock);
            mBatteryCB.setChecked(vc.battery);
        }
    }

    private void sendVisibilityConfiguration() {
        Intent intent=new Intent("CHANGE_VISIBILITY").setPackage(requireContext().getPackageName());
        Parcelable parcelable=new VisibilityConfiguration(mClockCB.isChecked(), mBatteryCB.isChecked(), mTimerCB.isChecked());
        intent.putExtra("configuration", parcelable);
        requireContext().sendBroadcast(intent);
    }

    public static final class ContainerView extends AppCompatTextView {
        private final int mKey;

        public ContainerView(@NonNull Context context, int key) {
            super(context);
            this.mKey =key;
            setTextColor(Color.WHITE);
        }

        public ContainerView(@NonNull Context context) {
            this(context, 0);
        }

        @NonNull
        @Override
        public String toString() {
            return getResources().getResourceEntryName(mKey);
        }

        public int getKey() {
            return mKey;
        }
    }

}
