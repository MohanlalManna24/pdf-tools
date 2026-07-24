package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.worker.PdfWorker
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class BatteryInfo(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val isPowerSaveMode: Boolean = false
) {
    val isLowBattery: Boolean get() = levelPercent <= 20 && !isCharging
}

object BatteryOptimizationManager {

    private const val PREFS_NAME = "battery_optimization_prefs"
    private const val KEY_BATTERY_MODE = "battery_saver_mode"
    private const val KEY_PAUSE_LOW_BATTERY = "pause_low_battery"
    private const val KEY_REQUIRE_CHARGING = "require_charging_heavy"

    fun isBatterySaverEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BATTERY_MODE, true)
    }

    fun setBatterySaverEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_BATTERY_MODE, enabled).apply()
    }

    fun isPauseLowBatteryEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PAUSE_LOW_BATTERY, true)
    }

    fun setPauseLowBatteryEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PAUSE_LOW_BATTERY, enabled).apply()
    }

    fun isRequireChargingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REQUIRE_CHARGING, false)
    }

    fun setRequireChargingEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REQUIRE_CHARGING, enabled).apply()
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        return BatteryInfo(
            levelPercent = batteryPct,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode
        )
    }

    fun shouldDeferToWorkManager(context: Context, isHeavyTask: Boolean = true): Boolean {
        val info = getBatteryInfo(context)
        val pauseLow = isPauseLowBatteryEnabled(context)
        val batterySaverActive = isBatterySaverEnabled(context)

        // Defer to WorkManager if:
        // 1. Device is in low battery or OS Power Save mode and user enabled Pause Low Battery
        // 2. Heavy task AND battery saver mode enabled with low battery
        return (pauseLow && (info.isLowBattery || info.isPowerSaveMode)) || (batterySaverActive && isHeavyTask && info.isLowBattery)
    }

    fun enqueueWorkManagerTask(
        context: Context,
        toolId: String,
        titlesOrPaths: List<String>,
        extraParam: String = ""
    ): UUID {
        val requireCharging = isRequireChargingEnabled(context)
        val isBatterySaver = isBatterySaverEnabled(context)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .apply {
                if (requireCharging) {
                    setRequiresCharging(true)
                }
            }
            .build()

        val inputData = Data.Builder()
            .putString(PdfWorker.KEY_TOOL_ID, toolId)
            .putString(PdfWorker.KEY_PATHS, titlesOrPaths.joinToString("|||"))
            .putString(PdfWorker.KEY_EXTRA_PARAM, extraParam)
            .putBoolean(PdfWorker.KEY_BATTERY_SAVER, isBatterySaver)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PdfWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("pdf_background_tool_$toolId")
            .build()

        val uniqueWorkName = "pdf_bg_job_${toolId}_${System.currentTimeMillis()}"

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        return workRequest.id
    }

    fun getWorkInfoFlow(context: Context, workId: UUID): Flow<WorkInfo?> {
        return WorkManager.getInstance(context).getWorkInfoByIdFlow(workId)
    }
}
