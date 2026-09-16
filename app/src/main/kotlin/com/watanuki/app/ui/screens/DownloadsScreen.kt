package com.watanuki.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.watanuki.app.R
import com.watanuki.app.download.DownloadItem
import com.watanuki.app.download.DownloadRepository
import com.watanuki.app.download.DownloadService
import com.watanuki.app.download.humanSize
import com.watanuki.app.ui.komi.KomiBadge
import com.watanuki.app.ui.komi.KomiBadgeTone
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiButtonSize
import com.watanuki.app.ui.komi.KomiButtonVariant
import com.watanuki.app.ui.komi.KomiLinearProgress
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.LocalPersonality
import androidx.compose.ui.platform.LocalContext

@Composable
fun DownloadsScreen(contentPadding: PaddingValues, onPlay: (DownloadItem) -> Unit) {
	val items by DownloadRepository.items.collectAsState()
	val colors = LocalPersonality.current.colors
	val context = LocalContext.current
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = contentPadding.calculateTopPadding() + 4.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item { KomiSectionHead(label = "${items.size} ${stringResource(R.string.downloads)}", kicker = "保存") }
		if (items.isEmpty()) {
			item {
				Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
					KomiText(text = stringResource(R.string.downloads_empty), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false, textAlign = TextAlign.Center)
				}
			}
		}
		items(items, key = { it.id }) { item ->
			KomiListContainer {
				val subtitle = when (item.status) {
					DownloadItem.STATUS_DONE -> "${item.sourceName} · ${humanSize(item.bytes)}"
					DownloadItem.STATUS_FAILED -> item.error ?: stringResource(R.string.download_failed)
					DownloadItem.STATUS_RUNNING -> if (item.total > 0) "${(item.bytes * 100 / item.total).toInt()} % · ${humanSize(item.bytes)}" else if (item.url.isBlank()) stringResource(R.string.download_resolving) else humanSize(item.bytes)
					else -> stringResource(R.string.download_queued)
				}
				KomiListRow(
					title = "${item.animeTitle} · ${item.episodeName}",
					subtitle = subtitle,
					onClick = { if (item.status == DownloadItem.STATUS_DONE) onPlay(item) },
					trailing = {
						when (item.status) {
							DownloadItem.STATUS_DONE -> KomiBadge(text = "OK", tone = KomiBadgeTone.Neutral)
							DownloadItem.STATUS_FAILED -> KomiBadge(text = "!", tone = KomiBadgeTone.Alert, tilt = true)
							else -> Unit
						}
					},
				)
				if (item.status == DownloadItem.STATUS_RUNNING) {
					KomiLinearProgress(
						modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 10.dp),
						progress = if (item.total > 0) ({ (item.bytes.toFloat() / item.total).coerceIn(0f, 1f) }) else null,
					)
				}
				Row(Modifier.padding(horizontal = 14.dp).padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					if (item.status == DownloadItem.STATUS_DONE) {
						KomiButton(onClick = { onPlay(item) }, label = stringResource(R.string.play), size = KomiButtonSize.Sm)
					}
					if (item.status == DownloadItem.STATUS_FAILED) {
						KomiButton(
							onClick = {
								// these CDN links die within minutes, so a retry asks the source for a fresh one
							DownloadRepository.update(item.id) {
								if (it.episodeUrl != null && it.sourceId != null) it.copy(status = DownloadItem.STATUS_QUEUED, error = null, url = "", headers = emptyMap())
								else it.copy(status = DownloadItem.STATUS_QUEUED, error = null)
							}
								DownloadService.start(context)
							},
							label = stringResource(R.string.retry), size = KomiButtonSize.Sm,
						)
					}
					KomiButton(onClick = { DownloadRepository.remove(item) }, label = stringResource(R.string.delete), size = KomiButtonSize.Sm, variant = KomiButtonVariant.Outline)
				}
			}
		}
	}
}
