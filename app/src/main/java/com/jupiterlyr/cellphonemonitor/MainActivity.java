package com.jupiterlyr.cellphonemonitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.jupiterlyr.cellphonemonitor.monitor.BatteryMonitor;
import com.jupiterlyr.cellphonemonitor.monitor.SystemMonitor;

public class MainActivity extends AppCompatActivity {

    private MainViewBinder viewBinder;

    private BatteryMonitor batteryMonitor;
    private SystemMonitor systemMonitor;

    private static final int REQ_POST_NOTIFICATIONS = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        viewBinder = new MainViewBinder(this);

        batteryMonitor = new BatteryMonitor(this);
        systemMonitor = new SystemMonitor(this);

        ensureNotificationPermission();
    }

    private void ensureNotificationPermission() {
        // Android 13+ 需要运行时申请通知权限，否则前台服务的通知不会展示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_POST_NOTIFICATIONS
                );
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        viewBinder.startTimeTicker();

        batteryMonitor.start(snapshot -> runOnUiThread(() -> {
            viewBinder.renderBattery(snapshot);
        }));
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewBinder.stopTimeTicker();
        batteryMonitor.stop();
        systemMonitor.stop();
    }
}