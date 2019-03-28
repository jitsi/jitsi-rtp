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

import kotlin.experimental.or

// We have to store Chunk as an Int, to avoid sign issues
typealias Chunk = Int

class LastChunk {
    private var numStatuses: Int = 0
    private var allSame: Boolean = true
    private var hasLargeDelta: Boolean = false
    private val deltaSizes = Array<DeltaSize>(MAX_VECTOR_CAPACITY) {0}

    val empty: Boolean
        get() = numStatuses == 0


    fun canAdd(deltaSize: DeltaSize): Boolean {
        return when {
            numStatuses < MAX_TWO_BIT_CAPACITY -> true
            numStatuses < MAX_ONE_BIT_CAPACITY && !hasLargeDelta && deltaSize != LARGE_DELTA_SIZE -> true
            numStatuses < MAX_RUN_LENGTH_CAPACITY && allSame && deltaSizes[0] == deltaSize -> true
            else -> false
        }
    }

    // Should only be called if canAdd returned true
    fun add(deltaSize: DeltaSize) {
        if (numStatuses < MAX_VECTOR_CAPACITY) {
            deltaSizes[numStatuses++] = deltaSize
        }
        allSame = allSame && deltaSize == deltaSizes[0]
        hasLargeDelta = hasLargeDelta || deltaSize == LARGE_DELTA_SIZE
    }

    //TODO: what is the trigger for calling this? --> it assumes the size will be on a 'nice'
    // boundary?
    fun emit(): Chunk {
        if (allSame) {
            return encodeRunLength().also {
                clear()
            }
        }
        if (numStatuses == MAX_ONE_BIT_CAPACITY) {
            return encodeOneBit().also {
                clear()
            }
        }
        val chunk = encodeTwoBit(MAX_TWO_BIT_CAPACITY)
        // Remove MAX_TWO_BIT_CAPACITY encoded delta sizes,
        // shift remaining delta sizes and recalculate allSame and
        // hasLargeDelta
        numStatuses -= MAX_TWO_BIT_CAPACITY
        allSame = true
        hasLargeDelta = false
        (0 until numStatuses).forEach { index ->
            val deltaSize = deltaSizes[MAX_TWO_BIT_CAPACITY + index]
            deltaSizes[index] = deltaSize
            allSame = allSame && deltaSize == deltaSizes[0]
            hasLargeDelta = hasLargeDelta || deltaSize == LARGE_DELTA_SIZE
        }

        return chunk
    }

    fun encodeLast(): Chunk {
        return when {
            allSame -> encodeRunLength()
            numStatuses < MAX_TWO_BIT_CAPACITY -> encodeTwoBit(numStatuses)
            else -> encodeOneBit()
        }
    }

    fun clear() {
        numStatuses = 0
        allSame = true
        hasLargeDelta = true
    }

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
    private fun encodeRunLength(): Chunk =
        ((deltaSizes[0] shl 13) or numStatuses)

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
    private fun encodeOneBit(): Chunk {
        var chunk = 0x8000
        deltaSizes.forEachIndexed { index, deltaSize ->
            chunk = (chunk or (deltaSize shl MAX_ONE_BIT_CAPACITY - 1 - index))
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
    private fun encodeTwoBit(numStatuses: Int): Chunk {
        var chunk = 0xC000
        for (i in 0 until numStatuses) {
            chunk = (chunk or (deltaSizes[i] shl (2 * (MAX_TWO_BIT_CAPACITY - 1 - i))))
        }
        return chunk
    }


    companion object {
        const val MAX_RUN_LENGTH_CAPACITY = 0x1FFF
        const val MAX_ONE_BIT_CAPACITY = 14
        const val MAX_TWO_BIT_CAPACITY = 7
        const val MAX_VECTOR_CAPACITY = MAX_ONE_BIT_CAPACITY
        const val LARGE_DELTA_SIZE: DeltaSize = 2
    }
}