package tachiyomi.core.common.util.system

import android.util.Log
import logcat.LogPriority

/** Tiny stand-in for square/logcat as used by the vendored Aniyomi code. */
inline fun Any.logcat(
	priority: LogPriority = LogPriority.DEBUG,
	throwable: Throwable? = null,
	message: () -> String = { "" },
) {
	val tag = this::class.java.simpleName.ifEmpty { "Watanuki" }
	val text = message()
	when (priority) {
		LogPriority.VERBOSE -> Log.v(tag, text, throwable)
		LogPriority.DEBUG -> Log.d(tag, text, throwable)
		LogPriority.INFO -> Log.i(tag, text, throwable)
		LogPriority.WARN -> Log.w(tag, text, throwable)
		LogPriority.ERROR, LogPriority.ASSERT -> Log.e(tag, text, throwable)
	}
}
