package eu.kanade.tachiyomi

/** Host app identity exposed to sources (some use it in their User-Agent). Set once at startup. */
object AppInfo {
	@Volatile private var versionCode: Int = 1
	@Volatile private var versionName: String = "0.0.0"

	fun init(versionCode: Int, versionName: String) {
		this.versionCode = versionCode
		this.versionName = versionName
	}

	fun getVersionCode(): Int = versionCode
	fun getVersionName(): String = versionName
}
