package com.app.screensaver;

import android.app.KeyguardManager;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import java.util.Objects;
import java.util.function.Function;

public class ScreensaverFragment extends Fragment {
    static final String ACTION_CHANGE_FONT       = "screensaver.CHANGE_FONT";
    static final String FONT_RES_ID_EXTRA        = "screensaver.FONT_RES_ID_EXTRA";
    static final String ACTION_CHANGE_IMAGE      = "screensaver.CHANGE_IMAGE";
    static final String IMAGE_URI_EXTRA          = "screensaver.IMAGE_URI_EXTRA";
    static final String ACTION_CHANGE_COLOR      = "screensaver.CHANGE_COLOR";
    static final String COLOR_EXTRA              = "screensaver.COLOR_EXTRA";
    static final String ACTION_CHANGE_VISIBILITY = "screensaver.CHANGE_VISIBILITY";
    static final String VISIBILITY_CONFIG_EXTRA  = "screensaver.VISIBILITY_CONFIG_EXTRA";
    static final String ACTION_REMOVE_IMAGE      = "screensaver.REMOVE_IMAGE";
    static final String ACTION_UPDATE_TIME       = "screensaver.UPDATE_TIME";
    static final String TIME_EXTRA               = "screensaver.TIME_EXTRA";
    static final String DEFAULT_PREFERENCES_NAME = "screensaver.prefs.MAIN_PREFS";
    static final String CLOCK_VISIBLE_EXTRA      = "screensaver.conf.TIME_VISIBLE";
    static final String SECONDS_VISIBLE_EXTRA    = "screensaver.conf.SECONDS_VISIBLE";
    static final String TIMER_VISIBLE_EXTRA      = "screensaver.conf.TIMER_VISIBLE";
    static final String BATTERY_VISIBLE_EXTRA    = "screensaver.conf.BATTERY_PERC";

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
        /* stores taken values into variables and applies them on resume. */
        @Override
        public void onReceive(Context context, Intent intent) {
            switch(Objects.requireNonNull(intent.getAction())) {
                case ACTION_CHANGE_FONT:
                    mFontId = intent.getIntExtra(FONT_RES_ID_EXTRA, R.font.libertinusserif_regular);
                    break;
                case ACTION_CHANGE_IMAGE:
                    mImageUri = Uri.parse(intent.getStringExtra(IMAGE_URI_EXTRA));
                    Log.d("dev-test", "Received image uri "+mImageUri.toString());
                    break;
                case ACTION_REMOVE_IMAGE:
                    mImageUri = null;
                    Log.d("dev-test", "Removed background image");
                    break;
                case ACTION_CHANGE_COLOR:
                    mTextColor = intent.getIntExtra(COLOR_EXTRA, Color.WHITE);
                    Log.d("dev-test", "Changed clock color to "+ mTextColor);
                    break;
                case ACTION_CHANGE_VISIBILITY:
                    mVisibilityConfig = intent.getParcelableExtra(VISIBILITY_CONFIG_EXTRA, VisibilityConfiguration.class);
                    Log.d("visibility_update", "visibility config updated: "+mVisibilityConfig);
                    break;
            }
            saveSelectedPreferences(context);
        }
    };

    // listening to event UPDATE_TIME
    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mTimerView.setText(TimerService.longToTime(intent.getLongExtra(TIME_EXTRA, 0)));
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
            KeyguardManager keyguardManager = requireContext().getSystemService(KeyguardManager.class);
            if(!keyguardManager.isDeviceLocked()) {
                Intent intent = new Intent(MainActivity.ACTION_CHANGE_FRAGMENT).setPackage(requireContext().getPackageName());
                intent.putExtra(MainActivity.FRAGMENT_CLASS_EXTRA, SettingsFragment.class.getName());
                requireContext().sendBroadcast(intent);
            } else {
                Log.w("dev-test", "Cannot access settings because device is locked!");
                Toast.makeText(requireContext(), R.string.settings_access_denied, Toast.LENGTH_LONG).show();
            }
        });
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ACTION_CHANGE_FONT);
        intentFilter.addAction(ACTION_CHANGE_IMAGE);
        intentFilter.addAction(ACTION_REMOVE_IMAGE);
        intentFilter.addAction(ACTION_CHANGE_COLOR);
        intentFilter.addAction(ACTION_CHANGE_VISIBILITY);
        ContextCompat.registerReceiver(requireContext(), mBroadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter timeReceiverFilter=new IntentFilter(ACTION_UPDATE_TIME);
        ContextCompat.registerReceiver(requireContext(), mTimeReceiver, timeReceiverFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        requireContext().registerReceiver(mBatteryReceiver, filter);
        ContextCompat.registerReceiver(requireContext(), mBatteryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

        mResetButton = view.findViewById(R.id.reset_timer_button);
        mStopButton = view.findViewById(R.id.stop_timer_button);
        mStartButton = view.findViewById(R.id.start_timer_button);

        mStartButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction(TimerService.ACTION_START);
            requireContext().startService(intent);
        });
        mStopButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction(TimerService.ACTION_STOP);
            requireContext().startService(intent);
        });
        mResetButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction(TimerService.ACTION_RESET);
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
        SharedPreferences preferences = context.getSharedPreferences(DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor preferencesEditor=preferences.edit();
        preferencesEditor.putString(IMAGE_URI_EXTRA, mImageUri!=null?mImageUri.toString():"null");
        if(mFontId!=0) preferencesEditor.putInt(FONT_RES_ID_EXTRA, mFontId);
        if(mTextColor !=0) preferencesEditor.putInt(COLOR_EXTRA, mTextColor);
        if(mVisibilityConfig!=null){
            preferencesEditor.putBoolean(CLOCK_VISIBLE_EXTRA, mVisibilityConfig.clock);
            preferencesEditor.putBoolean(TIMER_VISIBLE_EXTRA, mVisibilityConfig.timer);
            preferencesEditor.putBoolean(BATTERY_VISIBLE_EXTRA, mVisibilityConfig.battery);
            preferencesEditor.putBoolean(SECONDS_VISIBLE_EXTRA, mVisibilityConfig.seconds);
        }
        preferencesEditor.apply();
    }

    void loadSavedPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
        // load image
        String resourceString=prefs.getString(IMAGE_URI_EXTRA, null);
        mImageUri = resourceString != null && !resourceString.equals("null") ?
                Uri.parse(prefs.getString(IMAGE_URI_EXTRA, null)) : null;
        // load font
        int loadedFontResId = prefs.getInt(FONT_RES_ID_EXTRA, R.font.libertinusserif_regular);
        mFontId = loadedFontResId!=0?loadedFontResId:R.font.libertinusserif_regular;
        // load color
        int loadedFontColor=prefs.getInt(COLOR_EXTRA, Color.WHITE);
        mTextColor =loadedFontColor!=0?loadedFontColor:Color.WHITE;
        // load visibility config
        boolean clock=prefs.getBoolean(CLOCK_VISIBLE_EXTRA, true);
        boolean battery=prefs.getBoolean(BATTERY_VISIBLE_EXTRA, true);
        boolean timer=prefs.getBoolean(TIMER_VISIBLE_EXTRA, true);
        boolean seconds=prefs.getBoolean(SECONDS_VISIBLE_EXTRA, true);
        mVisibilityConfig=new VisibilityConfiguration(clock, battery, timer, seconds);
    }

    void updateTypefaces() {
        Typeface typeface = ResourcesCompat.getFont(requireContext(), mFontId);
        mClockView.setTypeface(typeface);
        mTimerView.setTypeface(typeface);
        mBatteryView.setTypeface(typeface);
        mResetButton.setTypeface(typeface);
        mStopButton.setTypeface(typeface);
        mStartButton.setTypeface(typeface);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTypefaces();
        setTextColor();
        updateVisibility();
        updateBackgroundImage(requireContext());
        mClockView.setFormat12Hour(mVisibilityConfig.seconds?"hh:mm:ss":"hh:mm");
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
