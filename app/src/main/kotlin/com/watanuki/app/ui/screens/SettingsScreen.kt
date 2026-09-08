package com.watanuki.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watanuki.app.BuildConfig
import com.watanuki.app.R
import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.komi.KomiCheckbox
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSegmented
import com.watanuki.app.ui.komi.KomiSegmentedItem
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.LocalPersonality

/** Settings: appearance (Touhou palettes, mode), content, playback, storage. */
@Composable
fun SettingsScreen(contentPadding: PaddingValues, onOpenDownloads: () -> Unit) {
	val colors = LocalPersonality.current.colors
	Column(
		modifier = Modifier
			.fillMaxSize()
			.padding(contentPadding)
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 16.dp)
			.padding(top = 4.dp, bottom = 24.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		KomiSectionHead(label = stringResource(R.string.appearance), kicker = "外観")
		ThemePicker()

		KomiSectionHead(label = stringResource(R.string.content), kicker = "内容")
		AdultRow()

		KomiSectionHead(label = stringResource(R.string.playback), kicker = "再生")
		KomiListContainer {
			KomiListRow(
				title = stringResource(R.string.auto_server),
				subtitle = stringResource(R.string.auto_server_summary),
				onClick = { AppPrefs.updateAutoSelectServer(!AppPrefs.autoSelectServer) },
				trailing = { KomiCheckbox(checked = AppPrefs.autoSelectServer, onCheckedChange = { AppPrefs.updateAutoSelectServer(it) }) },
			)
		}

		KomiSectionHead(label = stringResource(R.string.downloads), kicker = "保存")
		KomiListContainer {
			KomiListRow(title = stringResource(R.string.downloads), subtitle = stringResource(R.string.downloads_summary), onClick = onOpenDownloads, showDivider = true)
			KomiListRow(
				title = stringResource(R.string.parallel_downloads),
				subtitle = stringResource(R.string.parallel_downloads_summary),
				trailing = {
					KomiSegmented(
						selected = AppPrefs.parallelDownloads,
						items = listOf(1, 2, 3, 4).map { KomiSegmentedItem(it, it.toString()) },
						onSelect = { AppPrefs.updateParallelDownloads(it) },
						height = 34.dp,
					)
				},
			)
		}

		KomiSectionHead(label = stringResource(R.string.about), kicker = "情報")
		AboutSection()

		KomiText(
			text = "― ${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME} ―",
			role = KomiTextRole.Stamp, color = colors.onSurfaceVariant, fontSize = 11.sp, uppercase = false, textAlign = TextAlign.Center,
			modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
		)
	}
}
