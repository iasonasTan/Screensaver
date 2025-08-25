package com.app.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.Objects;
import java.util.function.Function;

public class ScreensaverFragment extends Fragment {
    // text
    private TextClock mClockView;
    private TextView mTimerView, mBatteryView;
    // image & buttons
    private ImageView mImageView;
    private Button mStopButton, mStartButton, mResetButton;
    // configuration
    private int mFontId, mTextColor;
    private Uri mImageUri;
    static VisibilityConfiguration mVisibilityConfig;

    // listening on event 'android.intent.action.BATTERY_CHANGED'
    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int)((level / (float)scale) * 100);
            // noinspection all
            mBatteryView.setText(batteryPct + "%");
        }
    };

    // listening on action CHANGE_FONT, REMOVE_IMAGE, CHANGE_COLOR, CHANGE_VISIBILITY and CHANGE_IMAGE
    private final BroadcastReceiver mBroadcastReceiver =new BroadcastReceiver() {
        /*
        stores taken values into variables and applies them on resume.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(Objects.requireNonNull(intent.getAction())) {
                case "CHANGE_FONT":
                    mFontId = intent.getIntExtra("fontID", R.font.libertinusserif_regular);
                    break;
                case "CHANGE_IMAGE":
                    mImageUri = Uri.parse(intent.getStringExtra("imageUri"));
                    Log.d("dev-test", "Received image uri "+mImageUri.toString());
                    break;
                case "REMOVE_IMAGE":
                    mImageUri = null;
                    Log.d("dev-test", "Removed background image");
                    break;
                case "CHANGE_COLOR":
                    mTextColor = intent.getIntExtra("color", Color.WHITE);
                    Log.d("dev-test", "Changed clock color to "+ mTextColor);
                    break;
                case "CHANGE_VISIBILITY":
                    mVisibilityConfig = intent.getParcelableExtra("configuration", VisibilityConfiguration.class);
                    break;
            }
            saveSelectedPreferences(context);
        }
    };

    // listening to event UPDATE_TIME
    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTimerView.setText(TimerService.longToTime(intent.getLongExtra("time", 0)));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_screensaver, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mClockView=view.findViewById(R.id.text_clock);
        mImageView=view.findViewById(R.id.image_view);
        mTimerView=view.findViewById(R.id.timer_textview);
        mBatteryView=view.findViewById(R.id.battery_textview);
        view.findViewById(R.id.settings_button).setOnClickListener(ignored -> {
            Intent intent=new Intent("CHANGE_FRAGMENT").setPackage(requireContext().getPackageName());
            intent.putExtra("fragmentClass", SettingsFragment.class.getName());
            requireContext().sendBroadcast(intent);
        });
        IntentFilter intentFilter=new IntentFilter("CHANGE_FONT");
        intentFilter.addAction("CHANGE_IMAGE");
        intentFilter.addAction("REMOVE_IMAGE");
        intentFilter.addAction("CHANGE_COLOR");
        intentFilter.addAction("CHANGE_VISIBILITY");
        ContextCompat.registerReceiver(requireContext(), mBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter timeReceiverFilter=new IntentFilter("UPDATE_TIME");
        ContextCompat.registerReceiver(requireContext(), mTimeReceiver, timeReceiverFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        requireContext().registerReceiver(mBatteryReceiver, filter);

        mResetButton = view.findViewById(R.id.reset_timer_button);
        mStopButton = view.findViewById(R.id.stop_timer_button);
        mStartButton = view.findViewById(R.id.start_timer_button);
        mStartButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction("START");
            requireContext().startService(intent);
        });
        mStopButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction("PAUSE");
            requireContext().startService(intent);
        });
        mResetButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction("RESET");
            requireContext().startService(intent);
        });
    }

    void setTextColor() {
        // clock & battery
        mClockView.setTextColor(mTextColor);
        mBatteryView.setTextColor(mTextColor);
        // timer
        mTimerView.setTextColor(mTextColor);
        mStopButton.setTextColor(mTextColor);
        mStartButton.setTextColor(mTextColor);
        mResetButton.setTextColor(mTextColor);
    }

    void updateBackgroundImage(Context context) {
        if(mImageUri==null) {
            mImageView.setImageBitmap(null);
            return;
        }
        try {
            ImageDecoder.Source imageSource = ImageDecoder.createSource(context.getContentResolver(), mImageUri);
            Bitmap bitmap = ImageDecoder.decodeBitmap(imageSource);
            mImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void saveSelectedPreferences(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor=preferences.edit();
        preferencesEditor.putString("background_image", mImageUri!=null?mImageUri.toString():"null");
        if(mFontId!=0) preferencesEditor.putInt("font", mFontId);
        if(mTextColor !=0) preferencesEditor.putInt("color", mTextColor);
        if(mVisibilityConfig!=null){
            preferencesEditor.putBoolean("clockVisible", mVisibilityConfig.clock);
            preferencesEditor.putBoolean("timerVisible", mVisibilityConfig.timer);
            preferencesEditor.putBoolean("batteryVisible", mVisibilityConfig.battery);
        }
        preferencesEditor.apply();
    }

    void loadSavedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        // load image
        String resourceString=prefs.getString("background_image", null);
        mImageUri = resourceString != null && !resourceString.equals("null") ?
                Uri.parse(prefs.getString("background_image", null)) : null;
        // load font
        int loadedFontResId = prefs.getInt("font", R.font.libertinusserif_regular);
        mFontId = loadedFontResId!=0?loadedFontResId:R.font.libertinusserif_regular;
        // load color
        int loadedFontColor=prefs.getInt("color", Color.WHITE);
        mTextColor =loadedFontColor!=0?loadedFontColor:Color.WHITE;
        // load visibility config
        boolean clock=prefs.getBoolean("clockVisible", true);
        boolean battery=prefs.getBoolean("batteryVisible", true);
        boolean timer=prefs.getBoolean("timerVisible", true);
        mVisibilityConfig=new VisibilityConfiguration(clock, battery, timer);
    }

    void updateTypefaces() {
        Typeface typeface = ResourcesCompat.getFont(requireContext(), mFontId);
        mClockView.setTypeface(typeface);
        mTimerView.setTypeface(typeface);
        mBatteryView.setTypeface(typeface);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTypefaces();
        setTextColor();
        updateVisibility();
        updateBackgroundImage(requireContext());
    }

    private void updateVisibility() {
        if(mVisibilityConfig==null)return;
        Function<Boolean, Integer> getVisibility = b -> b ? View.VISIBLE : View.GONE;

        mBatteryView.setVisibility(getVisibility.apply(mVisibilityConfig.battery));
        mClockView.setVisibility(getVisibility.apply(mVisibilityConfig.clock));
        int timerVisibility=getVisibility.apply(mVisibilityConfig.timer);
        mTimerView.setVisibility(timerVisibility);
        mStopButton.setVisibility(timerVisibility);
        mStartButton.setVisibility(timerVisibility);
        mResetButton.setVisibility(timerVisibility);
    }

    public BroadcastReceiver[] getReceivers() {
        return new BroadcastReceiver[]{mBroadcastReceiver, mTimeReceiver, mBatteryReceiver};
    }
}
