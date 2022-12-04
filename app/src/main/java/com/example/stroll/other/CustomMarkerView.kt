package com.example.stroll.other

import android.annotation.SuppressLint
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

@SuppressLint("ViewConstructor")
class CustomMarkerView(
    /*
    class to make a custom marker view, it displays the different values for a hike when a hike is
        selected. It is called for example in the barchart when a bar is pressed on.
     */
    private val hikes: List<StrollDataEntity>,
    c: Context,
    layoutId: Int
    ) : MarkerView(c, layoutId) {

    // the different values of each hike
    private val tvID = findViewById<TextView>(R.id.tvID)
    private val tvDate = findViewById<TextView>(R.id.tvDate)
    private val tvAvgSpeed: TextView = findViewById(R.id.tvAvgSpeed)
    private val tvDistance: TextView = findViewById(R.id.tvDistance)
    private val tvDuration: TextView = findViewById(R.id.tvDuration)

    override fun getOffset(): MPPointF {
        return MPPointF(-width / 2f, -height.toFloat())
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        /*
        refreshing the content and updating the textViews to display the hike's values
         */
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
        "${resources.getString(R.string.date)}: $dateFormat".also { tvDate.text = it }

        val avgSpeed = "${resources.getString(R.string.speed)}: ${hike.averageSpeedInKMH}km/h"
        tvAvgSpeed.text = avgSpeed

        val distanceInKm = "${resources.getString(R.string.distance)}: ${hike.distanceInMeters / 1000f}km"
        tvDistance.text = distanceInKm

        "${resources.getString(R.string.time)}: ${Utility.getFormattedStopWatchTime(hike.timeInMillis)}".also { tvDuration.text = it }
    }
}