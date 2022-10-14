package com.example.stroll.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.stroll.MyClickListener
import com.example.stroll.R
import com.example.stroll.data.local.Hike
//import kotlinx.synthetic.main.list_hike.view.*

class HikesAdapter(val listener: MyClickListener): RecyclerView.Adapter<HikesAdapter.HikesViewHolder>() {

    var hikeList = emptyList<Hike>()

    inner class HikesViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener{
                val position = absoluteAdapterPosition
                listener.onClick(position)
            }
        }
    }

    private val diffCallBack = object : DiffUtil.ItemCallback<Hike>() {
        override fun areItemsTheSame(oldItem: Hike, newItem: Hike): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Hike, newItem: Hike): Boolean {
            return oldItem == newItem
        }
    }

    override fun getItemCount() = hikeList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HikesViewHolder {
        return HikesViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_hike, parent, false))
    }

    override fun onBindViewHolder(holder: HikesViewHolder, position: Int) {
        /*holder.itemView.txtTimestamp = hikeList[position].timestamp
        holder.itemView.strolldata = hikeList[position].strollDataEntity*/
    }

    fun setData(newList: List<Hike>) {
        hikeList = newList
        notifyDataSetChanged()
    }
}