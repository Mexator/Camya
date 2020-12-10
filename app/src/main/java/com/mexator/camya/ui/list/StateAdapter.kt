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

/**
 * [StateAdapter] is an adapter class that can show loading and errors in [RecyclerView].
 * It is made as a wrapper around [ConcatAdapter] with [LoadingAdapter] and [ErrorAdapter]
 * inside it.
 * @property state when changed, adapter shows another state. If null, all cells are hidden
 */
class StateAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
            notifyDataSetChanged()
            field = value
        }

    private var errorAdapter = ErrorAdapter()
    private var loadingAdapter = LoadingAdapter()
    private val adapter = ConcatAdapter(loadingAdapter, errorAdapter)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        adapter.onCreateViewHolder(parent, viewType)


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
        adapter.onBindViewHolder(holder, position)

    override fun getItemCount(): Int =
        adapter.itemCount

    override fun getItemViewType(position: Int): Int {
        return adapter.getItemViewType(position)
    }
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
        return if (errorContent == null) 0 else 1
    }
}