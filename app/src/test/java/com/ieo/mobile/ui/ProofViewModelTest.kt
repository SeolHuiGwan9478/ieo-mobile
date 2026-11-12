package com.ieo.mobile.ui

import com.ieo.mobile.core.network.ApiError
import com.ieo.mobile.core.network.ApiResult
import com.ieo.mobile.domain.proof.DeliveryProof
import com.ieo.mobile.domain.proof.DeliveryProofStatus
import com.ieo.mobile.domain.proof.ProofLink
import com.ieo.mobile.domain.proof.ProofRepository
import com.ieo.mobile.domain.proof.ReceiptPhoto
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

    private fun proof(status: DeliveryProofStatus = DeliveryProofStatus.NONE, photos: List<Long> = emptyList(), canUpload: Boolean = true) =
        DeliveryProof(true, status, null, photos.map { ReceiptPhoto(it) }, 5, canUpload)

    private class Fake(var delivery: DeliveryProof) : ProofRepository {
        val calls = mutableListOf<String>()
        var fails = false
        override suspend fun waits(tripId: Long) = ApiResult.Success(emptyList<WaitRecord>())
        override suspend fun waitProofLink(tripId: Long) = ApiResult.Success(ProofLink("http://x/w/a", Instant.EPOCH))
        override suspend fun editWait(tripId: Long, stop: StopType, times: WaitTimes) =
            ApiResult.Success(WaitRecord(stop, "평택", times.arrivedAt, times.workStartedAt, times.departedAt, 0, 0, true, true))
        override suspend fun deliveryProof(tripId: Long) = ApiResult.Success(delivery)
        private fun change(name: String, next: () -> DeliveryProof): ApiResult<DeliveryProof> {
            calls += name
            if (fails) return ApiResult.Failure(ApiError.Network)
            delivery = next()
            return ApiResult.Success(delivery)
        }
        override suspend fun uploadReceiptPhoto(tripId: Long, jpeg: ByteArray) =
            change("upload") { delivery.copy(status = DeliveryProofStatus.PENDING, photos = delivery.photos + ReceiptPhoto(delivery.photos.size + 10L)) }
        override suspend fun deleteReceiptPhoto(tripId: Long, photoId: Long) =
            change("delete") { delivery.copy(photos = delivery.photos.filter { it.id != photoId }) }
        override suspend fun sendReceipt(tripId: Long) =
            change("send") { delivery.copy(status = DeliveryProofStatus.SENT, sentAt = Instant.EPOCH, canUpload = false) }
        override suspend fun receiptPhoto(tripId: Long, photoId: Long): ApiResult<ByteArray> {
            calls += "photo$photoId"
            return ApiResult.Success(byteArrayOf(photoId.toByte()))
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
    fun `열면 올린 사진을 받아 둔다`() = runTest(main.dispatcher) {
        val fake = Fake(proof(DeliveryProofStatus.PENDING, listOf(1, 2)))
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        assertEquals(listOf("photo1", "photo2"), fake.calls)
        assertEquals(setOf(1L, 2L), vm.state.value.photoFiles.keys)
    }

    @Test
    fun `찍은 사진을 올리면 바로 목록에 보이고 지울 수 있다`() = runTest(main.dispatcher) {
        val fake = Fake(proof())
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(7))
        advanceUntilIdle()
        val id = vm.state.value.delivery!!.photos.single().id
        assertEquals(7.toByte(), vm.state.value.photoFiles.getValue(id)[0])
        assertTrue(vm.state.value.delivery!!.canSend)

        vm.deletePhoto(id)
        advanceUntilIdle()
        assertTrue(vm.state.value.delivery!!.photos.isEmpty())
        assertFalse(vm.state.value.photoFiles.containsKey(id))
        assertFalse(vm.state.value.delivery!!.canSend)
    }

    @Test
    fun `보내면 보냈다고 알려 주고 더는 못 올린다`() = runTest(main.dispatcher) {
        val fake = Fake(proof(DeliveryProofStatus.PENDING, listOf(1)))
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.sendReceipt()
        advanceUntilIdle()
        assertTrue(vm.state.value.delivery!!.isSent)
        assertEquals("인수증을 주선사에게 보냈어요", vm.state.value.message)
        assertFalse(vm.state.value.delivery!!.canSend)
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertFalse("upload" in fake.calls)
    }

    @Test
    fun `하차지 도착 전에는 사진을 올리지 않는다`() = runTest(main.dispatcher) {
        val fake = Fake(proof(canUpload = false))
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun `올리기에 실패하면 이유를 보여 준다`() = runTest(main.dispatcher) {
        val fake = Fake(proof()).apply { fails = true }
        val vm = ProofViewModel(fake)
        vm.open(1)
        advanceUntilIdle()
        vm.uploadPaper(byteArrayOf(1))
        advanceUntilIdle()
        assertEquals(ApiError.Network.message, vm.state.value.error)
        assertNull(vm.state.value.busy)
    }
}
