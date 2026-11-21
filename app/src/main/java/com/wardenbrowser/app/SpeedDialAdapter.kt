package com.wardenbrowser.app

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SpeedDialAdapter(
    private val items: MutableList<SpeedDialItem>,
    private var isPrivateMode: Boolean = false,
    private val onItemClick: (SpeedDialItem) -> Unit,
    private val onAddClick: () -> Unit,
    private val onItemLongClick: (SpeedDialItem, View) -> Unit
) : RecyclerView.Adapter<SpeedDialAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.speedDialName)
        val iconText: TextView = view.findViewById(R.id.speedDialIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_speed_dial, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Son hücre "+" butonudur (sınıra ulaşılmadıysa)
        val item = items.getOrNull(position)
        val context = holder.itemView.context
        holder.nameText.text = item?.name ?: ""
        holder.iconText.text = item?.name?.first()?.toString()?.uppercase() ?: "+"

        val surfaceColor = if (isPrivateMode) {
            ContextCompat.getColor(context, R.color.private_mode_surface)
        } else {
            // Get colorSurfaceVariant from theme
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
            typedValue.data
        }

        holder.iconText.backgroundTintList = ColorStateList.valueOf(surfaceColor)

        val textColor = if (isPrivateMode) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            // Get textPrimary from colors (or theme)
            ContextCompat.getColor(context, R.color.text_primary)
        }
        holder.nameText.setTextColor(textColor)

        holder.itemView.setOnClickListener {
            if (item == null) onAddClick() else onItemClick(item)
        }
        holder.itemView.setOnLongClickListener {
            if (item == null) false else { onItemLongClick(item, holder.itemView); true }
        }
    }

    fun removeItem(item: SpeedDialItem) {
        if (items.remove(item)) notifyDataSetChanged()
    }

    fun addItem(item: SpeedDialItem) {
        if (items.size >= MAX_ITEMS) return
        items.add(item)
        notifyDataSetChanged()
    }

    fun updatePrivateMode(privateMode: Boolean) {
        if (this.isPrivateMode != privateMode) {
            this.isPrivateMode = privateMode
            notifyDataSetChanged()
        }
    }

    // MAX_ITEMS'a ulaşılınca "+" butonu gösterilmez
    override fun getItemCount() = if (items.size < MAX_ITEMS) items.size + 1 else items.size

    companion object {
        const val MAX_ITEMS = 8
    }
}
