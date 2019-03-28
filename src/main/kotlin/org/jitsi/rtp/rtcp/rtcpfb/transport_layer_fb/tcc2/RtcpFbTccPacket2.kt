/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.tcc2

import org.jitsi.rtp.extensions.bytearray.put3Bytes
import org.jitsi.rtp.extensions.bytearray.putShort
import org.jitsi.rtp.rtcp.RtcpHeaderBuilder
import org.jitsi.rtp.rtcp.rtcpfb.RtcpFbPacket
import org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.TransportLayerRtcpFbPacket
import org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.tcc.RtcpFbTccPacket
import org.jitsi.rtp.util.BufferPool
import org.jitsi.rtp.util.RtpUtils

typealias DeltaSize = Int

// The base sequence number is passed because we know, based on what has previously
// been received, what the next expected seq num should be.  We don't pass in the reference
// timestamp though, since that will be based on the first packet which is received
/**
 * This class is a port of TransportFeedback in
 * transport_feedback.h/transport_feedback.cc in Chrome
 * https://cs.chromium.org/chromium/src/third_party/webrtc/modules/rtp_rtcp/source/rtcp_packet/transport_feedback.h?l=95&rcl=20393ee9b7ba622f254908646a9c31bf87349fc7
 *
 * Because of this, it explicitly does NOT try to conform
 * to Kotlin style or idioms, instead striving to match the
 * Chrome code as closely as possible in an effort to make
 * future updates easier.
 */
class RtcpFbTccPacket2Builder(
    val rtcpHeader: RtcpHeaderBuilder = RtcpHeaderBuilder(),
    var mediaSourceSsrc: Long = -1,
    val feedbackPacketSeqNum: Int = -1,
    val base_seq_no_: Int = -1
) {
    // The reference time, in ticks.  Chrome passes this into BuildFeedbackPacket, but we don't
    // hold the times in the same way, so we'll just assign it the first time we see
    // a packet in AddReceivedPacket
    private var base_time_ticks_: Long = -1
    // The amount of packets_ whose status are represented
    var num_seq_no_ = 0
        private set
    // The current chunk we're 'filling out' as packets
    // are received
    private var last_chunk_ = LastChunk()
    // All but last encoded packet chunks.
    private val encoded_chunks_ = mutableListOf<Chunk>()
    // The size of the entire packet, in bytes
    private var size_bytes_ = kTransportFeedbackHeaderSizeBytes
    private var last_timestamp_us_: Long = 0
    private val packets_ = mutableListOf<ReceivedPacket>()

    class ReceivedPacket(val seqNum: Int, val deltaTicks: Short)

    fun AddReceivedPacket(sequence_number: Int, timestamp_us: Long): Boolean {
        if (base_time_ticks_ == -1L) {
            // Take timestamp_us and convert it to a number that fits and wraps properly to be represented
            // as the 24 bit reference time field
            base_time_ticks_ = (timestamp_us % kTimeWrapPeriodUs) / kBaseScaleFactor
            last_timestamp_us_ = base_time_ticks_ * kBaseScaleFactor
        }
        var delta_full = (timestamp_us - last_timestamp_us_) % kTimeWrapPeriodUs
        if (delta_full > kTimeWrapPeriodUs / 2) {
            delta_full -= kTimeWrapPeriodUs
        }
        delta_full += if (delta_full < 0) -(kDeltaScaleFactor / 2) else kDeltaScaleFactor / 2
        delta_full /= kDeltaScaleFactor

        val delta = delta_full.toShort()
        // If larger than 16bit signed, we can't represent it - need new fb packet.
        if (delta.toLong() != delta_full) {
            println("Delta value too large! ( >= 2^16 ticks )")
            return false
        }
        var next_seq_no = base_seq_no_ + num_seq_no_
        if (sequence_number != next_seq_no) {
            val lastSeqNo = next_seq_no - 1
            //TODO: proper seq num comparison
            if (sequence_number <= lastSeqNo) {
                return false
            }
            while (next_seq_no != sequence_number) {
                if (!AddDeltaSize(0))
                    return false
                next_seq_no++
            }
        }
        val delta_size = if (delta >= 0 && delta <= 0xff) 1 else 2
        if (!AddDeltaSize(delta_size))
            return false

        //TODO: too costly to create a new ReceivedPacket instance each time?
        packets_.add(ReceivedPacket(sequence_number, delta))
        last_timestamp_us_ += delta * kDeltaScaleFactor
        size_bytes_ += delta_size

        return true
    }

    private fun AddDeltaSize(deltaSize: DeltaSize): Boolean {
        if (num_seq_no_ == kMaxReportedPackets)
            return false
        val add_chunk_size = if (last_chunk_.Empty()) kChunkSizeBytes else 0

        if (size_bytes_ + deltaSize + add_chunk_size > kMaxSizeBytes)
            return false

        if (last_chunk_.CanAdd(deltaSize)) {
            size_bytes_ += add_chunk_size
            last_chunk_.Add(deltaSize)
            ++num_seq_no_
            return true
        }

        if (size_bytes_ + deltaSize + kChunkSizeBytes > kMaxSizeBytes)
            return false

        encoded_chunks_.add(last_chunk_.Emit())
        size_bytes_ += kChunkSizeBytes
        last_chunk_.Add(deltaSize)
        ++num_seq_no_
        return true
    }

    fun build(): RtcpFbTccPacket {
        val packetSize = size_bytes_ + RtpUtils.getNumPaddingBytes(size_bytes_)
        val buf = BufferPool.getArray(packetSize)
        writeTo(buf, 0)
        return RtcpFbTccPacket(buf, 0, packetSize)
    }

    fun writeTo(buf: ByteArray, offset: Int) {
        //NOTE: padding is held 'internally' in the TCC FCI, so we don't set
        // the padding bit on the header
        val paddingBytes = RtpUtils.getNumPaddingBytes(size_bytes_)
        rtcpHeader.apply {
            packetType = TransportLayerRtcpFbPacket.PT
            reportCount = RtcpFbTccPacket.FMT
            length = RtpUtils.calculateRtcpLengthFieldValue(size_bytes_ + paddingBytes)
        }.writeTo(buf, offset)

        RtcpFbPacket.setMediaSourceSsrc(buf, offset, mediaSourceSsrc)
        RtcpFbTccPacket.setBaseSeqNum(buf, offset, base_seq_no_)
        RtcpFbTccPacket.setPacketStatusCount(buf, offset, num_seq_no_)
        buf.put3Bytes(offset + RtcpFbTccPacket.REFERENCE_TIME_OFFSET, base_time_ticks_.toInt())
        RtcpFbTccPacket.setFeedbackPacketCount(buf, offset, feedbackPacketSeqNum)

        var currOffset = RtcpFbTccPacket.PACKET_CHUNKS_OFFSET
        encoded_chunks_.forEach {
            buf.putShort(currOffset, it.toShort())
            currOffset += kChunkSizeBytes
        }
        if (!last_chunk_.Empty()) {
            val chunk = last_chunk_.EncodeLast()
            buf.putShort(currOffset, chunk.toShort())
            currOffset += kChunkSizeBytes
        }
        packets_.forEach {
            when (it.deltaTicks) {
                in 0..0xFF -> buf[currOffset++] = it.deltaTicks.toByte()
                else -> {
                    buf.putShort(currOffset, it.deltaTicks)
                    currOffset += 2
                }
            }
        }
        repeat(paddingBytes) {
            buf[currOffset++] = 0x00
        }
    }

    fun clear() {
        num_seq_no_ = 0
        size_bytes_ = kTransportFeedbackHeaderSizeBytes
    }

    companion object {
        // Convert to multiples of 0.25ms
        const val kDeltaScaleFactor = 250
        // Maximum number of packets_ (including missing) TransportFeedback can report.
        const val kMaxReportedPackets = 0xFFFF
        const val kChunkSizeBytes = 2
        // Size constraint imposed by RTCP common header: 16bit size field interpreted
        // as number of four byte words minus the first header word.
        const val kMaxSizeBytes = (1 shl 16) * 4
        // Header size:
        // * 4 bytes Common RTCP Packet Header
        // * 8 bytes Common Packet Format for RTCP Feedback Messages
        // * 8 bytes FeedbackPacket header
        const val kTransportFeedbackHeaderSizeBytes = 4 + 8 + 8
        // Used to convert from microseconds to multiples of 64ms(?)
        const val kBaseScaleFactor = kDeltaScaleFactor * (1 shl 8)
        // The reference time field is 24 bits and are represented as multiples of 64ms
        // When the reference time field would need to wrap around
        const val kTimeWrapPeriodUs: Long = (1 shl 24).toLong() * kBaseScaleFactor
    }
}