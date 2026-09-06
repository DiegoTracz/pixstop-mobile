package com.pixstop.mobile.data.repository

import com.pixstop.mobile.core.config.ApiConfig
import com.pixstop.mobile.core.network.safeCall
import com.pixstop.mobile.data.remote.dto.ReferralDto
import com.pixstop.mobile.data.remote.dto.RewardsPageDto
import com.pixstop.mobile.data.remote.dto.VoucherDto
import com.pixstop.mobile.domain.model.Outcome
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post

/**
 * Recompensas por voucher: o catálogo, o resgate e os vouchers da pessoa.
 */
class RewardRepository(private val client: HttpClient) {

    suspend fun page(): Outcome<RewardsPageDto> =
        safeCall(TAG) { client.get(ApiConfig.Endpoints.REWARDS) }

    /** `null` no sucesso quer dizer que esta empresa não usa indicação. */
    suspend fun referral(): Outcome<ReferralDto?> =
        safeCall(TAG) { client.get(ApiConfig.Endpoints.REFERRALS) }

    suspend fun redeem(rewardId: Long): Outcome<VoucherDto> =
        safeCall(TAG) { client.post(ApiConfig.Endpoints.rewardRedeem(rewardId)) }

    private companion object {
        const val TAG = "Rewards"
    }
}
