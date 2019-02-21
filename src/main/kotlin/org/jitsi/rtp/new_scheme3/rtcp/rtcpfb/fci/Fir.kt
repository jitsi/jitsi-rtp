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

package org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.fci

import org.jitsi.rtp.extensions.unsigned.getUByte
import org.jitsi.rtp.extensions.unsigned.getUInt
import org.jitsi.rtp.extensions.unsigned.putUByte
import org.jitsi.rtp.extensions.unsigned.putUInt
import java.nio.ByteBuffer

/**
 * https://tools.ietf.org/html/rfc5104#section-4.3.1.1
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                              SSRC                             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Seq nr.       |    Reserved                                   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *
 * The SSRC field in the FIR FCI block is used to set the media sender
 * SSRC, the media source SSRC field in the RTCPFB header is unsed for FIR packets.
 */
@ExperimentalUnsignedTypes
class Fir(
    ssrc: UInt = 0u,
    seqNum: UByte = 0u
) : FeedbackControlInformation() {
    override val sizeBytes: UInt = SIZE_BYTES

    var ssrc: UInt = ssrc
        private set

    var seqNum: UByte = seqNum
        private set

    //TODO: legacy constructor until other types are moved over to unsigned
    constructor(ssrc: Long = -1, seqNum: Int = -1) : this(ssrc.toUInt(), seqNum.toUByte())

    override fun serializeTo(buf: ByteBuffer) {
        buf.putUInt(ssrc)
        buf.putUByte(seqNum)
        // Add padding for the reserved chunk
        repeat (3) { buf.put(0x00) }
    }

    companion object {
        const val SIZE_BYTES: UInt = 8u

        fun fromBuffer(buf: ByteBuffer): Fir {
            val ssrc = buf.getUInt()
            val seqNum = buf.getUByte()
            // Parse passed the reserved chunk
            repeat (3) { buf.get() }

            return Fir(ssrc, seqNum)
        }
    }
}