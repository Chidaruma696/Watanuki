package tachiyomi.core.common.i18n

import android.content.Context

/** In Aniyomi this resolves a moko resource; here strings are plain constants. */
@Suppress("UnusedReceiverParameter")
fun Context.stringResource(text: String): String = text
