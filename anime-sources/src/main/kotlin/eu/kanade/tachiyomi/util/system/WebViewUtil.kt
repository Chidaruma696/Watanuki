package eu.kanade.tachiyomi.util.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast

/*
 * Replacements for the `eu.kanade.tachiyomi.util.system` helpers the vendored network code
 * relies on (device checks, WebView setup, toasts).
 */

object DeviceUtil {
	val isMiui: Boolean by lazy { getSystemProperty("ro.miui.ui.version.name")?.isNotEmpty() == true }
	val isSamsung: Boolean by lazy { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }

	@SuppressLint("PrivateApi")
	private fun getSystemProperty(key: String): String? = try {
		Class.forName("android.os.SystemProperties")
			.getDeclaredMethod("get", String::class.java)
			.invoke(null, key) as? String
	} catch (_: Exception) {
		null
	}
}

object WebViewUtil {
	const val MINIMUM_WEBVIEW_VERSION = 118

	fun supportsWebView(context: Context): Boolean {
		return try {
			context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW) &&
				WebSettings.getDefaultUserAgent(context).isNotEmpty()
		} catch (_: Throwable) {
			false
		}
	}
}

@SuppressLint("SetJavaScriptEnabled")
fun WebView.setDefaultSettings() {
	with(settings) {
		javaScriptEnabled = true
		domStorageEnabled = true
		databaseEnabled = true
		useWideViewPort = true
		loadWithOverviewMode = true
		cacheMode = WebSettings.LOAD_DEFAULT
	}
}

/** True when the WebView's Chromium major version is older than what Cloudflare tolerates. */
fun WebView.isOutdated(): Boolean {
	val version = Regex("""Chrome/(\d+)""").find(settings.userAgentString)?.groupValues?.get(1)?.toIntOrNull()
		?: return false
	return version < WebViewUtil.MINIMUM_WEBVIEW_VERSION
}

fun Context.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
	val show = { Toast.makeText(applicationContext, text, duration).show() }
	if (Looper.myLooper() == Looper.getMainLooper()) show() else Handler(Looper.getMainLooper()).post(show)
}
