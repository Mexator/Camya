package com.mexator.camya.ui.file_list

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mexator.camya.R
import com.mexator.camya.databinding.ItemFileBinding
import com.mexator.camya.extensions.getTag

class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFileBinding.bind(itemView)
}

object FileDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem != newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
        false
}

class FileAdapter : ListAdapter<String, FileViewHolder>(FileDiffCallback) {
    var chosenPosition: Int? = null
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.binding.filename.text = currentList[position]

        with(holder.itemView.context) {
            val colorId =
                if (position == chosenPosition) {
                    R.color.chosen_card
                } else {
                    R.color.white
                }
            holder.binding.root.setCardBackgroundColor(
                resources.getColor(
                    colorId,
                    this.theme
                )
            )
        }

        holder.binding.root.setOnClickListener {
            val oldPosition = chosenPosition
            chosenPosition = position

            notifyItemChanged(position)
            oldPosition?.let { notifyItemChanged(it) }
        }
    }
}