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
import org.jitsi.rtp.extensions.bytearray.toHex
import org.jitsi.rtp.rtcp.RtcpHeaderBuilder
import org.jitsi.rtp.rtcp.rtcpfb.RtcpFbPacket
import org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.TransportLayerRtcpFbPacket
import org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.tcc.RtcpFbTccPacket
import org.jitsi.rtp.util.BufferPool
import org.jitsi.rtp.util.RtpUtils

typealias DeltaSize = Int

// We'll always encode using 2 bit status symbol, status vector chunks
class StatusVectorChunkBuilder {

//    fun add




    fun writeTo(buf: ByteArray, offset: Int) {

    }
}

// The base sequence number is passed because we know, based on what has previously
// been received, what the next expected seq num should be.  We don't pass in the reference
// timestamp though, since that will be based on the first packet which is received
class RtcpFbTccPacket2Builder(
    val rtcpHeader: RtcpHeaderBuilder = RtcpHeaderBuilder(),
    var mediaSourceSsrc: Long = -1,
    val feedbackPacketSeqNum: Int = -1,
    val baseSeqNo: Int = -1
) {
    // The reference time, in ticks.  Chrome passes this into BuildFeedbackPacket, but we don't
    // hold the times in the same way, so we'll just assign it the first time we see
    // a packet in addReceivedPacket
    private var baseTimeTicks: Long = -1
    // The amount of packets whose status are represented
    var numSeqNo = 0
        private set
    // The current chunk we're 'filling out' as packets
    // are received
    private var lastChunk = LastChunk()
    // The list of status chunks we've already encoded from previously
    // received packets
    private val encodedChunks = mutableListOf<Chunk>()
    // The size of the entire packet, in bytes
    private var sizeBytes = TRANSPORT_CC_HEADER_SIZE_BYTES
    private var lastTimestampUs: Long = 0
    private val packets = mutableListOf<ReceivedPacket>()

    class ReceivedPacket(val seqNum: Int, val deltaTicks: Short)

    fun addReceivedPacket(sequenceNumber: Int, timestampUs: Long): Boolean {
//        println("tcc packet $feedbackPacketSeqNum adding seqNum $sequenceNumber ts $timestampUs")
        if (baseTimeTicks == -1L) {
            baseTimeTicks = (timestampUs % TIME_WRAP_PERIOD_US) / BASE_SCALE_FACTOR
            lastTimestampUs = baseTimeTicks * BASE_SCALE_FACTOR
        }
        var deltaFull = (timestampUs - lastTimestampUs) % TIME_WRAP_PERIOD_US
        if (deltaFull > TIME_WRAP_PERIOD_US / 2) {
            deltaFull -= TIME_WRAP_PERIOD_US
        }
        deltaFull += if (deltaFull < 0) -(DELTA_SCALE_FACTOR / 2) else DELTA_SCALE_FACTOR / 2
        deltaFull /= DELTA_SCALE_FACTOR

        val delta = deltaFull.toShort()
        if (delta.toLong() != deltaFull) {
            println("Delta value too large! ( >= 2^16 ticks )")
            return false
        }
        var nextSeqNo = baseSeqNo + numSeqNo
        if (sequenceNumber != nextSeqNo) {
            val lastSeqNo = nextSeqNo - 1
            //TODO: proper seq num comparison
            if (sequenceNumber <= lastSeqNo) {
                return false
            }
            while (nextSeqNo != sequenceNumber) {
                if (!addDeltaSize(0)) {
                    return false
                }
                nextSeqNo++
            }
        }
        val deltaSize = if (delta in 0..0xFF) 1 else 2
        if (!addDeltaSize(deltaSize)) {
            return false
        }
        //TODO: too costly to create a new ReceivedPacket instance each time?
        packets.add(ReceivedPacket(sequenceNumber, delta))
        lastTimestampUs += delta * DELTA_SCALE_FACTOR
        sizeBytes += deltaSize

        return true
    }

    private fun addDeltaSize(deltaSize: DeltaSize): Boolean {
        if (numSeqNo == MAX_REPORTED_PACKETS) {
            return false
        }
        val addChunkSize = if (lastChunk.empty) CHUNK_SIZE_BYTES else 0

        if (sizeBytes + deltaSize + addChunkSize > MAX_SIZE_BYTES) {
            return false
        }

        if (lastChunk.canAdd(deltaSize)) {
            sizeBytes += addChunkSize
            lastChunk.add(deltaSize)
            numSeqNo++
            return true
        }

        if (sizeBytes + deltaSize + CHUNK_SIZE_BYTES > MAX_SIZE_BYTES) {
            return false
        }
        encodedChunks.add(lastChunk.emit())
        sizeBytes += CHUNK_SIZE_BYTES
        lastChunk.add(deltaSize)
        numSeqNo++
        return true
    }

    fun build(): RtcpFbTccPacket {
        val packetSize = sizeBytes + RtpUtils.getNumPaddingBytes(sizeBytes)
        val buf = BufferPool.getArray(packetSize)
        writeTo(buf, 0)
//        println("created tcc packet:\n${buf.toHex()}")
        return RtcpFbTccPacket(buf, 0, packetSize)
    }

    fun writeTo(buf: ByteArray, offset: Int) {
        //NOTE: padding is held 'internally' in the TCC FCI, so we don't set
        // the padding bit on the header
        val paddingBytes = RtpUtils.getNumPaddingBytes(sizeBytes)
        rtcpHeader.apply {
            packetType = TransportLayerRtcpFbPacket.PT
            reportCount = RtcpFbTccPacket.FMT
            length = RtpUtils.calculateRtcpLengthFieldValue(sizeBytes + paddingBytes)
        }.writeTo(buf, offset)

        RtcpFbPacket.setMediaSourceSsrc(buf, offset, mediaSourceSsrc)
        RtcpFbTccPacket.setBaseSeqNum(buf, offset, baseSeqNo)
        RtcpFbTccPacket.setPacketStatusCount(buf, offset, numSeqNo)
//        RtcpFbTccPacket.setReferenceTimeMs(buf, offset, baseTimeTicks)
        buf.put3Bytes(offset + RtcpFbTccPacket.REFERENCE_TIME_OFFSET, baseTimeTicks.toInt())
//        RtcpFbTccPacket.setReferenceTimeMs(buf, offset, baseTimeTicks)
        RtcpFbTccPacket.setFeedbackPacketCount(buf, offset, feedbackPacketSeqNum)

        var currOffset = RtcpFbTccPacket.PACKET_CHUNKS_OFFSET
        encodedChunks.forEach {
            buf.putShort(currOffset, it.toShort())
            currOffset += CHUNK_SIZE_BYTES
        }
        if (!lastChunk.empty) {
            val chunk = lastChunk.encodeLast()
            buf.putShort(currOffset, chunk.toShort())
            currOffset += CHUNK_SIZE_BYTES
        }
        packets.forEach {
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
        numSeqNo = 0
        sizeBytes = TRANSPORT_CC_HEADER_SIZE_BYTES
    }

    companion object {
        // Maximum number of packets (including missing) TransportFeedback can report.
        const val MAX_REPORTED_PACKETS = 0xFFFF
        // // Convert to multiples of 0.25ms
        const val DELTA_SCALE_FACTOR = 250
        const val CHUNK_SIZE_BYTES = 2
        // Size constraint imposed by RTCP common header: 16bit size field interpreted
        // as number of four byte words minus the first header word.
        const val MAX_SIZE_BYTES = (1 shl 16) * 4
        // Header size:
        // * 4 bytes Common RTCP Packet Header
        // * 8 bytes Common Packet Format for RTCP Feedback Messages
        // * 8 bytes FeedbackPacket header
        const val TRANSPORT_CC_HEADER_SIZE_BYTES = 4 + 8 + 8

        const val BASE_SCALE_FACTOR = DELTA_SCALE_FACTOR * (1 shl 8)
        //TODO: make sure we're not overflowing anything here
        const val TIME_WRAP_PERIOD_US: Long = (1 shl 24).toLong() * BASE_SCALE_FACTOR
    }
}