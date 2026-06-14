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

/**
 * MainActivity 的视图绑定/渲染层（仅在主线程使用，不做线程安全保护）
 */
public class MainViewBinder {

    private final Activity activity;

    // ---------- Views ----------
    private final TextView tvTime;
    private final TextView tvBattery;
    private final TextView tvCharging;
    private final TextView tvBatteryTemp;
    private final ProgressBar progressBattery;
    private final TextView tvCpuTemp;
    private final TextView tvCpuFreq;
    private final TextView tvMemoryLoad;
    private final TextView tvBatteryCurrent;
    private final TextView tvBatteryPower;
    private final TextView tvThermalStatus;

    // ---------- 渲染所需状态（由 Activity 通过 setter 同步） ----------
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final Runnable timeTicker = new Runnable() {
        @Override
        public void run() {
            tvTime.setText(timeFormat.format(new Date()));
            mainHandler.postDelayed(this, 1000);
        }
    };

    public MainViewBinder(@NonNull Activity activity) {
        this.activity = activity;

        tvTime = activity.findViewById(R.id.tvTime);
        tvBattery = activity.findViewById(R.id.tvBattery);
        tvCharging = activity.findViewById(R.id.tvCharging);
        tvBatteryTemp = activity.findViewById(R.id.tvBatteryTemp);
        progressBattery = activity.findViewById(R.id.progressBattery);
        tvCpuTemp = activity.findViewById(R.id.tvCpuTemp);
        tvMemoryLoad = activity.findViewById(R.id.tvMemoryload);
        tvCpuFreq = activity.findViewById(R.id.tvCpuFreq);
        tvBatteryCurrent = activity.findViewById(R.id.tvBatteryCurrent);
        tvBatteryPower = activity.findViewById(R.id.tvBatteryPower);
        tvThermalStatus = activity.findViewById(R.id.tvThermalStatus);
    }

    // ---------- 时间 Ticker ----------

    public void startTimeTicker() {
        mainHandler.post(timeTicker);
    }

    public void stopTimeTicker() {
        mainHandler.removeCallbacks(timeTicker);
    }

    // ---------- 电量 ----------

    @SuppressLint("SetTextI18n")
    public void renderBattery(@NonNull BatterySnapshot snapshot) {
        int level = snapshot.getBatteryLevel();
        tvBattery.setText(level + "%");
        progressBattery.setProgress(level);

        // 分档着色：<=20% 红色告警，<=40% 黄色提醒，其余绿色正常
        progressBattery.setProgressTintList(
                ColorStateList.valueOf(ContextCompat.getColor(activity, batteryProgressColorRes(level)))
        );

        tvCharging.setText("充电状态：" + chargingText(snapshot.isCharging(), level));
        tvBatteryTemp.setText(
                String.format(Locale.getDefault(), "电池温度：%.1f°C", snapshot.getBatteryTempC())
        );
    }

    @ColorRes
    private static int batteryProgressColorRes(int level) {
        if (level <= 20) return R.color.red_main;
        if (level <= 40) return R.color.yellow_main;
        return R.color.green_main;
    }

    private static String chargingText(boolean charging, int level) {
        if (charging) return level == 100 ? "充电已完成" : "充电中";
        return level < 30 ? "电量过低" : "未充电";
    }

    // ---------- CPU / GPU 负荷 ----------

    /**
     * @param stats     最新系统数据
     * @param isBurning 当前是否处于烤机运行状态（GPU 负荷文案需要据此切占位）
     */
    @SuppressLint("SetTextI18n")
    public void renderSystemStats(@NonNull SystemStats stats, boolean isBurning) {
        tvCpuTemp.setText(
                String.format(Locale.getDefault(), "CPU温度：%.1f°C", stats.getCpuTemperature())
        );

        long memUsed = stats.getMemoryUsedBytes();
        long memTotal = stats.getMemoryTotalBytes();
        if (memTotal > 0L) {
            tvMemoryLoad.setText(
                    String.format(Locale.getDefault(),
                            "内存占用：%s / %s",
                            formatMb(memUsed),
                            formatMb(memTotal))
            );
        } else {
            tvMemoryLoad.setText("内存占用：—");
        }

        // ---- CPU 频率：读不到时显示“—” ----
        if (stats.getCpuFreqMhz() > 0f) {
            tvCpuFreq.setText(
                    String.format(Locale.getDefault(),
                            "CPU频率：%.0f MHz（%.0f%%）",
                            stats.getCpuFreqMhz(), stats.getCpuFreqRatio())
            );
        } else {
            tvCpuFreq.setText("CPU频率：—（设备受限）");
        }

        // ---- 电池电流：保留方向（充电为正、放电为负），部分国产 ROM 符号反转，可由用户自行解读 ----
        float currentUa = stats.getBatteryCurrentMa();
        if (currentUa == 0f) {
            tvBatteryCurrent.setText("电池电流：—");
        } else {
            String dir = currentUa > 0f ? "充电" : "放电";
            tvBatteryCurrent.setText(
                    String.format(Locale.getDefault(),
                            "电池电流：%,.0f μA（%s）", Math.abs(currentUa), dir)
            );
        }

        // ---- 瞬时功率：μW 数量级较大，使用千位分隔提升可读性 ----
        float powerUw = stats.getBatteryPowerW();
        if (powerUw == 0f) {
            tvBatteryPower.setText("瞬时功率：—");
        } else {
            tvBatteryPower.setText(
                    String.format(Locale.getDefault(),
                            "瞬时功率：%,.0f μW", Math.abs(powerUw))
            );
        }

        // ---- 系统热状态 ----
        tvThermalStatus.setText("系统热状态：" + thermalStatusText(stats.getThermalStatus()));
    }

    /**
     * 将 {@link android.os.PowerManager#getCurrentThermalStatus()} 的常量映射为中文档位名
     * 不依赖 PowerManager 的常量值（避免低版本设备上加载常量报错），直接使用文档中的整数取值
     */
    private String thermalStatusText(int status) {
        switch (status) {
            case 0: return "正常";
            case 1: return "轻度发热";
            case 2: return "中度发热";
            case 3: return "严重发热";
            case 4: return "危险";
            case 5: return "紧急限流";
            case 6: return "即将关机";
            case SystemStats.THERMAL_UNKNOWN:
            default:
                return "未知（需 Android 10+）";
        }
    }

    /**
     * 将字节数格式化为以 MB 为单位的字符串，保留一位小数
     * 1024 进制（与 Android Settings 中"已用内存"展示的口径一致）
     */
    private static String formatMb(long bytes) {
        if (bytes <= 0L) return "0.0 MB";
        final float MB = 1024f * 1024f;
        return String.format(Locale.getDefault(), "%.1f MB", bytes / MB);
    }
}
