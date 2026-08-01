package com.wardenbrowser.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DownloadAdapter(
    private val items: List<DownloadItem>,
    private val onItemClick: (DownloadItem) -> Unit,
    private val onDeleteClick: (DownloadItem) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.downloadItemTitle)
        val urlText: TextView = view.findViewById(R.id.downloadItemUrl)
        val deleteButton: ImageButton = view.findViewById(R.id.btnDeleteDownloadItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_download, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.titleText.text = item.fileName
        holder.urlText.text = item.url
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.deleteButton.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount() = items.size
}