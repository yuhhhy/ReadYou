package me.ash.reader.ui.component.webview

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Hosts HTML5/iframe video custom views above the Compose UI. */
class FullscreenWebChromeClient(private val activity: Activity?) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var systemBarsWereVisible = true
    private var keepScreenOnWasEnabled = false

    private val backCallback =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                onHideCustomView()
            }
        }

    init {
        (activity as? ComponentActivity)?.let {
            it.onBackPressedDispatcher.addCallback(it, backCallback)
        }
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (view == null || activity == null) {
            callback?.onCustomViewHidden()
            return
        }

        if (customView != null) {
            onHideCustomView()
        }

        customView = view
        customViewCallback = callback
        backCallback.isEnabled = true

        val window = activity.window
        val decorView = window.decorView
        systemBarsWereVisible =
            ViewCompat.getRootWindowInsets(decorView)
                ?.isVisible(WindowInsetsCompat.Type.systemBars()) ?: true
        keepScreenOnWasEnabled =
            window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0

        (view.parent as? ViewGroup)?.removeView(view)
        view.setBackgroundColor(Color.BLACK)
        window.addContentView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    @Suppress("DEPRECATION")
    override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        callback: CustomViewCallback?,
    ) {
        onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        val view = customView ?: return
        (view.parent as? ViewGroup)?.removeView(view)
        customView = null

        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        backCallback.isEnabled = false

        activity?.let {
            val window = it.window
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            if (systemBarsWereVisible) {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
            if (!keepScreenOnWasEnabled) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    fun release() {
        onHideCustomView()
        backCallback.remove()
    }
}
