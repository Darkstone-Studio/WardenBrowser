package com.wardenbrowser.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DownloadsDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "downloads.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_DOWNLOADS = "downloads"
        const val KEY_ID = "id"
        const val KEY_FILENAME = "filename"
        const val KEY_URL = "url"
        const val KEY_PATH = "path"
        const val KEY_MIME = "mime"
        const val KEY_TIMESTAMP = "timestamp"

        @Volatile private var instance: DownloadsDbHelper? = null
        fun getInstance(context: Context): DownloadsDbHelper =
            instance ?: synchronized(this) {
                instance ?: DownloadsDbHelper(context.applicationContext).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_DOWNLOADS (" +
                "$KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$KEY_FILENAME TEXT, " +
                "$KEY_URL TEXT, " +
                "$KEY_PATH TEXT, " +
                "$KEY_MIME TEXT, " +
                "$KEY_TIMESTAMP INTEGER)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DOWNLOADS")
        onCreate(db)
    }

    fun addDownload(fileName: String, url: String, path: String, mime: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_FILENAME, fileName)
            put(KEY_URL, url)
            put(KEY_PATH, path)
            put(KEY_MIME, mime)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_DOWNLOADS, null, values)
    }

    fun getAllDownloads(): List<DownloadItem> {
        val list = mutableListOf<DownloadItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DOWNLOADS ORDER BY $KEY_TIMESTAMP DESC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    DownloadItem(
                        id = it.getLong(it.getColumnIndexOrThrow(KEY_ID)),
                        fileName = it.getString(it.getColumnIndexOrThrow(KEY_FILENAME)),
                        url = it.getString(it.getColumnIndexOrThrow(KEY_URL)),
                        filePath = it.getString(it.getColumnIndexOrThrow(KEY_PATH)),
                        mimeType = it.getString(it.getColumnIndexOrThrow(KEY_MIME)),
                        timestamp = it.getLong(it.getColumnIndexOrThrow(KEY_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    fun deleteDownload(id: Long) {
        writableDatabase.delete(TABLE_DOWNLOADS, "$KEY_ID = ?", arrayOf(id.toString()))
    }

    fun clearAll() {
        writableDatabase.delete(TABLE_DOWNLOADS, null, null)
    }
}