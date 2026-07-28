package com.wardenbrowser.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TabAdapter(
    private val items: List<BrowserTab>,
    private val currentTabId: String?,
    private val onTabClick: (BrowserTab) -> Unit,
    private val onCloseClick: (BrowserTab) -> Unit
) : RecyclerView.Adapter<TabAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.tabItemTitle)
        val urlText: TextView = view.findViewById(R.id.tabItemUrl)
        val closeButton: ImageButton = view.findViewById(R.id.btnCloseTab)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleText.text = item.title.ifBlank { holder.itemView.context.getString(R.string.tab_default_title) }
        holder.urlText.text = item.url
        holder.itemView.setOnClickListener { onTabClick(item) }
        holder.closeButton.setOnClickListener { onCloseClick(item) }

        val card = holder.itemView as com.google.android.material.card.MaterialCardView
        if (item.id == currentTabId) {
            card.strokeWidth = (2 * holder.itemView.resources.displayMetrics.density).toInt()
            card.strokeColor = androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.accent_blue)
        } else {
            card.strokeWidth = (1 * holder.itemView.resources.displayMetrics.density).toInt()
            card.strokeColor = androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.card_stroke)
        }
    }

    override fun getItemCount() = items.size
}
