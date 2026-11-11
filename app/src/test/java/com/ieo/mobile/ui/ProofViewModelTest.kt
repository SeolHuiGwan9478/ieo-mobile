package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.DeliveryProofStatus
import com.ieo.mobile.domain.proof.ProofLink
import com.ieo.mobile.domain.proof.ProofRepository
import com.ieo.mobile.domain.proof.WaitRecord
import com.ieo.mobile.domain.proof.WaitTimes
import com.ieo.mobile.domain.trip.StopType
import com.ieo.mobile.testing.MainDispatcherRule
import com.ieo.mobile.ui.proof.ProofViewModel
import com.ieo.mobile.ui.proof.waitChargeText
import com.ieo.mobile.ui.proof.waitHeadline
import java.time.Instant
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProofViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val at = Instant.parse("2026-11-08T00:00:00Z")

    private fun record(
        workStartedAt: Instant? = at.plusSeconds(3600),
        departedAt: Instant? = at.plusSeconds(7200),
        wait: Long? = 130,
        amount: Long? = 10_000,
        policy: Boolean = true,
    ) = WaitRecord(StopType.PICKUP, "평택", at, workStartedAt, departedAt, wait, amount, policy, false)

    private fun proof(status: DeliveryProofStatus = DeliveryProofStatus.PENDING, photos: Int = 0, signedAt: Instant? = null) =
        DeliveryProof("http://x/r/a", status, null, signedAt, null, photos, null)

    private class Fake(var delivery: DeliveryProof? = null, val issued: DeliveryProof) : ProofRepository {
        val calls = mutableListOf<String>()
        var uploadFails = false
        override suspend fun waits(tripId: Long) = ApiResult.Success(emptyList<WaitRecord>())
        override suspend fun waitProofLink(tripId: Long) = ApiResult.Success(ProofLink("http://x/w/a", Instant.EPOCH))
        override suspend fun editWait(tripId: Long, stop: StopType, times: WaitTimes) =
            ApiResult.Success(WaitRecord(stop, "평택", times.arrivedAt, times.workStartedAt, times.departedAt, 0, 0, true, true))
        override suspend fun deliveryProof(tripId: Long) = ApiResult.Success(delivery)
        override suspend fun issueDeliveryProof(tripId: Long): ApiResult<DeliveryProof> {
            calls += "issue"
            delivery = issued
            return ApiResult.Success(issued)
        }
        override suspend fun uploadPaperPhoto(tripId: Long, jpeg: ByteArray): ApiResult<DeliveryProof> {
            calls += "upload"
            return if (uploadFails) ApiResult.Failure(ApiError.Network) else ApiResult.Success(issued.copy(status = DeliveryProofStatus.PHOTO, photoCount = 1))
        }
    }

    @Test
    fun `대기 문구는 상황마다 다르다`() {
        assertEquals("2시간 10분 기다렸어요", waitHeadline(record()))
        assertEquals("지금까지 25분 기다렸어요", waitHeadline(record(null, null, null, null), at.plusSeconds(1500)))
        assertEquals("청구할 수 있는 대기비 1만 원", waitChargeText(record()))
        assertEquals("무료 대기 시간 안이라 청구할 금액은 없어요", waitChargeText(record(amount = 0)))
        assertEquals("이 주선사는 대기비 기준이 없어 시간만 기록했어요", waitChargeText(record(amount = null, policy = false)))
        assertEquals("작업 시작 기록이 없어 대기비를 계산하지 않았어요", waitChargeText(record(workStartedAt = null, wait = null, amount = null)))
    }

    @Test
    fun `서명 링크를 만들면 공유할 글이 생기고 한 번만 쓴다`() = runTest(main.dispatcher) {
        val vm = ProofViewModel(Fake(issued = proof()))
        vm.open(1)
        advanceUntilIdle()
        vm.shareDeliveryLink()
        advanceUntilIdle()
        assertTrue(vm.state.value.share!!.contains("http://x/r/a"))
        vm.shareConsumed()
        assertNull(vm.state.value.share)
        assertNotNull(vm.state.value.delivery)
    }

    @Test
    fun `인수증이 없으면 발급한 뒤 사진을 올린다`() = runTest(main.dispatcher) {
        val fake = Fake(issued = proof())
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertEquals(listOf("issue", "upload"), fake.calls)
        assertEquals(DeliveryProofStatus.PHOTO, vm.state.value.delivery?.status)
    }

    @Test
    fun `인수증이 있으면 바로 올리고 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake(delivery = proof(), issued = proof()).apply { uploadFails = true }
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertEquals(listOf("upload"), fake.calls)
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertNull(vm.state.value.busy)
    }

    @Test
    fun `하차지 도착 전에는 사진을 보내지 않는다`() = runTest(main.dispatcher) {
        val fake = Fake(delivery = proof(), issued = proof())
        val vm = ProofViewModel(fake)
        vm.open(1, paperAllowed = false)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun `서명 전에 확인하면 아직이라고 알려 준다`() = runTest(main.dispatcher) {
        val vm = ProofViewModel(Fake(delivery = proof(), issued = proof()))
        vm.open(1)
        advanceUntilIdle()
        vm.checkSigned()
        advanceUntilIdle()
        assertEquals("아직 서명 전이에요", vm.state.value.message)
    }

    @Test
    fun `서명됐거나 다섯 장이면 사진을 더 못 올린다`() {
        assertFalse(proof(DeliveryProofStatus.SIGNED, signedAt = at).canAddPhoto)
        assertFalse(proof(DeliveryProofStatus.PHOTO, photos = 5).canAddPhoto)
        assertTrue(proof(DeliveryProofStatus.PHOTO, photos = 4).canAddPhoto)
    }
}
