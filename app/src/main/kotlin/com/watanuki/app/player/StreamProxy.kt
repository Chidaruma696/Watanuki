package com.watanuki.app.player

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.watanuki.sources.SourcesRuntime
import fi.iki.elonen.NanoHTTPD
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Local HTTP proxy between libVLC and the video hosts.
 *
 * libVLC can only send a User-Agent and a Referer, but the sources' video links often need
 * extra headers (Origin, Accept, tokens) and the cookies the app already holds (Cloudflare).
 * Every request goes through the sources' OkHttp client instead, so all of that applies.
 * HLS playlists are rewritten so segments and keys also come through the proxy.
 */
class StreamProxy : NanoHTTPD("127.0.0.1", 0) {

	private val client get() = SourcesRuntime.network.client

	fun proxiedUrl(target: String, headers: Map<String, String>): String {
		val u = b64(target)
		val h = b64(JSONObject(headers as Map<*, *>).toString())
		return "http://127.0.0.1:$listeningPort/s?u=$u&h=$h"
	}

	override fun serve(session: IHTTPSession): Response {
		val target = session.parameters["u"]?.firstOrNull()?.let(::unb64) ?: return bad("missing url")
		val headers = session.parameters["h"]?.firstOrNull()?.let(::unb64)?.let { json ->
			val obj = JSONObject(json)
			obj.keys().asSequence().associateWith { obj.getString(it) }
		}.orEmpty()

		val request = Request.Builder().url(target).apply {
			headers.forEach { (k, v) -> if (!k.equals("Host", true)) header(k, v) }
			session.headers["range"]?.let { header("Range", it) }
		}.build()

		val upstream: okhttp3.Response = try {
			client.newCall(request).execute()
		} catch (e: Exception) {
			Log.w(TAG, "proxy request failed: $target", e)
			return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.message ?: "error")
		}
		val body = upstream.body
		val contentType = upstream.header("Content-Type") ?: "application/octet-stream"
		val isPlaylist = target.substringBefore('?').endsWith(".m3u8", true) ||
			contentType.contains("mpegurl", true) || contentType.contains("m3u8", true)

		return if (isPlaylist) {
			val text = body.string()
			upstream.close()
			val rewritten = rewritePlaylist(text, target, headers)
			val bytes = rewritten.toByteArray()
			newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", ByteArrayInputStream(bytes), bytes.size.toLong()).also {
				it.addHeader("Access-Control-Allow-Origin", "*")
			}
		} else {
			val status = Response.Status.lookup(upstream.code) ?: Response.Status.OK
			val length = upstream.header("Content-Length")?.toLongOrNull()
			val stream: InputStream = body.byteStream()
			val response = if (length != null) {
				newFixedLengthResponse(status, contentType, stream, length)
			} else {
				newChunkedResponse(status, contentType, stream)
			}
			upstream.header("Content-Range")?.let { response.addHeader("Content-Range", it) }
			upstream.header("Accept-Ranges")?.let { response.addHeader("Accept-Ranges", it) }
			response
		}
	}

	/** Points every URI in an HLS playlist back at this proxy, keeping the same headers. */
	private fun rewritePlaylist(text: String, playlistUrl: String, headers: Map<String, String>): String {
		val base = Uri.parse(playlistUrl)
		fun absolute(ref: String): String = try {
			val t = ref.trim()
			when {
				t.startsWith("http://") || t.startsWith("https://") -> t
				t.startsWith("//") -> "${base.scheme}:$t"
				t.startsWith("/") -> "${base.scheme}://${base.authority}$t"
				else -> {
					val path = playlistUrl.substringBefore('?').substringBeforeLast('/')
					"$path/$t"
				}
			}
		} catch (_: Exception) {
			ref
		}
		val uriAttr = Regex("""URI="([^"]+)"""")
		return text.lines().joinToString("\n") { line ->
			when {
				line.isBlank() -> line
				line.startsWith("#") -> uriAttr.replace(line) { m -> "URI=\"${proxiedUrl(absolute(m.groupValues[1]), headers)}\"" }
				else -> proxiedUrl(absolute(line), headers)
			}
		}
	}

	private fun bad(msg: String) = newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", msg)

	private fun b64(s: String): String = Base64.encodeToString(s.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
	private fun unb64(s: String): String = String(Base64.decode(s, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))

	companion object {
		private const val TAG = "StreamProxy"
	}
}
