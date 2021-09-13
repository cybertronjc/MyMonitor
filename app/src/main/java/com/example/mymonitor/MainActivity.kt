package com.example.mymonitor

import android.app.Activity
import android.app.AppOpsManager
import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.OPSTR_GET_USAGE_STATS
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.util.*
import java.util.concurrent.TimeUnit


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

        getStats()
        if (checkForPermission(this)){
            //val mUsageStatsManager = getSystemService("usagestats") as UsageStatsManager //Context.USAGE_STATS_SERVICE

            val usageStatsManager =this.getSystemService(USAGE_STATS_SERVICE) as  UsageStatsManager
            if (!checkForPermission(this)) {

                return
            }

            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -5)

            val stat = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                cal.timeInMillis, System.currentTimeMillis()
            )
            Log.d("stat", "list: $stat")
            stat.forEach {
                Log.d("monitor", "pkg:")
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
                Log.d("TAG1", "totalTIme ${getDurationBreakdown(lastTimeDate3)}")
                //Log.d("TAG", "packagename: ${it.value.packageName} firstTime: $firstTime lastTime: $lastTimeDate totalTimeForground: $lastTimeDate1 lastTimeUsed $lastTimeDate2 totalTimeVisible $lastTimeDate3")
                //Log.d("TAG1", 1"list: ${it.key}, ${it.value.totalTimeInForeground}  ${it.value.lastTimeUsed}")

            }
            //UStats.getStats(this)
            //UStats.printCurrentUsageStatus(this)
            //UStats.printCurrentUsageStatus(this)

            val stats: MutableList<UsageStats> = ArrayList()
            stats.addAll(usageStats.values)
//            val finalList: List<UsageStatsWrapper> = buildUsageStatsWrapper(installedApps, stats)
//            view.onUsageStatsRetrieved(finalList)
        }else{
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getStats(){
        val cal = Calendar.getInstance()
        cal[Calendar.DAY_OF_YEAR] = -2
        val startTime = cal.timeInMillis

        val cal1 = Calendar.getInstance()
        cal[Calendar.DAY_OF_YEAR] = -1
        val endTime = cal1.timeInMillis

        Log.d("Date", "Start: ${cal.time}, end: ${cal1.time}")
        //val endTime = System.currentTimeMillis()
        var usm: UsageStatsManager =
            (this.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager)!!

        val allEvents: ArrayList<UsageEvents.Event> = ArrayList()
        val usageEvents: UsageEvents = usm.queryEvents(getStartTime(), getEndTime())
        var event: UsageEvents.Event

        var icon: Drawable?

        while (usageEvents.hasNextEvent()) {
            event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            if (event.eventType == 1 || event.eventType == 2) {
                allEvents.add(event)
            }
        }
        allEvents.sortBy { it.packageName }
        allEvents.groupBy { it.packageName }
        //Collections.sort(allEvents, sort1())
        allEvents.forEachIndexed { index, event ->
            //val add =+ event.timeStamp
            Log.d("grouped", "ind: $index Event: ${event.packageName}")
        }
        for (i in 1 until allEvents.size) {
            val E0 = allEvents[i]
            val E1 = allEvents[i - 1]
            var addedValue = 0L
            if (E1.eventType == 1 && E0.eventType == 2 && E1.packageName == E0.packageName) {
                //icon = packageManager.getApplicationIcon(E1.packageName)
                val diff = E0.timeStamp - E1.timeStamp
                //val add = E0.timeStamp + E1
                addedValue =+ diff

            }
            Log.d("total:","total ${getDurationBreakdown(addedValue.toLong())}")
            Log.d("App:", "pkg name: ${E1.getPackageName()} breakdown ${getDurationBreakdown(addedValue.toLong())}")
        }
    }

//    fun getAppNameFromPkgName(ctx: Context, Packagename: String?): String? {
//        return try {
//            val packageManager = ctx.packageManager
//            val info =
//                packageManager.getApplicationInfo(Packagename!!, PackageManager.GET_META_DATA)
//            packageManager.getApplicationLabel(info) as String
//        } catch (e: PackageManager.NameNotFoundException) {
//            e.printStackTrace()
//            ""
//        }
//    }


    fun getDurationBreakdown(millis: Long): String? {
        var millis = millis
        require(millis >= 0) { "Duration must be greater than zero!" }
        val days: Long = TimeUnit.MILLISECONDS.toDays(millis)
        millis -= TimeUnit.DAYS.toMillis(days)
        val hours: Long = TimeUnit.MILLISECONDS.toHours(millis)
        millis -= TimeUnit.HOURS.toMillis(hours)
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(millis)
        millis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(millis)
        val sb = StringBuilder(64)
        sb.append(hours)
        sb.append(" Hours ")
        sb.append(minutes)
        sb.append(" Minutes ")
        sb.append(seconds)
        sb.append(" Seconds")
        //return sb.toString()

//        val hms = String.format(
//            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
//            TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
//            TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
//        )
        return String.format(
            "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
            TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
        )
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
        Log.d("Date", "Start: ${calendar.time}")
        return calendar.timeInMillis
    }
    private fun getEndTime(): Long {

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        Log.d("Date", " end: ${calendar.time}")
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