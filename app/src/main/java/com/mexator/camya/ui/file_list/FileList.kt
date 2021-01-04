package com.mexator.camya.ui.file_list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mexator.camya.R
import com.mexator.camya.databinding.ItemFileBinding

class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    companion object {
        private const val ACTIVE_COLOR = R.attr.colorPrimary
        private const val INACTIVE_COLOR = R.attr.colorOnPrimary
    }

    val binding = ItemFileBinding.bind(itemView)
    private val activeColor =
        itemView.context
            .obtainStyledAttributes(0, intArrayOf(ACTIVE_COLOR))
            .getColor(0, 0)
    private val inactiveColor =
        itemView.context
            .obtainStyledAttributes(0, intArrayOf(INACTIVE_COLOR))
            .getColor(0, 0)
    var isActive = false
        set(value) {
            binding.root.setCardBackgroundColor(if (value) activeColor else inactiveColor)
            field = value
        }
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

        holder.isActive = (position == chosenPosition)

        holder.binding.root.setOnClickListener {
            val oldPosition = chosenPosition
            chosenPosition = position

            notifyItemChanged(position)
            oldPosition?.let { notifyItemChanged(it) }
        }
    }
}