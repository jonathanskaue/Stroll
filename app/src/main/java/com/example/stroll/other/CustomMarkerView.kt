package com.example.stroll.other

import android.content.Context
import android.widget.TextView
import com.example.stroll.R
import com.example.stroll.data.local.StrollDataEntity
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    val hikes: List<StrollDataEntity>,
    c: Context,
    layoutId: Int
) : MarkerView(c, layoutId) {

    val tvID = findViewById<TextView>(R.id.tvID)
    val tvDate = findViewById<TextView>(R.id.tvDate)
    val tvAvgSpeed = findViewById<TextView>(R.id.tvAvgSpeed)
    val tvDistance = findViewById<TextView>(R.id.tvDistance)
    val tvDuration = findViewById<TextView>(R.id.tvDuration)

    override fun getOffset(): MPPointF {
        return MPPointF(-width / 2f, -height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        if(e == null) {
            return
        }
        val curRunId = e.x.toInt()
        val hike = hikes[curRunId]

        val calendar = Calendar.getInstance().apply {
            timeInMillis = hike.timeStamp
        }
        val id_number = "${resources.getString(R.string.hike)} #${hike.id}"
        tvID.text = id_number

        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(calendar.time)
        "${resources.getString(R.string.date)}: ${dateFormat}".also { tvDate.text = it }

        val avgSpeed = "${resources.getString(R.string.speed)}: ${hike.averageSpeedInKMH}km/h"
        tvAvgSpeed.text = avgSpeed

        val distanceInKm = "${resources.getString(R.string.distance)}: ${hike.distanceInMeters / 1000f}km"
        tvDistance.text = distanceInKm

        "${resources.getString(R.string.time)}: ${Utility.getFormattedStopWatchTime(hike.timeInMillis)}".also { tvDuration.text = it }
    }
}