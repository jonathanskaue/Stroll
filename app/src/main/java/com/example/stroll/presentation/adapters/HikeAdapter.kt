package com.example.stroll.presentation.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stroll.R
import com.example.stroll.data.local.StrollDataEntity
import com.example.stroll.databinding.ItemHikeBinding
import com.example.stroll.domain.repository.RVClickListener
import com.example.stroll.other.Utility
import java.text.SimpleDateFormat
import java.util.*

/*
Hike adapter for hike history
 */
class HikeAdapter(var listener: RVClickListener): RecyclerView.Adapter<HikeAdapter.HikeViewHolder>() {

    inner class HikeViewHolder(var binding: ItemHikeBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                val position = absoluteAdapterPosition
                listener.onClick(position)
            }
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<StrollDataEntity>() {
        override fun areItemsTheSame(
            oldItem: StrollDataEntity,
            newItem: StrollDataEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: StrollDataEntity,
            newItem: StrollDataEntity
        ): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<StrollDataEntity>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikeViewHolder {
        val binding = ItemHikeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HikeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HikeViewHolder, position: Int) {
        val hike = differ.currentList[position]
        holder.itemView.apply {
            Glide.with(this)
                .load(BitmapFactory.decodeFile(context.filesDir.path + "/${hike.mapSnapShot}"))
                .override(250, 250)
                .circleCrop()
                .into(this.findViewById(R.id.hikeMapSnapShot))
            val calendar = Calendar.getInstance().apply {
                timeInMillis = hike.timeStamp
            }
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            holder.binding.dateTimeStamp.text = dateFormat.format(calendar.time)

            val averageSpeed = "${hike.averageSpeedInKMH}km/h"
            holder.binding.averageSpeed.text = averageSpeed

            val distanceInKM = "${hike.distanceInMeters / 1000f}km"
            holder.binding.totalDistanceHiked.text = distanceInKM

            holder.binding.totalTimeHiked.text = Utility.getFormattedStopWatchTime(hike.timeInMillis)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}