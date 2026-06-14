package com.jupiterlyr.cellphonemonitor.monitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SystemMonitor {

    public interface Listener {
        void onSystemStatsChanged(SystemStats stats);
    }

    private static final long UPDATE_INTERVAL_MS = 1000L;

    private static final String[] CPU_TEMP_PATHS = {
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/class/hwmon/hwmon0/temp1_input"
    };

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Listener listener;
    private boolean monitoring;

    private long prevProcCpuMs;
    private long prevWallMs;
    private final int cpuCoreCount = Math.max(1, Runtime.getRuntime().availableProcessors());

    private Boolean procStatAvailable;
    private long prevSysBusy;
    private long prevSysTotal;

    private long[] cpuMaxFreqKhzCache;
    private int sysfsCpuCount = -1;

    public SystemMonitor(Context context) {
        this.context = context.getApplicationContext();
    }

    public void start(Listener listener) {
        if (monitoring) {
            return;
        }
        this.listener = listener;
        monitoring = true;
        handler.post(monitorRunnable);
    }

    public void stop() {
        monitoring = false;
        listener = null;
        handler.removeCallbacks(monitorRunnable);
    }

    private final Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (!monitoring || listener == null) {
                return;
            }
            listener.onSystemStatsChanged(collectSystemStats());
            if (monitoring) {
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        }
    };

    private SystemStats collectSystemStats() {
        float batteryCurrentUa = getBatteryCurrentUa();
        float batteryVoltageMv = getBatteryVoltageMv();
        float batteryCurrentMa = batteryCurrentUa / 1000f;
        float batteryPowerMw = batteryCurrentMa * batteryVoltageMv / 1000f;
        float cpuTemperatureC = getCpuTemperature();
        float cpuFreqMhz = 0f;
        float cpuFreqRatio = 0f;
        long[] memory = getMemoryInfo();
        int thermalStatus = getThermalStatus();

        float[] cpuFreq = getCpuFrequency();
        cpuFreqMhz = cpuFreq[0];
        cpuFreqRatio = cpuFreq[1];

        return new SystemStats(
                batteryCurrentMa,
                batteryVoltageMv,
                batteryPowerMw,
                cpuTemperatureC,
                cpuFreqMhz,
                cpuFreqRatio,
                memory[0],
                memory[1],
                thermalStatus
        );
    }

    private float getBatteryCurrentUa() {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager == null) {
                return 0f;
            }
            long currentUa = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            return currentUa == Long.MIN_VALUE ? 0f : (float) currentUa;
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private float getBatteryVoltageMv() {
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) {
                return 0f;
            }
            return intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        } catch (Exception ignored) {
            return 0f;
        }
    }

    private float getCpuTemperature() {
        for (String path : CPU_TEMP_PATHS) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(path));
                String line = reader.readLine();
                if (line == null) {
                    continue;
                }
                float temp = Float.parseFloat(line.trim());
                if (temp > 1000f) {
                    temp /= 1000f;
                } else if (temp > 100f) {
                    temp /= 100f;
                }
                return temp;
            } catch (Exception ignored) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return 0f;
    }

    private float[] getCpuFrequency() {
        if (sysfsCpuCount < 0) {
            sysfsCpuCount = detectSysfsCpuCount();
        }
        if (sysfsCpuCount == 0) {
            return new float[]{0f, 0f};
        }
        if (cpuMaxFreqKhzCache == null) {
            cpuMaxFreqKhzCache = new long[sysfsCpuCount];
            for (int i = 0; i < sysfsCpuCount; i++) {
                cpuMaxFreqKhzCache[i] = readCpuFreqKhz("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
            }
        }

        long maxCurKhz = 0L;
        float maxRatio = 0f;
        for (int i = 0; i < sysfsCpuCount; i++) {
            long cur = readCpuFreqKhz("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
            if (cur <= 0L) {
                continue;
            }
            if (cur > maxCurKhz) {
                maxCurKhz = cur;
            }
            long maxKhz = cpuMaxFreqKhzCache[i];
            if (maxKhz > 0L) {
                float ratio = (float) cur / (float) maxKhz;
                if (ratio > maxRatio) {
                    maxRatio = ratio;
                }
            }
        }
        if (maxRatio < 0f) {
            maxRatio = 0f;
        } else if (maxRatio > 1f) {
            maxRatio = 1f;
        }
        return new float[]{maxCurKhz / 1000f, maxRatio * 100f};
    }

    private static int detectSysfsCpuCount() {
        int count = 0;
        for (int i = 0; i < 64; i++) {
            File f = new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq");
            if (f.exists()) {
                count++;
            } else if (i > 0) {
                break;
            }
        }
        return count;
    }

    private static long readCpuFreqKhz(String path) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            if (line == null) {
                return -1L;
            }
            return Long.parseLong(line.trim());
        } catch (Exception ignored) {
            return -1L;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private int getThermalStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return SystemStats.THERMAL_UNKNOWN;
        }
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                return SystemStats.THERMAL_UNKNOWN;
            }
            return powerManager.getCurrentThermalStatus();
        } catch (Exception ignored) {
            return SystemStats.THERMAL_UNKNOWN;
        }
    }

    private long[] getMemoryInfo() {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) {
                return new long[]{0L, 0L};
            }
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            if (memoryInfo.totalMem <= 0L) {
                return new long[]{0L, 0L};
            }
            long used = memoryInfo.totalMem - memoryInfo.availMem;
            if (used < 0L) {
                used = 0L;
            }
            if (used > memoryInfo.totalMem) {
                used = memoryInfo.totalMem;
            }
            return new long[]{used, memoryInfo.totalMem};
        } catch (Exception ignored) {
            return new long[]{0L, 0L};
        }
    }
}
