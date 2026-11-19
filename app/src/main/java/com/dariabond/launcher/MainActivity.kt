package com.dariabond.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private val clockHandler = Handler(Looper.getMainLooper())

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            // re-run exactly at the next minute boundary (battery-friendly)
            val secondsToNextMinute = 60 - Calendar.getInstance().get(Calendar.SECOND)
            clockHandler.postDelayed(this, secondsToNextMinute * 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tvTime)
        tvDate = findViewById(R.id.tvDate)

        val rvApps: RecyclerView = findViewById(R.id.rvApps)
        rvApps.layoutManager = GridLayoutManager(this, 4)
        rvApps.adapter = AppAdapter(this, loadInstalledApps())

        updateClock()
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockTick)
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockTick)
    }

    private fun updateClock() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
        val now = Calendar.getInstance().time
        tvTime.text = timeFormat.format(now)
        tvDate.text = dateFormat.format(now)
    }

    /** Finds every app that has a launcher icon, sorted alphabetically. */
    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedApps = pm.queryIntentActivities(mainIntent, 0)

        return resolvedApps
            .filter { it.activityInfo.packageName != packageName } // hide ourselves
            .map { resolveInfo ->
                val label = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val pkg = resolveInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    ?: Intent().setClassName(pkg, resolveInfo.activityInfo.name)
                AppInfo(label, pkg, icon, launchIntent)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }

    // Disable the back button acting like "leave launcher" - a launcher has nowhere to go back to.
    override fun onBackPressed() {
        // no-op
    }
}
