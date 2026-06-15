package com.jupiterlyr.cellphonemonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.jupiterlyr.cellphonemonitor.monitor.BatterySnapshot;
import com.jupiterlyr.cellphonemonitor.monitor.SystemStats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainViewBinder {

    private final Activity activity;
    private final TextView tvTime;
    private final TextView tvBattery;
    private final TextView tvCharging;
    private final TextView tvBatteryTemp;
    private final ProgressBar progressBattery;
    private final TextView tvThermalStatus;
    private final TextView tvBatteryCurrent;
    private final TextView tvBatteryVoltage;
    private final TextView tvBatteryPower;
    private final TextView tvRefreshRate;
    private final TextView tvCpuTemp;
    private final TextView tvCpuFreq;
    private final TextView tvMemoryLoad;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH : mm : ss", Locale.getDefault());

    private final Runnable timeTicker = new Runnable() {
        @Override
        public void run() {
            tvTime.setText(timeFormat.format(new Date()));
            mainHandler.postDelayed(this, 1000L);
        }
    };

    public MainViewBinder(@NonNull Activity activity) {
        this.activity = activity;
        tvTime = activity.findViewById(R.id.tvTime);
        tvBattery = activity.findViewById(R.id.tvBattery);
        tvCharging = activity.findViewById(R.id.tvCharging);
        tvBatteryTemp = activity.findViewById(R.id.tvBatteryTemp);
        progressBattery = activity.findViewById(R.id.progressBattery);
        tvThermalStatus = activity.findViewById(R.id.tvThermalStatus);
        tvBatteryCurrent = activity.findViewById(R.id.tvBatteryCurrent);
        tvBatteryVoltage = activity.findViewById(R.id.tvBatteryVoltage);
        tvBatteryPower = activity.findViewById(R.id.tvBatteryPower);
        tvRefreshRate = activity.findViewById(R.id.tvRefreshRate);
        tvCpuTemp = activity.findViewById(R.id.tvCpuTemp);
        tvCpuFreq = activity.findViewById(R.id.tvCpuFreq);
        tvMemoryLoad = activity.findViewById(R.id.tvMemoryload);
    }

    public void startTimeTicker() {
        mainHandler.removeCallbacks(timeTicker);
        mainHandler.post(timeTicker);
    }

    public void stopTimeTicker() {
        mainHandler.removeCallbacks(timeTicker);
    }

    @SuppressLint("SetTextI18n")
    public void renderBattery(@NonNull BatterySnapshot snapshot) {
        int level = snapshot.getBatteryLevel();
        tvBattery.setText(level + "%");
        progressBattery.setProgress(level);
        progressBattery.setProgressTintList(
                ColorStateList.valueOf(ContextCompat.getColor(activity, batteryProgressColorRes(level)))
        );
        tvCharging.setText(chargingValue(snapshot.isCharging(), level));
        tvBatteryTemp.setText(String.format(Locale.getDefault(), "%.1f ℃", snapshot.getBatteryTempC()));
    }

    @SuppressLint("SetTextI18n")
    public void renderSystemStats(@NonNull SystemStats stats) {
        tvThermalStatus.setText(thermalStatusText(stats.getThermalStatus()));
        float batteryCurrent = stats.getBatteryCurrentMa();
        if (batteryCurrent > 0) {
            tvBatteryCurrent.setText(String.format(Locale.getDefault(), "%.0f mA (充)", batteryCurrent));
        } else {
            tvBatteryCurrent.setText(String.format(Locale.getDefault(), "%.0f mA (放)", Math.abs(batteryCurrent)));
        }
        tvBatteryVoltage.setText(String.format(Locale.getDefault(), "%.0f mV", stats.getBatteryVoltageMv()));
        tvBatteryPower.setText(String.format(Locale.getDefault(), "%.0f mW", Math.abs(stats.getBatteryPowerMw())));
        if (stats.getRefreshRateHz() > 0f) {
            tvRefreshRate.setText(String.format(Locale.getDefault(), "%.0f Hz", stats.getRefreshRateHz()));
        } else {
            tvRefreshRate.setText("-");
        }
        tvCpuTemp.setText(String.format(Locale.getDefault(), "%.1f ℃", stats.getCpuTemperatureC()));
        if (stats.getCpuFreqMhz() > 0f) {
            tvCpuFreq.setText(String.format(Locale.getDefault(), "%.0f MHz (%.0f%%)",
                    stats.getCpuFreqMhz(), stats.getCpuFreqRatio()));
        } else {
            tvCpuFreq.setText("— MHz (—%)");
        }
        long memUsed = stats.getMemoryUsedBytes();
        long memTotal = stats.getMemoryTotalBytes();
        if (memTotal > 0L) {
            tvMemoryLoad.setText(String.format(Locale.getDefault(), "%s / %s",
                    formatMb(memUsed), formatMb(memTotal)));
        } else {
            tvMemoryLoad.setText("— MB / — MB");
        }
    }

    @ColorRes
    private static int batteryProgressColorRes(int level) {
        if (level <= 20) {
            return R.color.red_main;
        }
        if (level <= 40) {
            return R.color.yellow_main;
        }
        return R.color.green_main;
    }

    private static String chargingValue(boolean charging, int level) {
        if (charging) {
            return level == 100 ? "充电已完成" : "充电中";
        }
        return level > 20 ? "未充电" : "电量过低";
    }

    private static String thermalStatusText(int status) {
        switch (status) {
            case 0:
                return "正常";
            case 1:
                return "轻度发热";
            case 2:
                return "中度发热";
            case 3:
                return "严重发热";
            case 4:
                return "危险";
            case 5:
                return "紧急限流";
            case 6:
                return "即将关机";
            case SystemStats.THERMAL_UNKNOWN:
            default:
                return "Android 10+ 支持";
        }
    }

    private static String formatMb(long bytes) {
        if (bytes <= 0L) {
            return "0.0 MB";
        }
        final float mb = 1024f * 1024f;
        return String.format(Locale.getDefault(), "%.1f MB", bytes / mb);
    }
}
