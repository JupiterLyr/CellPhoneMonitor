package com.jupiterlyr.cellphonemonitor.monitor;

public class SystemStats {

    public static final int THERMAL_UNKNOWN = -1;

    private final float batteryCurrentMa;
    private final float batteryVoltageMv;
    private final float batteryPowerMw;
    private final float refreshRateHz;
    private final float cpuTemperatureC;
    private final float cpuFreqMhz;
    private final float cpuFreqRatio;
    private final long memoryUsedBytes;
    private final long memoryTotalBytes;
    private final int thermalStatus;

    public SystemStats(float batteryCurrentMa,
                       float batteryVoltageMv,
                       float batteryPowerMw,
                       float refreshRateHz,
                       float cpuTemperatureC,
                       float cpuFreqMhz,
                       float cpuFreqRatio,
                       long memoryUsedBytes,
                       long memoryTotalBytes,
                       int thermalStatus) {
        this.batteryCurrentMa = batteryCurrentMa;
        this.batteryVoltageMv = batteryVoltageMv;
        this.batteryPowerMw = batteryPowerMw;
        this.refreshRateHz = refreshRateHz;
        this.cpuTemperatureC = cpuTemperatureC;
        this.cpuFreqMhz = cpuFreqMhz;
        this.cpuFreqRatio = cpuFreqRatio;
        this.memoryUsedBytes = memoryUsedBytes;
        this.memoryTotalBytes = memoryTotalBytes;
        this.thermalStatus = thermalStatus;
    }

    public float getBatteryCurrentMa() {
        return batteryCurrentMa;
    }

    public float getBatteryVoltageMv() {
        return batteryVoltageMv;
    }

    public float getBatteryPowerMw() {
        return batteryPowerMw;
    }

    public float getRefreshRateHz() {
        return refreshRateHz;
    }

    public float getCpuTemperatureC() {
        return cpuTemperatureC;
    }

    public float getCpuFreqMhz() {
        return cpuFreqMhz;
    }

    public float getCpuFreqRatio() {
        return cpuFreqRatio;
    }

    public long getMemoryUsedBytes() {
        return memoryUsedBytes;
    }

    public long getMemoryTotalBytes() {
        return memoryTotalBytes;
    }

    public int getThermalStatus() {
        return thermalStatus;
    }
}
