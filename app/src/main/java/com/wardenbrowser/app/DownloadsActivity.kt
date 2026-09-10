package com.wardenbrowser.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton

class DownloadsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DownloadsDbHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnClear: ExtendedFloatingActionButton
    private lateinit var emptyText: View

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downloads)
        
        setupStatusBar()

        dbHelper = DownloadsDbHelper.getInstance(this)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        recyclerView = findViewById(R.id.downloadsRecyclerView)
        btnClear = findViewById(R.id.btnClearDownloads)
        emptyText = findViewById(R.id.emptyDownloadsText)

        ViewCompat.setOnApplyWindowInsetsListener(btnClear) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val params = v.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = (32 * resources.displayMetrics.density).toInt() + systemBars.bottom
            v.layoutParams = params
            insets
        }

        loadDownloads()

        btnClear.setOnClickListener {
            dbHelper.clearAll()
            loadDownloads()
        }
    }

    private fun setupStatusBar() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        windowInsetsController.isAppearanceLightStatusBars = !isNightMode
    }

    private fun loadDownloads() {
        val downloads = dbHelper.getAllDownloads()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = DownloadAdapter(
            items = downloads,
            onItemClick = { item ->
                openDownload(item)
            },
            onDeleteClick = { item ->
                dbHelper.deleteDownload(item.id)
                loadDownloads()
            }
        )
        
        val isEmpty = downloads.isEmpty()
        btnClear.visibility = if (isEmpty) View.GONE else View.VISIBLE
        emptyText.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun openDownload(item: DownloadItem) {
        try {
            val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), item.fileName)
            if (!file.exists()) {
                Toast.makeText(this, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
                return
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.toast_file_not_found), Toast.LENGTH_SHORT).show()
        }
    }
}