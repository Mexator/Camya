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
    val binding = ItemFileBinding.bind(itemView)
}

object FileDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem != newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
        true
}

class FileAdapter : ListAdapter<String, FileViewHolder>(FileDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        )
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.binding.filename.text = currentList[position]
    }
}