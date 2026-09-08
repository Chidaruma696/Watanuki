package tachiyomi.core.common.util.lang

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rx.Observable
import rx.Subscriber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Minimal replacements for Aniyomi's `tachiyomi.core.common.util.lang` helpers used by the
 * vendored source-api / network code.
 */

suspend fun <T> withIOContext(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)

@OptIn(DelicateCoroutinesApi::class)
fun launchUI(block: suspend CoroutineScope.() -> Unit): Job = GlobalScope.launch(Dispatchers.Main, block = block)

/** Bridges a RxJava 1 [Observable] (the legacy extension API) into a suspending call. */
suspend fun <T> Observable<T>.awaitSingle(): T = suspendCancellableCoroutine { continuation ->
	val subscription = subscribe(object : Subscriber<T>() {
		private var value: T? = null
		private var hasValue = false

		override fun onNext(t: T) {
			if (hasValue) {
				unsubscribe()
				if (continuation.isActive) {
					continuation.resumeWithException(IllegalStateException("Observable emitted more than one value"))
				}
				return
			}
			value = t
			hasValue = true
		}

		override fun onCompleted() {
			if (!continuation.isActive) return
			if (hasValue) {
				@Suppress("UNCHECKED_CAST")
				continuation.resume(value as T)
			} else {
				continuation.resumeWithException(NoSuchElementException("Observable completed without emitting"))
			}
		}

		override fun onError(e: Throwable) {
			if (continuation.isActive) continuation.resumeWithException(e)
		}
	})
	continuation.invokeOnCancellation { subscription.unsubscribe() }
}
