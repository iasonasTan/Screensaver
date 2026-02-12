package com.app.screensaver.page;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.app.screensaver.ImageCallbacks;
import com.app.screensaver.MainActivity;
import com.app.screensaver.R;
import com.app.screensaver.view.ContainerView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import yuku.ambilwarna.AmbilWarnaDialog;

public class SettingsFragment extends Fragment {
    private CheckBox mClockCB, mTimerCB, mBatteryCB, mSecsCB, mUse24HourCB;
    private boolean mIsUserSelection = false;
    private ImageCallbacks mImageCallbacks;

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
        mSecsCB = view.findViewById(R.id.show_seconds);
        mUse24HourCB = view.findViewById(R.id.use_24_hour_format);

        mImageCallbacks = new ImageCallbacks(requireContext(), this);

        initFontSpinner(view);
        setOnClickListeners(view);

        requireActivity().getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Intent intent = new Intent(MainActivity.ACTION_CHANGE_FRAGMENT).setPackage(requireContext().getPackageName());
                intent.putExtra(MainActivity.FRAGMENT_CLASS_EXTRA, ScreensaverFragment.class.getName());
                requireContext().sendBroadcast(intent);
            }
        });

        SharedPreferences preferences = requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mTimerCB.setChecked(preferences.getBoolean(ScreensaverFragment.TIMER_VISIBLE_EXTRA, true));
        mClockCB.setChecked(preferences.getBoolean(ScreensaverFragment.CLOCK_VISIBLE_EXTRA, true));
        mBatteryCB.setChecked(preferences.getBoolean(ScreensaverFragment.BATTERY_VISIBLE_EXTRA, true));
        mSecsCB.setChecked(preferences.getBoolean(ScreensaverFragment.SECONDS_VISIBLE_EXTRA, true));
        mUse24HourCB.setChecked(preferences.getBoolean(ScreensaverFragment.USE_24_HOUR_FORMAT_EXTRA, true));

        int sizeSp = preferences.getInt(ScreensaverFragment.SIZE_EXTRA, 170);
        SeekBar bar = view.findViewById(R.id.clock_size_bar);
        bar.setProgress(sizeSp);
    }

    private void setOnClickListeners(View view) {
        view.findViewById(R.id.back_button).setOnClickListener(ignored -> {
            savePreferencesFromGui();
            Intent intent = new Intent(MainActivity.ACTION_CHANGE_FRAGMENT).setPackage(requireContext().getPackageName());
            intent.putExtra(MainActivity.FRAGMENT_CLASS_EXTRA, ScreensaverFragment.class.getName());
            requireContext().sendBroadcast(intent);
        });

        view.findViewById(R.id.select_image_button).setOnClickListener(mImageCallbacks);
        view.findViewById(R.id.remove_image_button).setOnClickListener(mImageCallbacks);

        SeekBar seekBar = view.findViewById(R.id.clock_size_bar);
        seekBar.setOnSeekBarChangeListener(new SeekbarListener());

        SharedPreferences preferences = requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        int previousUsedColor = preferences.getInt(ScreensaverFragment.COLOR_EXTRA, Color.WHITE);
        view.findViewById(R.id.change_color_button).setOnClickListener(v -> {
            v.setEnabled(false);
            Log.d("colorPicker", "Opening color picker...");
            new yuku.ambilwarna.AmbilWarnaDialog(requireContext(), previousUsedColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override public void onCancel(AmbilWarnaDialog ignored) { /* Blank method */ }
                @Override public void onOk(AmbilWarnaDialog dialog, int color) {
                    requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                            .putInt(ScreensaverFragment.COLOR_EXTRA, color)
                            .apply();
                    v.setEnabled(true);
                }
            }).show();
        });
    }

    private void savePreferencesFromGui() {
        requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(ScreensaverFragment.BATTERY_VISIBLE_EXTRA, mBatteryCB.isChecked())
                .putBoolean(ScreensaverFragment.CLOCK_VISIBLE_EXTRA, mClockCB.isChecked())
                .putBoolean(ScreensaverFragment.TIMER_VISIBLE_EXTRA, mTimerCB.isChecked())
                .putBoolean(ScreensaverFragment.SECONDS_VISIBLE_EXTRA, mSecsCB.isChecked())
                .putBoolean(ScreensaverFragment.USE_24_HOUR_FORMAT_EXTRA, mUse24HourCB.isChecked())
                .apply();
    }

    private void initFontSpinner(View rootView) {
        Spinner fontSpinner = rootView.findViewById(R.id.font_spinner);
        Field[] fontResourceFields=R.font.class.getFields();
        List<Integer> fontIDs = new ArrayList<>();
        for (int i = 0; i < fontResourceFields.length; i++) {
            try {
                fontResourceFields[i].setAccessible(true);
                fontIDs.add(fontResourceFields[i].getInt(null));
                Log.d("dev-test", "Added font "+fontIDs.get(i));
            } catch (IllegalAccessException e) {
                Log.d("error", "Error while trying to access font field." + e.getMessage());
            }
        }

        List<ContainerView> items = new ArrayList<>();
        for (int id : fontIDs) {
            ContainerView containerView = new ContainerView(requireContext(), id);
            containerView.setText(getResources().getResourceEntryName(id)); // show font name
            items.add(containerView);
        }
        ArrayAdapter<ContainerView> adapter = getContainerViewArrayAdapter(items, fontIDs.toArray(new Integer[0]));
        fontSpinner.setAdapter(adapter);

        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(!mIsUserSelection) {
                    mIsUserSelection = true;
                    return;
                }

                ContainerView containerView = (ContainerView) parent.getItemAtPosition(position);
                requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                        .putInt(ScreensaverFragment.FONT_RES_ID_EXTRA, containerView.getKey())
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing
            }
        });

        SharedPreferences preferences = requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        int savedFontId = preferences.getInt(ScreensaverFragment.FONT_RES_ID_EXTRA, R.font.libertinus_serif_bold);
        int savedFontIndex = fontIDs.indexOf(savedFontId);
        fontSpinner.setSelection(savedFontIndex);
    }

    @NonNull
    private ArrayAdapter<ContainerView> getContainerViewArrayAdapter(List<ContainerView> items, Integer[] fontIDs) {
        ArrayAdapter<ContainerView> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, items) {
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTypeface(ResourcesCompat.getFont(getContext(), fontIDs[position]));
                return tv;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTypeface(ResourcesCompat.getFont(getContext(), fontIDs[position]));
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private final class SeekbarListener implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            requireContext().getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                    .putInt(ScreensaverFragment.SIZE_EXTRA, bar.getProgress())
                    .apply();
        }

        @Override
        public void onStartTrackingTouch(SeekBar ignored) {
            // nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar ignored) {
            // nothing
        }
    }
}
