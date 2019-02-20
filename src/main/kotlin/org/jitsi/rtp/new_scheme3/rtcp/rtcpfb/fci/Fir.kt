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
class Fir(
    ssrc: Long = -1,
    seqNum: Int = -1
) : FeedbackControlInformation() {
    override val sizeBytes: Int = SIZE_BYTES

    var ssrc: Long = ssrc
        private set

    var seqNum: Int = seqNum
        private set


    override fun serializeTo(buf: ByteBuffer) {
        buf.putInt(ssrc.toInt())
        buf.put(seqNum.toByte())
    }

    companion object {
        const val SIZE_BYTES = 8

        fun fromBuffer(buf: ByteBuffer): Fir {
            val ssrc = getSsrc(buf)
            val seqNum = getSeqNum(buf)

            return Fir(ssrc, seqNum)
        }

        fun getSsrc(buf: ByteBuffer): Long = buf.getInt(0).toLong()
        fun setSsrc(buf: ByteBuffer, ssrc: Long) = buf.putInt(0, ssrc.toInt())

        fun getSeqNum(buf: ByteBuffer) = buf.get(4).toInt()
        fun setSeqNum(buf: ByteBuffer, seqNum: Int) {
            buf.put(4, seqNum.toByte())
        }
    }
}