package com.metamatch.app.ui.publish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.exception.MicroFeeRequiredException
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RideShareIntent
import com.metamatch.app.domain.repository.RideShareRepository
import com.metamatch.app.domain.usecase.PublishRideIntentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

/**
 * PublishIntentViewModel
 * ========================
 *
 * WHAT: backs the "Publish Intent" screen — holds every form field the
 * user is editing, and turns a tap on "Publish" into a call through
 * [PublishRideIntentUseCase].
 *
 * WHY the form fields live here and not inside the Composable itself:
 * Compose recomposes constantly (every time anything on screen changes),
 * and a plain `var` inside a `@Composable` function would be wiped out on
 * configuration changes (e.g. rotating the phone) unless wrapped in
 * `rememberSaveable`, which gets unwieldy for a form with this many
 * fields. A `ViewModel` survives configuration changes automatically and
 * is the conventional MVVM home for "the current state of a screen."
 *
 * HOW the anti-spam counter stays live: [activeIntentCount] is collected
 * from [RideShareRepository.observeActiveIntentsForUser] — a [kotlinx.
 * coroutines.flow.Flow] — inside `init`, so the "X / 5 FREE" badge updates
 * the instant a publish succeeds, with no manual refresh call needed
 * anywhere.
 *
 * Kotlin note: the whole screen's state is one immutable [PublishUiState]
 * data class, replaced wholesale on every change via `_uiState.update { }`.
 * This "single state object per screen" pattern (sometimes called a
 * Unidirectional Data Flow / UDF) is the standard modern approach to
 * Android ViewModels: the Composable never mutates state directly, it
 * only ever calls a ViewModel function, which computes a brand new
 * `PublishUiState` and publishes it.
 */
@HiltViewModel
class PublishIntentViewModel @Inject constructor(
    private val repository: RideShareRepository,
    private val publishRideIntentUseCase: PublishRideIntentUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID
    private val currentUserEmail = "demo.rider@ucaribe.edu.mx"

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    init {
        // Seed the date/time picker with the exact same instant the
        // "seed-cancun-1" demo intent is scheduled for (converted to this
        // device's own local wall-clock time), so a first-time user who
        // accepts every default and taps Publish immediately lands within
        // FindMatchesUseCase's schedule-tolerance window of that seed —
        // regardless of what timezone this device happens to be set to.
        // See MockRideShareRepository.DEMO_RIDE_SCHEDULED_AT for why this
        // is computed at runtime instead of a fixed calendar date.
        val demoLocalDateTime = MockRideShareRepository.DEMO_RIDE_SCHEDULED_AT
            .toLocalDateTime(TimeZone.currentSystemDefault())
        _uiState.update {
            it.copy(
                year = demoLocalDateTime.year,
                month = demoLocalDateTime.monthNumber,
                day = demoLocalDateTime.dayOfMonth,
                hour = demoLocalDateTime.hour,
                minute = demoLocalDateTime.minute,
            )
        }

        viewModelScope.launch {
            val policy = repository.getPlatformPolicy()
            _uiState.update { it.copy(freeIntentLimit = policy.freeActiveIntentLimit) }
        }
        viewModelScope.launch {
            repository.observeActiveIntentsForUser(currentUserId).collect { activeIntents ->
                _uiState.update { it.copy(activeIntentCount = activeIntents.size) }
            }
        }
    }

    fun onDepartureChanged(lat: String, lng: String) {
        _uiState.update { it.copy(departureLat = lat, departureLng = lng) }
    }

    fun onDestinationChanged(lat: String, lng: String) {
        _uiState.update { it.copy(destinationLat = lat, destinationLng = lng) }
    }

    fun onDateSelected(year: Int, month: Int, day: Int) {
        _uiState.update { it.copy(year = year, month = month, day = day) }
    }

    fun onTimeSelected(hour: Int, minute: Int) {
        _uiState.update { it.copy(hour = hour, minute = minute) }
    }

    fun onMaxWalkingDistanceChanged(value: String) {
        _uiState.update { it.copy(maxWalkingDistanceMeters = value) }
    }

    fun onBudgetChanged(value: String) {
        _uiState.update { it.copy(budgetContribution = value) }
    }

    fun onAllowedDomainsChanged(value: String) {
        _uiState.update { it.copy(allowedDomainsRaw = value) }
    }

    fun onLegalConsentChanged(acknowledged: Boolean) {
        _uiState.update { it.copy(legalConsentAcknowledged = acknowledged) }
    }

    fun dismissMicroFeeDialog() {
        _uiState.update { it.copy(pendingMicroFeeCents = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /** Publish attempt #1 — will surface a micro-fee dialog instead of
     * publishing if the user is already at their free-tier limit. */
    fun onPublishClicked() = publish(acceptsMicroFee = false)

    /** Called after the user explicitly confirms the micro-fee dialog. */
    fun onMicroFeeConfirmed() = publish(acceptsMicroFee = true)

    private fun publish(acceptsMicroFee: Boolean) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            try {
                // Building the intent can throw (e.g. a latitude typed
                // outside -90..90 trips GeoPoint's own validation) — kept
                // inside this try block so any such mistake surfaces as a
                // friendly error message instead of crashing the screen.
                val intent = buildIntentOrNull(state)
                    ?: throw IllegalArgumentException(
                        "Please check the form: all fields must be valid numbers, and you must agree to the legal notice.",
                    )
                publishRideIntentUseCase(intent, acceptsMicroFee = acceptsMicroFee)
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        pendingMicroFeeCents = null,
                        successMessage = "Intent published! Check Match Results.",
                    )
                }
            } catch (fee: MicroFeeRequiredException) {
                _uiState.update {
                    it.copy(isPublishing = false, pendingMicroFeeCents = fee.feeAmountCents)
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(isPublishing = false, errorMessage = t.message ?: "Could not publish this intent.")
                }
            }
        }
    }

    private fun buildIntentOrNull(state: PublishUiState): RideShareIntent? {
        val departureLat = state.departureLat.toDoubleOrNull() ?: return null
        val departureLng = state.departureLng.toDoubleOrNull() ?: return null
        val destinationLat = state.destinationLat.toDoubleOrNull() ?: return null
        val destinationLng = state.destinationLng.toDoubleOrNull() ?: return null
        val maxWalking = state.maxWalkingDistanceMeters.toDoubleOrNull() ?: return null
        val budget = state.budgetContribution.toDoubleOrNull() ?: 0.0

        val scheduledLocalDateTime = LocalDateTime(
            year = state.year,
            monthNumber = state.month,
            dayOfMonth = state.day,
            hour = state.hour,
            minute = state.minute,
        )
        val scheduledAt = scheduledLocalDateTime.toInstant(TimeZone.currentSystemDefault())
        // A generous 4-hour window after the scheduled time covers
        // "picked up a bit late" without keeping stale intents around
        // forever.
        val expiresAt = scheduledAt.plus(4.hours)

        val allowedDomains = state.allowedDomainsRaw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        if (!state.legalConsentAcknowledged) return null

        return RideShareIntent(
            id = UUID.randomUUID().toString(),
            creatorUserId = currentUserId,
            creatorEmail = currentUserEmail,
            createdAt = Clock.System.now(),
            scheduledAt = scheduledAt,
            expiresAt = expiresAt,
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = budget),
            status = ContractStatus.ACTIVE,
            departure = GeoPoint(departureLat, departureLng),
            destination = GeoPoint(destinationLat, destinationLng),
            maxWalkingDistanceMeters = maxWalking,
            allowedEmailDomains = allowedDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        )
    }
}

/**
 * PublishUiState
 * ================
 * Every field the Publish screen needs to render, as one immutable
 * snapshot. Kept as simple `String` inputs for numeric fields (lat/lng,
 * distance, budget) rather than `Double`, because that is what a
 * Compose `TextField` actually produces as the user types — parsing to
 * `Double` only happens once, at publish time, in
 * [PublishIntentViewModel.buildIntentOrNull].
 *
 * The `year`/`month`/`day`/`hour`/`minute` defaults below are placeholders
 * only — [PublishIntentViewModel]'s `init` block immediately overwrites
 * them with [MockRideShareRepository.DEMO_RIDE_SCHEDULED_AT] converted to
 * local time, so the picker starts out aligned with the "seed-cancun-1"
 * demo intent (María, UCaribe → Cancún Airport) regardless of device
 * timezone — see that constant's docs for why it's computed at runtime
 * rather than a fixed calendar date.
 */
data class PublishUiState(
    val departureLat: String = "21.0894",
    val departureLng: String = "-86.8459",
    val destinationLat: String = "21.0370",
    val destinationLng: String = "-86.8760",
    val year: Int = 2026,
    val month: Int = 8,
    val day: Int = 15,
    val hour: Int = 7,
    val minute: Int = 45,
    val maxWalkingDistanceMeters: String = "400",
    val budgetContribution: String = "0",
    val allowedDomainsRaw: String = "ucaribe.edu.mx, anahuac.mx, xcaret.com",
    val legalConsentAcknowledged: Boolean = false,
    val activeIntentCount: Int = 0,
    val freeIntentLimit: Int = 5,
    val isPublishing: Boolean = false,
    val pendingMicroFeeCents: Int? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
