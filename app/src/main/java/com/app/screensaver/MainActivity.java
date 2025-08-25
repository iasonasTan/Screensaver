package com.app.screensaver;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private final List<Fragment> mFragmentsList =new ArrayList<>();

    // listening on action CHANGE_FRAGMENT
    private final BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
            try {
                String className=intent.getStringExtra("fragmentClass");
                Class<?> pulledClass=Class.forName(Objects.requireNonNull(className));
                Class<? extends Fragment> fragmentClass=(Class<? extends Fragment>)pulledClass;
                setFragment(fragmentClass);
            } catch (ClassNotFoundException | ClassCastException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public <T extends Fragment> T getFragment(Class<T> fragmentClazz) throws NoSuchElementException {
        for(Fragment fragment: mFragmentsList) {
            if(fragmentClazz.isAssignableFrom(fragment.getClass())) {
                return fragmentClazz.cast(fragment);
            }
        }
        throw new NoSuchElementException("No such element of type "+fragmentClazz.getTypeName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        configSystemUI();

        mFragmentsList.add(new ScreensaverFragment());
        mFragmentsList.add(new SettingsFragment());

        IntentFilter intentFilter=new IntentFilter("CHANGE_FRAGMENT");
        ContextCompat.registerReceiver(this, broadcastReceiver, intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        setFragment(ScreensaverFragment.class);
        getFragment(ScreensaverFragment.class).loadSavedPreferences(getApplicationContext());
        grantPermissions();
    }

    private void grantPermissions() {
        if(checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1002);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        for (BroadcastReceiver receiver : getFragment(ScreensaverFragment.class).getReceivers()) {
            unregisterReceiver(receiver);
        }
    }

    public void setFragment(Class<? extends Fragment> fragmentClass) throws NoSuchElementException {
        getSupportFragmentManager().beginTransaction().replace(R.id.main,
                Objects.requireNonNull(getFragment(fragmentClass))).commit();
    }

    private void configSystemUI() {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

}