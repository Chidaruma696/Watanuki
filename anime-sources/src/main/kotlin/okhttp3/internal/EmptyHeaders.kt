package okhttp3.internal

import okhttp3.Headers

/*
 * Some upstream extractors import these OkHttp 5.0-alpha internals; newer OkHttp releases
 * no longer expose them, so the module provides equivalent constants.
 */
val commonEmptyHeaders: Headers = Headers.Builder().build()

@Suppress("ObjectPropertyName")
val EMPTY_HEADERS: Headers = commonEmptyHeaders
