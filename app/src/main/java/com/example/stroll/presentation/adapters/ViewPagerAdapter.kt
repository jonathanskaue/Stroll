package com.example.stroll.presentation.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.viewpager.widget.PagerAdapter
import com.example.stroll.R
import com.example.stroll.presentation.viewmodel.MainViewModel
import java.util.*

/*
Viewpager for introduction fragment
 */
class ViewPagerAdapter(
    val viewModel: MainViewModel,
    val view: View?,
    val context: Context,
    private val imageList: List<Int>,
    private val headingList: List<String>,
    private val bodyList: List<String>,
    ): PagerAdapter() {

    override fun getCount(): Int {
        return imageList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object` as RelativeLayout
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val mLayoutInflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val itemView: View = mLayoutInflater.inflate(R.layout.image_slider_item, container, false)
        val imageView: ImageView = itemView.findViewById<View>(R.id.idIVImage) as ImageView
        val headingView = itemView.findViewById(R.id.idIvHeading) as TextView
        val bodyView = itemView.findViewById(R.id.idTvBody) as TextView
        val introButtonButton = itemView.findViewById(R.id.introButton) as Button

        introButtonButton.setOnClickListener {
            viewModel.checkStarted()
        }
        imageView.setImageResource(imageList[position])
        headingView.text = headingList[position]
        bodyView.text = bodyList[position]
        Objects.requireNonNull(container).addView(itemView)
        introButtonButton.isVisible = position == count-1

        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as RelativeLayout)
    }
}