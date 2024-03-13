package com.app.cascadeos.ui

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityDocumentViewerBinding
import com.app.cascadeos.utility.logError


class DocumentViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDocumentViewerBinding
    private var fileUrl: String? = ""
    private var fileName: String? = ""
    private val appBarHandler = Handler(Looper.getMainLooper())

    companion object {
        fun start(context: Context, fileUrl: String, fileName: String = "") {
            val starter = Intent(context, DocumentViewerActivity::class.java)
            starter.putExtra("url", fileUrl)
            starter.putExtra("fileName", fileName)
            context.startActivity(starter)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_document_viewer)
        binding.layoutMain.layoutTransition = LayoutTransition()

        fileUrl = intent.getStringExtra("url")
        fileName = intent.getStringExtra("fileName")

        "FileName:${fileName} URL : $fileUrl".logError(javaClass)

        binding.layoutHiddenActionbar.setOnClickListener {
            binding.actionBar.actionBar.visibility = View.VISIBLE
            hideAppBar()
        }

        binding.actionBar.actionBar.findViewById<AppCompatTextView>(R.id.txt_title).text = fileName
        binding.actionBar.actionBar.findViewById<AppCompatImageView>(R.id.img_close)
            .setOnClickListener { finish() }

        hideAppBar()
        hideSystemBars()

        binding.apply {
/*
            if (savedInstanceState == null) {
*/
            docWebView.settings.builtInZoomControls = true
            docWebView.settings.javaScriptEnabled = true
            docWebView.settings.allowFileAccess = true
            docWebView.settings.allowContentAccess = true
            docWebView.loadUrl(fileUrl.toString())

            docWebView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    "Page Load Start".logError(javaClass)
                    progressDocumentLoader.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    "page Load Finished : {${view?.title}}".logError(javaClass)
                    if(view?.title.isNullOrEmpty()){
                       binding.docWebView.loadUrl(fileUrl.toString())
                    }else{
                        progressDocumentLoader.visibility = View.GONE

                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    "${error?.description}".logError(javaClass)
                    super.onReceivedError(view, request, error)
                }
            }
        }
        /* }
 */
    }

    /* override fun onSaveInstanceState(outState: Bundle) {
         super.onSaveInstanceState(outState)
         binding.docWebView.saveState(outState)
     }

     override fun onRestoreInstanceState(savedInstanceState: Bundle) {
         super.onRestoreInstanceState(savedInstanceState)
         binding.docWebView.restoreState(savedInstanceState)
     }
 */
    private fun hideAppBar() {
        appBarHandler.postDelayed({
            binding.actionBar.actionBar.visibility = View.GONE
        }, 3000)
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide both the status bar and the navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

}