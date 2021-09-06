package com.example.mymonitor

import android.app.Activity
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.annotation.RequiresApi
import java.util.*


class MainActivity : Activity() {
    val TAG = "UsageStatsActivity"
    val localLOGV = false
    var mUsageStatsManager: UsageStatsManager? = null
    var mInflater: LayoutInflater? = null
    var mPm: PackageManager? = null
    private val flags = PackageManager.GET_META_DATA or
            PackageManager.GET_SHARED_LIBRARY_FILES or
            PackageManager.GET_UNINSTALLED_PACKAGES


    /** Called when the activity is first created.  */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
     override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_main)

//        val list = getUsageStatistics(UsageStatsManager.INTERVAL_DAILY)
//            Log.d("TAG", "list: $list")
        if (checkForPermission(this)){
            //val mUsageStatsManager = getSystemService("usagestats") as UsageStatsManager //Context.USAGE_STATS_SERVICE

            val usageStatsManager =this.getSystemService(USAGE_STATS_SERVICE) as  UsageStatsManager
            if (!checkForPermission(this)) {

                return
            }
            val installedApps = getInstalledAppList()

            val usageStats: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(
                getStartTime(),
                getEndTime()
            )
            usageStats.entries.forEach {
                val lastTimeDate = Date(it.value.lastTimeStamp)
                val firstTime = Date(it.value.firstTimeStamp)
                val lastTimeDate1 = it.value.totalTimeInForeground
                val lastTimeDate2 = it.value.lastTimeUsed
                val lastTimeDate3 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    it.value.totalTimeVisible
                } else {
                    TODO("VERSION.SDK_INT < Q")
                }
                Log.d("TAG", "packagename: ${it.value.packageName} firstTime: $firstTime lastTime: $lastTimeDate totalTimeForground: $lastTimeDate1 lastTimeUsed $lastTimeDate2 totalTimeVisible $lastTimeDate3")
                //Log.d("TAG1", 1"list: ${it.key}, ${it.value.totalTimeInForeground}  ${it.value.lastTimeUsed}")

            }

            val stats: MutableList<UsageStats> = ArrayList()
            stats.addAll(usageStats.values)
//            val finalList: List<UsageStatsWrapper> = buildUsageStatsWrapper(installedApps, stats)
//            view.onUsageStatsRetrieved(finalList)
        }else{
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getUsageStatistics(intervalType: Int): List<UsageStats?>? {
        // Get the app statistics since one year ago from the current time.
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        val queryUsageStats = mUsageStatsManager?.queryUsageStats(
                intervalType, cal.timeInMillis,
                System.currentTimeMillis()
            )
        if (queryUsageStats?.size == 0) {
            Log.i(
                "TAG",
                "The user may not allow the access to apps usage. "
            )
            Toast.makeText(
                this,
                "App usages not active",
                Toast.LENGTH_LONG
            ).show()
            startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        }
        return queryUsageStats
    }
    private fun getStartTime(): Long {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        return calendar.timeInMillis
    }
    private fun getEndTime(): Long {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.timeInMillis
    }


    private fun getInstalledAppList(): List<String>? {
        val infos = packageManager.getInstalledApplications(flags)
        val installedApps: MutableList<String> = ArrayList()
        for (info in infos) {
            installedApps.add(info.packageName)
        }
        return installedApps
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun checkForPermission(context: Context): Boolean {
        val appOps = context.getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        Log.d("mode", "$mode")
        return mode == MODE_ALLOWED
    }

}