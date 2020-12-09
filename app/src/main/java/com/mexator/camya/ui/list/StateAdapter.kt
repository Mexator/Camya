package com.mexator.camya.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mexator.camya.R
import com.mexator.camya.databinding.ItemErrorBinding

/**
 * ViewHolders for different states (Loading, Error)
 */
sealed class StateHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class LoadingHolder(itemView: View) : StateHolder(itemView)
class ErrorHolder(itemView: View) : StateHolder(itemView) {
    val binding: ItemErrorBinding = ItemErrorBinding.bind(itemView)
}

class StateAdapterUtil {
    abstract class State
    object Loading : State()
    class Error(val errorContent: String) : State()

    var state: State? = null
        set(value) {
            when (value) {
                is Error -> errorAdapter.errorContent = value.errorContent
                is Loading -> loadingAdapter.shown = true
                else -> {
                    errorAdapter.errorContent = null
                    loadingAdapter.shown = false
                }
            }
            field = value
        }

    private var errorAdapter = ErrorAdapter()
    private var loadingAdapter = LoadingAdapter()
    private val adapter = ConcatAdapter(loadingAdapter, errorAdapter)
    fun getAdapter() = adapter
}

class LoadingAdapter : RecyclerView.Adapter<LoadingHolder>() {
    var shown: Boolean = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoadingHolder =
        LoadingHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
        )

    override fun onBindViewHolder(holder: LoadingHolder, position: Int) {}

    override fun getItemCount(): Int {
        return if (shown) 1 else 0
    }

}

class ErrorAdapter : RecyclerView.Adapter<ErrorHolder>() {
    var errorContent: String? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorHolder =
        ErrorHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_error, parent, false)
        )

    override fun onBindViewHolder(holder: ErrorHolder, position: Int) {
        holder.binding.errorContent.text = errorContent
    }

    override fun getItemCount(): Int {
        return if (errorContent == null) 1 else 0
    }
}