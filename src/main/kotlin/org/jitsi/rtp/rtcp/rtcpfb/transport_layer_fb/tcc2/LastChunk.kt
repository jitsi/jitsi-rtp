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

// NOTE(brian): We have to store Chunk as an Int to avoid sign issues
typealias Chunk = Int

/**
 * This class is a port of TransportFeedback::LastChunk in
 * transport_feedback.h/transport_feedback.cc in Chrome
 * https://cs.chromium.org/chromium/src/third_party/webrtc/modules/rtp_rtcp/source/rtcp_packet/transport_feedback.h?l=95&rcl=20393ee9b7ba622f254908646a9c31bf87349fc7
 *
 * Because of this, it explicitly does NOT try to conform
 * to Kotlin style or idioms, instead striving to match the
 * Chrome code as closely as possible in an effort to make
 * future updates easier.
 */
class LastChunk {
    fun Empty(): Boolean {
        return size_ == 0
    }

    fun Clear() {
        size_ = 0
        all_same_ = true
        has_large_delta_ = false
    }


    fun CanAdd(deltaSize: DeltaSize): Boolean {
        if (size_ < kMaxTwoBitCapacity)
            return true
        if (size_ < kMaxOneBitCapacity && !has_large_delta_ && deltaSize != kLarge)
            return true
        if (size_ < kMaxRunLengthCapacity && all_same_ && delta_sizes_[0] == deltaSize)
            return true
        return false
    }

    // Should only be called if CanAdd returned true
    fun Add(deltaSize: DeltaSize) {
        if (size_ < kMaxVectorCapacity)
            delta_sizes_[size_] = deltaSize
        size_++
        all_same_ = all_same_ && deltaSize == delta_sizes_[0]
        has_large_delta_ = has_large_delta_ || deltaSize == kLarge
    }

    fun Emit(): Chunk {
        if (all_same_) {
            val chunk = EncodeRunLength()
            Clear()
            return chunk
        }
        if (size_ == kMaxOneBitCapacity) {
            val chunk = EncodeOneBit()
            Clear()
            return chunk
        }
        val chunk = EncodeTwoBit(kMaxTwoBitCapacity)
        // Remove |kMaxTwoBitCapacity| encoded delta sizes:
        // Shift remaining delta sizes and recalculate all_same_ && has_large_delta_.
        size_ -= kMaxTwoBitCapacity
        all_same_ = true
        has_large_delta_ = false
        for (i in 0 until size_) {
            val deltaSize = delta_sizes_[kMaxTwoBitCapacity + i]
            delta_sizes_[i] = deltaSize
            all_same_ = all_same_ && deltaSize == delta_sizes_[0]
            has_large_delta_ = has_large_delta_ || deltaSize == kLarge
        }

        return chunk
    }

    fun EncodeLast(): Chunk {
        if (all_same_)
            return EncodeRunLength()
        if (size_ < kMaxTwoBitCapacity)
            EncodeTwoBit(size_)
        return EncodeOneBit()
    }

    //private:

    /**
     *
     * Run Length Status Vector Chunk
     *
     * 0                   1
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |T| S |       Run Length        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     * T = 0
     * S = symbol
     * Run Length = Unsigned integer denoting the run length of the symbol
     */
    private fun EncodeRunLength(): Chunk =
        ((delta_sizes_[0] shl 13) or size_)

    /**
     *  One Bit Status Vector Chunk
     *
     * 0                   1
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |T|S|       symbol list         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     * T = 1
     * S = 0
     * Symbol list = 14 entries where 0 = not received, 1 = received 1-byte delta.
     */
    private fun EncodeOneBit(): Chunk {
        var chunk = 0x8000
        for (i in 0 until size_) {
            chunk = (chunk or (delta_sizes_[i] shl kMaxOneBitCapacity - 1 - i))
        }
        return chunk
    }

    /**
     * Two Bit Status Vector Chunk
     *
     * 0                   1
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |T|S|       symbol list         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     * T = 1
     * S = 1
     * symbol list = 7 entries of two bits each.
     */
    private fun EncodeTwoBit(size: Int): Chunk {
        var chunk = 0xC000
        for (i in 0 until size) {
            chunk = (chunk or (delta_sizes_[i] shl (2 * (kMaxTwoBitCapacity - 1 - i))))
        }
        return chunk
    }

    private var size_: Int = 0
    private var all_same_: Boolean = true
    private var has_large_delta_: Boolean = false
    private val delta_sizes_ = Array<DeltaSize>(kMaxVectorCapacity) {0}

    companion object {
        private const val kMaxRunLengthCapacity = 0x1FFF
        private const val kMaxOneBitCapacity = 14
        private const val kMaxTwoBitCapacity = 7
        private const val kMaxVectorCapacity = kMaxOneBitCapacity
        private const val kLarge: DeltaSize = 2
    }
}