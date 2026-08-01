package com.wardenbrowser.app

data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val filePath: String,
    val mimeType: String,
    val timestamp: Long
)