package com.watanuki.sources

import android.app.Application
import android.content.Context
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.JavaScriptEngine
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.SharedPreferencesStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektModule
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingleton
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

/**
 * Wires the services the sources resolve through Injekt (`Injekt.get<NetworkHelper>()`,
 * `Injekt.get<Json>()`, `Injekt.get<Application>()`…). Call once from Application.onCreate.
 */
object SourcesRuntime {

	@Volatile private var initialized = false

	fun init(app: Application, versionCode: Int, versionName: String) {
		if (initialized) return
		initialized = true
		AppInfo.init(versionCode, versionName)
		Injekt.importModule(object : InjektModule {
			override fun InjektRegistrar.registerInjectables() {
				addSingleton<Application>(app)
				addSingleton<Context>(app)
				addSingletonFactory {
					NetworkPreferences(SharedPreferencesStore(app.getSharedPreferences("network", Context.MODE_PRIVATE)))
				}
				addSingletonFactory { NetworkHelper(app, get()) }
				addSingletonFactory { JavaScriptEngine(app) }
				addSingletonFactory {
					Json {
						ignoreUnknownKeys = true
						explicitNulls = false
						isLenient = true
					}
				}
			}
		})
	}

	val network: NetworkHelper get() = Injekt.get()
}
