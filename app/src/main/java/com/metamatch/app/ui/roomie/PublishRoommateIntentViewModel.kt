package com.metamatch.app.ui.roomie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metamatch.app.data.mock.MockRideShareRepository
import com.metamatch.app.data.mock.MockRoommateRepository
import com.metamatch.app.domain.exception.MicroFeeRequiredException
import com.metamatch.app.domain.model.ContractStatus
import com.metamatch.app.domain.model.FinancialTerms
import com.metamatch.app.domain.model.IdentityVerificationTier
import com.metamatch.app.domain.model.RoommateIntent
import com.metamatch.app.domain.model.RoommateRole
import com.metamatch.app.domain.repository.RoommateRepository
import com.metamatch.app.domain.usecase.PublishRoommateIntentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import javax.inject.Inject

/**
 * PublishRoommateIntentViewModel
 * ================================
 *
 * WHAT: backs the "Publish a Roomie listing" screen — the Roomie
 * vertical's twin of
 * [com.metamatch.app.ui.publish.PublishIntentViewModel]/
 * [com.metamatch.app.ui.pizza.PublishPizzaIntentViewModel]. Same
 * Unidirectional Data Flow pattern.
 *
 * WHY the default [RoommateUiState.role] is [RoommateRole.SEEKING] with
 * an 8,000 MXN budget: [MockRoommateRepository]'s seeded listing offers
 * the same zone at 8,500 MXN — accepting every default here and
 * publishing produces the exact "8,500 ask vs. 8,000 budget" scenario the
 * product brief describes, immediately demonstrating both the
 * price-gap badge and the "Adjust My Price" negotiation feature on the
 * Match Results screen.
 */
@HiltViewModel
class PublishRoommateIntentViewModel @Inject constructor(
    private val repository: RoommateRepository,
    private val publishRoommateIntentUseCase: PublishRoommateIntentUseCase,
) : ViewModel() {

    private val currentUserId = MockRideShareRepository.CURRENT_DEMO_USER_ID
    private val currentUserEmail = "demo.rider@ucaribe.edu.mx"

    private val _uiState = MutableStateFlow(RoommateUiState())
    val uiState: StateFlow<RoommateUiState> = _uiState.asStateFlow()

    init {
        val startDate = MockRoommateRepository.DEMO_MOVE_IN_WINDOW_START.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val endDate = MockRoommateRepository.DEMO_MOVE_IN_WINDOW_END.toLocalDateTime(TimeZone.currentSystemDefault()).date
        _uiState.update {
            it.copy(
                moveInStartYear = startDate.year,
                moveInStartMonth = startDate.monthNumber,
                moveInStartDay = startDate.dayOfMonth,
                moveInEndYear = endDate.year,
                moveInEndMonth = endDate.monthNumber,
                moveInEndDay = endDate.dayOfMonth,
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

    fun onRoleChanged(role: RoommateRole) {
        _uiState.update { it.copy(role = role) }
    }

    fun onZoneChanged(value: String) {
        _uiState.update { it.copy(zone = value) }
    }

    fun onPropertyDescriptionChanged(value: String) {
        _uiState.update { it.copy(propertyDescription = value) }
    }

    fun onMoveInStartSelected(year: Int, month: Int, day: Int) {
        _uiState.update { it.copy(moveInStartYear = year, moveInStartMonth = month, moveInStartDay = day) }
    }

    fun onMoveInEndSelected(year: Int, month: Int, day: Int) {
        _uiState.update { it.copy(moveInEndYear = year, moveInEndMonth = month, moveInEndDay = day) }
    }

    fun onLeaseDurationChanged(value: String) {
        _uiState.update { it.copy(leaseDurationMonths = value) }
    }

    fun onPriceChanged(value: String) {
        _uiState.update { it.copy(priceAmount = value) }
    }

    fun onDepositChanged(value: String) {
        _uiState.update { it.copy(depositAmount = value) }
    }

    fun onGuarantorArrangementChanged(value: String) {
        _uiState.update { it.copy(guarantorArrangement = value) }
    }

    fun onPreferenceNotesChanged(value: String) {
        _uiState.update { it.copy(preferenceNotes = value) }
    }

    fun onThirdPartyArrangementChanged(isThirdParty: Boolean) {
        _uiState.update { it.copy(isThirdPartyArrangement = isThirdParty) }
    }

    fun onOccupantDescriptionChanged(value: String) {
        _uiState.update { it.copy(occupantDescription = value) }
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
                        "Please check the form: all fields must be valid, and you must agree to the legal notice.",
                    )
                publishRoommateIntentUseCase(intent, acceptsMicroFee = acceptsMicroFee)
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

    private fun buildIntentOrNull(state: RoommateUiState): RoommateIntent? {
        val leaseDuration = state.leaseDurationMonths.toIntOrNull() ?: return null
        val price = state.priceAmount.toDoubleOrNull() ?: return null
        val deposit = state.depositAmount.toDoubleOrNull() ?: return null
        if (state.zone.isBlank() || state.propertyDescription.isBlank()) return null
        if (state.isThirdPartyArrangement && state.occupantDescription.isBlank()) return null
        if (!state.legalConsentAcknowledged) return null

        val timeZone = TimeZone.currentSystemDefault()
        val moveInStart = LocalDate(state.moveInStartYear, state.moveInStartMonth, state.moveInStartDay)
            .atStartOfDayIn(timeZone)
        val moveInEnd = LocalDate(state.moveInEndYear, state.moveInEndMonth, state.moveInEndDay)
            .atStartOfDayIn(timeZone)
        if (moveInEnd < moveInStart) return null

        val allowedDomains = state.allowedDomainsRaw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()

        return RoommateIntent(
            id = UUID.randomUUID().toString(),
            creatorUserId = currentUserId,
            creatorEmail = currentUserEmail,
            createdAt = Clock.System.now(),
            scheduledAt = moveInStart,
            expiresAt = moveInEnd,
            verificationTier = IdentityVerificationTier.EMAIL_ONLY,
            financialTerms = FinancialTerms(amount = price),
            status = ContractStatus.ACTIVE,
            role = state.role,
            zone = state.zone,
            propertyDescription = state.propertyDescription,
            moveInWindowStart = moveInStart,
            moveInWindowEnd = moveInEnd,
            leaseDurationMonths = leaseDuration,
            depositAmount = deposit,
            guarantorArrangement = state.guarantorArrangement,
            preferenceNotes = state.preferenceNotes,
            isThirdPartyArrangement = state.isThirdPartyArrangement,
            occupantDescription = state.occupantDescription,
            allowedEmailDomains = allowedDomains,
            legalConsentAcknowledgedAt = Clock.System.now(),
        )
    }
}

/**
 * RoommateUiState
 * =================
 * Every field the Roomie Publish screen needs, as one immutable snapshot
 * — same reasoning as [com.metamatch.app.ui.publish.PublishUiState].
 * Defaults are chosen to produce, out of the box, the "8,500 asking vs.
 * 8,000 budget" scenario against [MockRoommateRepository]'s seeded
 * listing — see this ViewModel's own class doc.
 */
data class RoommateUiState(
    val role: RoommateRole = RoommateRole.SEEKING,
    val zone: String = "Cancún - Región 15",
    val propertyDescription: String = "Busco cuarto amueblado cerca del campus.",
    val moveInStartYear: Int = 2026,
    val moveInStartMonth: Int = 8,
    val moveInStartDay: Int = 15,
    val moveInEndYear: Int = 2026,
    val moveInEndMonth: Int = 10,
    val moveInEndDay: Int = 15,
    val leaseDurationMonths: String = "12",
    val priceAmount: String = "8000",
    val depositAmount: String = "8000",
    val guarantorArrangement: String = "Aval o depósito adicional de un mes.",
    val preferenceNotes: String = "",
    val isThirdPartyArrangement: Boolean = false,
    val occupantDescription: String = "",
    val allowedDomainsRaw: String = "ucaribe.edu.mx, anahuac.mx, xcaret.com",
    val legalConsentAcknowledged: Boolean = false,
    val activeIntentCount: Int = 0,
    val freeIntentLimit: Int = 5,
    val isPublishing: Boolean = false,
    val pendingMicroFeeCents: Int? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
