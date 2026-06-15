package com.jupiterlyr.cellphonemonitor;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.jupiterlyr.cellphonemonitor.monitor.BatteryMonitor;
import com.jupiterlyr.cellphonemonitor.monitor.SystemMonitor;

public class MainActivity extends AppCompatActivity {

    private MainViewBinder viewBinder;
    private BatteryMonitor batteryMonitor;
    private SystemMonitor systemMonitor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewBinder = new MainViewBinder(this);
        batteryMonitor = new BatteryMonitor(this);
        systemMonitor = new SystemMonitor(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewBinder.startTimeTicker();
        batteryMonitor.start(snapshot -> runOnUiThread(() -> viewBinder.renderBattery(snapshot)));
        systemMonitor.start(stats -> runOnUiThread(() -> viewBinder.renderSystemStats(stats)));
    }

    @Override
    protected void onStop() {
        batteryMonitor.stop();
        systemMonitor.stop();
        viewBinder.stopTimeTicker();
        super.onStop();
    }
}
