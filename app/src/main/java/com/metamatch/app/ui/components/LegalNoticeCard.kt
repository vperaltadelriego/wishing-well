package com.metamatch.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metamatch.app.ui.theme.RetroTextMuted
import com.metamatch.app.ui.theme.RetroYellow

/**
 * LegalNoticeCard
 * =================
 *
 * WHAT: the consent gate every "Publish" screen shows before a user can
 * create an intent — a plain-language summary of what publishing actually
 * means legally, plus a checkbox the caller uses to enable/disable its own
 * Publish button. Deliberately kept legible (regular body text, no retro
 * theming tricks on the copy itself) — a consent notice that is hard to
 * read defeats its own purpose.
 *
 * WHY this is one shared component instead of copy-pasted text per
 * vertical: every kind of contract MetaMatch will ever support (rides
 * today; pizza splits, roommate searches, and whatever comes after) shares
 * the same baseline legal reality — the platform introduces people, it is
 * not a party to what they agree to. [baselineBulletPoints] captures that
 * once; [extraBulletPoints] is where a specific vertical adds its own
 * clauses (e.g. a roommate search should probably add "put a move-in
 * agreement in writing") without duplicating the shared baseline.
 *
 * HOW the checkbox connects to [com.metamatch.app.domain.model.ContractIntent
 * .legalConsentAcknowledgedAt]: the caller's ViewModel is expected to store
 * the moment [onAcknowledgedChanged] first reports `true`, and stamp that
 * timestamp onto the intent being built — see
 * [com.metamatch.app.ui.publish.PublishIntentViewModel] for the concrete
 * wiring. This component itself has no notion of time; it only reports a
 * boolean.
 */
@Composable
fun LegalNoticeCard(
    acknowledged: Boolean,
    onAcknowledgedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    extraBulletPoints: List<String> = emptyList(),
) {
    RetroCard(modifier = modifier.fillMaxWidth()) {
        Text("BEFORE YOU PUBLISH", style = MaterialTheme.typography.titleLarge)
        Column(modifier = Modifier.padding(top = 8.dp)) {
            (baselineBulletPoints + extraBulletPoints).forEach { point ->
                Text(
                    text = "• $point",
                    style = MaterialTheme.typography.bodyMedium,
                    color = RetroTextMuted,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = acknowledged,
                onCheckedChange = onAcknowledgedChanged,
                colors = CheckboxDefaults.colors(checkedColor = RetroYellow),
            )
            Text(
                text = "I have read this and agree to it.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val baselineBulletPoints = listOf(
    "MetaMatch introduces you to other users — it is not a party to any " +
        "agreement you make with them, and does not guarantee anyone will " +
        "show up or follow through.",
    "You must be at least 18 years old to publish or accept an intent.",
    "Any money involved changes hands directly between users; MetaMatch's " +
        "only charge is the micro-fee described on this screen, if it applies.",
    "You are responsible for verifying who you are actually meeting in " +
        "person — treat a match as an introduction, not a guarantee.",
    "Canceling after you've been matched affects your integrity score, " +
        "which other users can see before matching with you.",
)
