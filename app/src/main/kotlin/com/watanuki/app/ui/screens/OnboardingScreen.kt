package com.watanuki.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.watanuki.app.R
import com.watanuki.app.ui.AppPrefs
import com.watanuki.app.ui.komi.KomiButton
import com.watanuki.app.ui.komi.KomiCheckbox
import com.watanuki.app.ui.komi.KomiListContainer
import com.watanuki.app.ui.komi.KomiListRow
import com.watanuki.app.ui.komi.KomiScaffold
import com.watanuki.app.ui.komi.KomiSectionHead
import com.watanuki.app.ui.komi.KomiSegmented
import com.watanuki.app.ui.komi.KomiSegmentedItem
import com.watanuki.app.ui.komi.KomiText
import com.watanuki.app.ui.komi.KomiTextRole
import com.watanuki.app.ui.komi.KomiTopBar
import com.watanuki.app.ui.komi.LocalPersonality
import com.watanuki.app.ui.komi.ThemePrefs
import com.watanuki.app.ui.komi.Themes

/** First start: adult content choice (off by default) and the look of the app. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
	val colors = LocalPersonality.current.colors
	KomiScaffold(
		topBar = { KomiTopBar(title = stringResource(R.string.app_name), titleAccent = "nuki", subtitle = stringResource(R.string.onboarding_kicker)) },
	) { padding ->
		Column(Modifier.fillMaxSize().padding(padding)) {
			Column(
				modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp),
			) {
				KomiText(text = stringResource(R.string.onboarding_title), role = KomiTextRole.Display)
				KomiText(text = stringResource(R.string.onboarding_text), role = KomiTextRole.Body, color = colors.onSurfaceVariant, uppercase = false)

				KomiSectionHead(label = stringResource(R.string.content), kicker = "内容")
				AdultRow()

				KomiSectionHead(label = stringResource(R.string.appearance), kicker = "外観")
				ThemePicker()
			}
			Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.End) {
				KomiButton(onClick = { AppPrefs.setOnboardingDone(); onDone() }, label = stringResource(R.string.start), emphasized = true)
			}
		}
	}
}

@Composable
fun AdultRow() {
	KomiListContainer {
		KomiListRow(
			title = stringResource(R.string.show_adult),
			subtitle = stringResource(R.string.show_adult_summary),
			onClick = { AppPrefs.updateShowAdult(!AppPrefs.showAdult) },
			trailing = { KomiCheckbox(checked = AppPrefs.showAdult, onCheckedChange = { AppPrefs.updateShowAdult(it) }) },
		)
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePicker() {
	val colors = LocalPersonality.current.colors
	Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
		KomiSegmented(
			selected = ThemePrefs.mode,
			items = listOf(
				KomiSegmentedItem("system", stringResource(R.string.mode_system)),
				KomiSegmentedItem("light", stringResource(R.string.mode_light)),
				KomiSegmentedItem("dark", stringResource(R.string.mode_dark)),
			),
			onSelect = { ThemePrefs.selectMode(it) },
			modifier = Modifier.fillMaxWidth(),
			fillWidth = true,
		)
		FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
			Themes.all.forEach { theme ->
				val selected = theme.id == ThemePrefs.themeId
				val palette = if (colors.isDark) theme.dark else theme.light
				Row(
					modifier = Modifier
						.background(if (selected) colors.primary else colors.surface)
						.border(2.5.dp, colors.outline)
						.clickable { ThemePrefs.selectTheme(theme.id) }
						.padding(horizontal = 10.dp, vertical = 8.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					Row {
						Swatch(Color(palette.primary)); Swatch(Color(palette.secondary)); Swatch(Color(palette.gold))
					}
					KomiText(text = theme.name, role = KomiTextRole.Label, color = if (selected) colors.onPrimary else colors.onSurface, uppercase = false, fontSize = 12.sp)
				}
			}
		}
	}
}

@Composable
private fun Swatch(color: Color) {
	val colors = LocalPersonality.current.colors
	Box(Modifier.size(14.dp).background(color).border(1.5.dp, colors.outline))
	Spacer(Modifier.width(2.dp))
}
