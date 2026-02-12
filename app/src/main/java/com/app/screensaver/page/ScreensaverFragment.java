package com.app.screensaver.page;

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

import com.app.screensaver.ImageCallbacks;
import com.app.screensaver.MainActivity;
import com.app.screensaver.R;
import com.app.screensaver.TimerService;

import java.io.IOException;

public class ScreensaverFragment extends Fragment {
    public static final String SIZE_EXTRA               = "screensaver.SIZE_EXTRA";
    public static final String FONT_RES_ID_EXTRA        = "screensaver.FONT_RES_ID_EXTRA";
    public static final String COLOR_EXTRA              = "screensaver.COLOR_EXTRA";
    public static final String TIME_EXTRA               = "screensaver.TIME_EXTRA";
    public static final String USE_24_HOUR_FORMAT_EXTRA = "screensaver.USE_24_HOUR_FORMAT";

    public static final String ACTION_UPDATE_TIME       = "screensaver.UPDATE_TIME";

    public static final String DEFAULT_PREFERENCES_NAME = "screensaver.prefs.MAIN_PREFS";
    public static final String CLOCK_VISIBLE_EXTRA      = "screensaver.conf.TIME_VISIBLE";
    public static final String SECONDS_VISIBLE_EXTRA    = "screensaver.conf.SECONDS_VISIBLE";
    public static final String TIMER_VISIBLE_EXTRA      = "screensaver.conf.TIMER_VISIBLE";
    public static final String BATTERY_VISIBLE_EXTRA    = "screensaver.conf.BATTERY_VISIBLE";

    // text
    private TextClock mClockView;
    private TextView mTimerView, mBatteryView;
    // image & buttons
    private ImageView mImageView;
    private Button mStopButton, mStartButton, mResetButton;

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

    // listening to event UPDATE_TIME
    private final BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
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

        ContextCompat.registerReceiver(requireContext(), mTimeReceiver, new IntentFilter(ACTION_UPDATE_TIME), ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(requireContext(), mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);

        mResetButton = view.findViewById(R.id.reset_timer_button);
        mStopButton = view.findViewById(R.id.stop_timer_button);
        mStartButton = view.findViewById(R.id.start_timer_button);

        mStartButton.setOnClickListener(new TimerListener(TimerService.ACTION_START));
        mStopButton.setOnClickListener(new TimerListener(TimerService.ACTION_STOP));
        mResetButton.setOnClickListener(new TimerListener(TimerService.ACTION_RESET));

        PreferencesManager man = new PreferencesManager();
        man.loadSavedPreferences(requireContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().unregisterReceiver(mBatteryReceiver);
        requireContext().unregisterReceiver(mTimeReceiver);
    }

    private final class PreferencesManager {
        public void loadSavedPreferences(Context context) {
            SharedPreferences preferences = context.getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);

            // load size
            int clockSize = preferences.getInt(SIZE_EXTRA, 270);
            mClockView.setTextSize(clockSize);

            loadImage();
            loadColor(preferences);
            loadFont(context, preferences);
            updateVisibility(context);
        }

        private void loadImage() {
            try {
                // noinspection all : NullPointerException will get caught -> android studio false-positive
                ImageDecoder.Source source = ImageDecoder.createSource(ImageCallbacks.sImagePath.toFile());
                Bitmap bitmap = ImageDecoder.decodeBitmap(source);
                mImageView.setImageBitmap(bitmap);
            } catch (IOException | NullPointerException e) {
                Log.d("exception_in_screensaver", "Exception thrown: "+e);
            }
        }

        private void loadColor(SharedPreferences preferences) {
            int loadedFontColor=preferences.getInt(COLOR_EXTRA, Color.WHITE);
            int textColor =loadedFontColor!=0?loadedFontColor:Color.WHITE;
            mClockView.setTextColor(textColor);
            mBatteryView.setTextColor(textColor);
            mTimerView.setTextColor(textColor);
            mStopButton.setTextColor(textColor);
            mStartButton.setTextColor(textColor);
            mResetButton.setTextColor(textColor);
        }

        private void loadFont(Context context, SharedPreferences preferences) {
            int loadedFontResId = preferences.getInt(FONT_RES_ID_EXTRA, R.font.libertinusserif_regular);
            int fontId = loadedFontResId!=0?loadedFontResId:R.font.libertinusserif_regular;
            Typeface typeface = ResourcesCompat.getFont(context, fontId);
            mClockView.setTypeface(typeface);
            mTimerView.setTypeface(typeface);
            mBatteryView.setTypeface(typeface);
            mResetButton.setTypeface(typeface);
            mStopButton.setTypeface(typeface);
            mStartButton.setTypeface(typeface);
        }

        private void updateVisibility(Context context) {
            SharedPreferences preferences = context.getSharedPreferences(ScreensaverFragment.DEFAULT_PREFERENCES_NAME, Context.MODE_PRIVATE);
            boolean battery=preferences.getBoolean(BATTERY_VISIBLE_EXTRA, true);
            boolean clock=preferences.getBoolean(CLOCK_VISIBLE_EXTRA, true);
            boolean timer=preferences.getBoolean(TIMER_VISIBLE_EXTRA, true);
            boolean seconds=preferences.getBoolean(SECONDS_VISIBLE_EXTRA, true);

            mBatteryView.setVisibility(toInt(battery));
            mClockView.setVisibility(toInt(clock));

            int timerVisibility=toInt(timer);
            mTimerView.setVisibility(timerVisibility);
            mStopButton.setVisibility(timerVisibility);
            mStartButton.setVisibility(timerVisibility);
            mResetButton.setVisibility(timerVisibility);

            boolean using24HourFormat = preferences.getBoolean(ScreensaverFragment.USE_24_HOUR_FORMAT_EXTRA, true);
            String format12 = seconds?"hh:mm:ss":"hh:mm";
            String format24 = seconds?"HH:mm:ss":"HH:mm";
            if(using24HourFormat) {
                mClockView.setFormat24Hour(format24);
                mClockView.setFormat12Hour(format24);
            } else {
                mClockView.setFormat24Hour(format12);
                mClockView.setFormat12Hour(format12);
            }
        }

        private int toInt(boolean bool) {
            return bool ? View.VISIBLE : View.GONE;
        }
    }

    private final class TimerListener implements View.OnClickListener {
        private final String mAction;

        private TimerListener(String action) {
            this.mAction = action;
        }

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(requireContext(), TimerService.class);
            intent.setAction(mAction);
            requireContext().startForegroundService(intent);
        }
    }
}
