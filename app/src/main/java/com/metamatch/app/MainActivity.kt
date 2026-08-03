package com.metamatch.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.metamatch.app.ui.components.RetroButton
import com.metamatch.app.ui.hub.HubScreen
import com.metamatch.app.ui.intro.IntroScreen
import com.metamatch.app.ui.match.MatchResultsScreen
import com.metamatch.app.ui.publish.PublishIntentScreen
import com.metamatch.app.ui.theme.MetaMatchTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 * ==============
 *
 * WHAT: the single `Activity` Wishing Well has in this MVP pass. Hosts a
 * [androidx.navigation.compose.NavHost] with a real back stack: the intro
 * splash, the vertical hub, and (today) the Ride vertical's own
 * Publish/Matches flow. Pizza and Roomie will each add one more route
 * here once their own stages land — the hub already has cards for them
 * (see `ui/hub/HubScreen.kt`).
 *
 * WHY this now uses Navigation-Compose, where earlier iterations
 * deliberately used a plain `enum`/`when`: that earlier choice's own doc
 * comment named the exact trigger for switching — "when a screen needs a
 * real back stack, tapping into something and pressing back." The hub
 * screen is precisely that: Ride's Publish/Matches flow is now pushed on
 * top of the hub, and the hub is pushed on top of the intro splash, with
 * the system back button correctly unwinding one level at a time.
 *
 * HOW Hilt wires into this Activity: `@AndroidEntryPoint` is what allows
 * `hiltViewModel()` inside `PublishIntentScreen`/`MatchResultsScreen` to
 * resolve their `@HiltViewModel`-annotated ViewModels — without this
 * annotation, Hilt has no entry point into this Activity's dependency
 * graph and those calls would fail at runtime.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MetaMatchTheme {
                WishingWellApp()
            }
        }
    }
}

private object Routes {
    const val INTRO = "intro"
    const val HUB = "hub"
    const val RIDE = "ride"
}

@Composable
private fun WishingWellApp() {
    val navController = rememberNavController()

    // A plain NavHost paints nothing on its own — only its children's own
    // content draws. Earlier, the single-screen `Scaffold` filled this role
    // via `containerColor`; now that the intro/hub screens sit above any
    // Scaffold, one Surface here paints the retro background for the whole
    // app, once, instead of every screen having to remember to do it.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(navController = navController, startDestination = Routes.INTRO) {
            composable(Routes.INTRO) {
                IntroScreen(
                    onTossCoin = {
                        navController.navigate(Routes.HUB) {
                            popUpTo(Routes.INTRO) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HUB) {
                HubScreen(onOpenRide = { navController.navigate(Routes.RIDE) })
            }
            composable(Routes.RIDE) {
                RideVerticalScreen(navController = navController)
            }
        }
    }
}

private enum class RideTab { PUBLISH, MATCHES }

/**
 * The existing Ride vertical, unchanged in substance — still a plain tab
 * switch between Publish and Matches, since neither of those two screens
 * itself pushes a "detail" screen on top (the same reasoning that
 * originally justified skipping Navigation-Compose still holds one level
 * down; only the *top* of the app needed a real back stack). A "← Hub"
 * button returns to the vertical picker.
 */
@Composable
private fun RideVerticalScreen(navController: NavHostController) {
    var currentTab by rememberSaveable { mutableStateOf(RideTab.PUBLISH) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RetroButton(
                    label = "← Hub",
                    onClick = { navController.popBackStack() },
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                RetroButton(
                    label = "Publish",
                    onClick = { currentTab = RideTab.PUBLISH },
                    backgroundColor = if (currentTab == RideTab.PUBLISH) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (currentTab == RideTab.PUBLISH) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                RetroButton(
                    label = "Matches",
                    onClick = { currentTab = RideTab.MATCHES },
                    backgroundColor = if (currentTab == RideTab.MATCHES) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    contentColor = if (currentTab == RideTab.MATCHES) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (currentTab) {
                RideTab.PUBLISH -> PublishIntentScreen()
                RideTab.MATCHES -> MatchResultsScreen()
            }
        }
    }
}
