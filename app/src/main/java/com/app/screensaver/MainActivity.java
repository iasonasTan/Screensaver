package com.app.screensaver;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import com.app.screensaver.page.ScreensaverFragment;
import com.app.screensaver.page.SettingsFragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String ACTION_CHANGE_FRAGMENT = "screensaver.main.CHANGE_FRAGMENT";
    public static final String FRAGMENT_CLASS_EXTRA   = "screensaver.main.FRAGMENT_CLASS";

    private final List<Fragment> mFragmentsList = new ArrayList<>();

    // listening on action CHANGE_FRAGMENT
    private final BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
            try {
                String className=intent.getStringExtra(FRAGMENT_CLASS_EXTRA);
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

        try {
            ImageCallbacks.init(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mFragmentsList.add(new ScreensaverFragment());
        mFragmentsList.add(new SettingsFragment());

        ContextCompat.registerReceiver(this, broadcastReceiver, new IntentFilter(ACTION_CHANGE_FRAGMENT), ContextCompat.RECEIVER_NOT_EXPORTED);

        requestPermissions();

        if(savedInstanceState == null)
            setFragment(ScreensaverFragment.class);
    }

    private void requestPermissions() {
        if(checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 1001);
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1002);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    public void setFragment(Class<? extends Fragment> fragmentClass) throws NoSuchElementException {
        Fragment fragmentToShow = getFragment(fragmentClass);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragmentToShow)
                .commit();
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
        setShowWhenLocked(true);
        setTurnScreenOn(true);
    }

    @Deprecated(forRemoval = true)
    @SuppressWarnings("unused")
    private void checkForUpdates() {
        try {
            Log.d("net-test", "Checking for updates...");
            // noinspection all
            Socket socket = new Socket("iasonas.duckdns.org", 1422);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write("get_version_code?screensaver");
            writer.append('\n');
            writer.flush();
            String latest_version_code = reader.readLine();
            Log.d("net-test", "Latest version code is " + latest_version_code);
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            Log.d("net-test", "Checking package info...");
            if (info != null && info.versionName != null) {
                Log.d("net-test", "Comparing version codes...");
                double latestVersion = Double.parseDouble(latest_version_code);
                double appVersion = Double.parseDouble(info.versionName);
                Log.d("net-test", "Latest version: " + latestVersion);
                Log.d("net-test", "App version: " + appVersion);
                if (latestVersion > appVersion) {
                    Log.d("net-test", "Asking user to update...");
                    runOnUiThread(() -> new AlertDialog.Builder(this)
                            .setTitle("Update available!")
                            .setMessage("Do you want to update?")
                            .setCancelable(false)
                            .setNegativeButton("Not now.", (a, b) -> a.dismiss())
                            .setPositiveButton("Yes!", (a, b) -> {
                                a.dismiss();
                                String url = "http://iasonas.duckdns.org/download_screensaver/index.html";
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            })
                            .show());
                }
            }
            reader.close();
            writer.write("disconnect");
            writer.append('\n');
            writer.flush();
            writer.close();
            socket.close();
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.d("net-test", "Exception: "+e);
        }
    }

}