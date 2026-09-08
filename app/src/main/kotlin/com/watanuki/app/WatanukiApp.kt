package com.watanuki.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.watanuki.app.download.DownloadRepository
import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.komi.ThemePrefs
import com.watanuki.sources.SourcesRuntime

class WatanukiApp : Application(), SingletonImageLoader.Factory {

	override fun onCreate() {
		super.onCreate()
		instance = this
		ThemePrefs.init(this)
		AppPrefs.init(this)
		DownloadRepository.init(this)
		SourcesRuntime.init(this, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
	}

	/** Covers go through the sources' OkHttp client so cookies, user agent and Cloudflare apply. */
	override fun newImageLoader(context: coil3.PlatformContext): ImageLoader =
		ImageLoader.Builder(context)
			.components {
				add(OkHttpNetworkFetcherFactory(callFactory = { SourcesRuntime.network.client }))
			}
			.crossfade(true)
			.build()

	companion object {
		lateinit var instance: WatanukiApp
			private set
	}
}
