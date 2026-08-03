package com.metamatch.app.ui.pizza

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockPizzaShareRepository
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.domain.exception.MicroFeeRequiredException
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.GeoPoint
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.PizzaShareIntent
import com.metamatch.app.domain.repository.PizzaShareRepository
import com.metamatch.app.domain.usecase.PublishPizzaIntentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.plus
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours

/**
 * PublishPizzaIntentViewModel
 * =============================
 *
 * WHAT: backs the "Publish a shared purchase" screen for Meta-Match
 * Pizza — the Pizza vertical's twin of
 * [com.metamatch.app.ui.publish.PublishIntentViewModel]. Same
 * Unidirectional Data Flow pattern: one immutable [PizzaUiState],
 * replaced wholesale on every change.
 *
 * WHY there is no date/time picker here, unlike Ride: Pizza demand is
 * overwhelmingly "I want this now" (see `anatomía_de_meta-match_finder.md`
 * §5.2) rather than Ride's "publish today for a trip years out." Instead
 * of exposing pickers for a schedule nobody needs to adjust, this reuses
 * [MockPizzaShareRepository.DEMO_PIZZA_SCHEDULED_AT] directly as
 * `scheduledAt` — the exact same instant the seeded demo counterpart uses,
 * so the guided "accept every default, tap Publish" demo produces an
 * instant match here too, without a UI control that would only ever be
 * left at its default anyway.
 */
@HiltViewModel
class PublishPizzaIntentViewModel @Inject constructor(
    private val repository: PizzaShareRepository,
    private val publishPizzaIntentUseCase: PublishPizzaIntentUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID
    private val currentUserEmail = "demo.rider@ucaribe.edu.mx"

    private val _uiState = MutableStateFlow(PizzaUiState())
    val uiState: StateFlow<PizzaUiState> = _uiState.asStateFlow()

    init {
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

    fun onHomeLocationChanged(lat: String, lng: String) {
        _uiState.update { it.copy(homeLat = lat, homeLng = lng) }
    }

    fun onEstablishmentChanged(value: String) {
        _uiState.update { it.copy(establishment = value) }
    }

    fun onProductDescriptionChanged(value: String) {
        _uiState.update { it.copy(productDescription = value) }
    }

    fun onTotalUnitsChanged(value: String) {
        _uiState.update { it.copy(totalUnits = value) }
    }

    fun onTotalPriceChanged(value: String) {
        _uiState.update { it.copy(totalPriceForWholeOrder = value) }
    }

    fun onDesiredUnitsChanged(value: String) {
        _uiState.update { it.copy(desiredUnits = value) }
    }

    fun onPricePortionChanged(value: String) {
        _uiState.update { it.copy(pricePortion = value) }
    }

    fun onMaxDistanceChanged(value: String) {
        _uiState.update { it.copy(maxDistanceMeters = value) }
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

    fun onPublishClicked() = publish(acceptsMicroFee = false)

    fun onMicroFeeConfirmed() = publish(acceptsMicroFee = true)

    private fun publish(acceptsMicroFee: Boolean) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            try {
                val intent = buildIntentOrNull(state)
                    ?: throw IllegalArgumentException(
                        "Please check the form: all fields must be valid numbers, and you must agree to the legal notice.",
                    )
                publishPizzaIntentUseCase(intent, acceptsMicroFee = acceptsMicroFee)
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

    private fun buildIntentOrNull(state: PizzaUiState): PizzaShareIntent? {
        val homeLat = state.homeLat.toDoubleOrNull() ?: return null
        val homeLng = state.homeLng.toDoubleOrNull() ?: return null
        val totalUnits = state.totalUnits.toIntOrNull() ?: return null
        val totalPrice = state.totalPriceForWholeOrder.toDoubleOrNull() ?: return null
        val desiredUnits = state.desiredUnits.toIntOrNull() ?: return null
        val price = state.pricePortion.toDoubleOrNull() ?: return null
        val maxDistance = state.maxDistanceMeters.toDoubleOrNull() ?: return null
        if (state.establishment.isBlank() || state.productDescription.isBlank()) return null
        if (!state.legalConsentAcknowledged) return null

        val allowedDomains = state.allowedDomainsRaw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        val scheduledAt = MockPizzaShareRepository.DEMO_PIZZA_SCHEDULED_AT

        return PizzaShareIntent(
            id = UUID.randomUUID().toString(),
            creatorUserId = currentUserId,
            creatorEmail = currentUserEmail,
            createdAt = Clock.System.now(),
            scheduledAt = scheduledAt,
            expiresAt = scheduledAt.plus(1.hours),
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = price),
            status = ContractStatus.ACTIVE,
            homeLocation = GeoPoint(homeLat, homeLng),
            establishment = state.establishment,
            productDescription = state.productDescription,
            totalUnits = totalUnits,
            totalPriceForWholeOrder = totalPrice,
            desiredUnits = desiredUnits,
            maxDistanceMeters = maxDistance,
            allowedEmailDomains = allowedDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        )
    }
}

/**
 * PizzaUiState
 * ==============
 * Every field the Pizza Publish screen needs, as one immutable snapshot
 * — same reasoning as
 * [com.metamatch.app.ui.publish.PublishUiState]. Defaults are chosen to
 * exactly match [MockPizzaShareRepository]'s seeded "seed-pizza-carlos"
 * intent (same establishment, product, and a `desiredUnits` of 4 that
 * exactly complements the seed's own 4, filling the 8-slice order) so the
 * guided demo path produces an instant, fully-claimed match.
 */
data class PizzaUiState(
    val homeLat: String = "21.0894",
    val homeLng: String = "-86.8459",
    val establishment: String = "Domino's Pizza",
    val productDescription: String = "Pizza grande especial",
    val totalUnits: String = "8",
    val totalPriceForWholeOrder: String = "200",
    val desiredUnits: String = "4",
    val pricePortion: String = "100",
    val maxDistanceMeters: String = "400",
    val allowedDomainsRaw: String = "ucaribe.edu.mx, anahuac.mx, xcaret.com",
    val legalConsentAcknowledged: Boolean = false,
    val activeIntentCount: Int = 0,
    val freeIntentLimit: Int = 5,
    val isPublishing: Boolean = false,
    val pendingMicroFeeCents: Int? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
