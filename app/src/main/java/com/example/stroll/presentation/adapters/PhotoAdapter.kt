package com.example.stroll.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.stroll.data.local.InternalStoragePhoto
import com.example.stroll.databinding.ItemPhotoBinding
import com.example.stroll.domain.repository.RVClickListener

/*
Photo adapter for photos taken while on a hike.
 */
class PhotoAdapter(var listener: RVClickListener): RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(val binding: ItemPhotoBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener{
                val position = absoluteAdapterPosition
                listener.onClick(position)
            }
        }
    }

   private val diffCallback = object : DiffUtil.ItemCallback<InternalStoragePhoto>(){
        override fun areItemsTheSame(
            oldItem: InternalStoragePhoto,
            newItem: InternalStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(
            oldItem: InternalStoragePhoto,
            newItem: InternalStoragePhoto
        ): Boolean {
            return oldItem.name == newItem.name && oldItem.bmp.sameAs(newItem.bmp)
        }
    }

    val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<InternalStoragePhoto>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = differ.currentList[position]
        holder.binding.apply {
            ivPhoto.setImageBitmap(photo.bmp)

            val aspectRatio = photo.bmp.width.toFloat() / photo.bmp.height.toFloat()
            ConstraintSet().apply {
                clone(root)
                setDimensionRatio(ivPhoto.id, aspectRatio.toString())
                applyTo(root)
            }
        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}