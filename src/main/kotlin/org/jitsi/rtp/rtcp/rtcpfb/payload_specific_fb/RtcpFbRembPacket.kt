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

package org.jitsi.rtp.rtcp.rtcpfb.payload_specific_fb

import org.jitsi.rtp.extensions.bytearray.cloneFromPool
import org.jitsi.rtp.rtcp.RtcpHeaderBuilder
import org.jitsi.rtp.rtcp.rtcpfb.RtcpFbPacket
import org.jitsi.rtp.rtcp.rtcpfb.payload_specific_fb.RtcpFbRembPacket.Companion.REMB_OFFSET
import org.jitsi.rtp.util.BufferPool
import org.jitsi.rtp.util.RtpUtils
import org.jitsi.rtp.util.getBitsAsInt
import org.jitsi.rtp.util.getByteAsInt
import org.jitsi.rtp.util.getIntAsLong
import org.jitsi.rtp.util.getShortAsInt

/**
 * https://tools.ietf.org/html/draft-alvestrand-rmcat-remb-03
 *
 * 0                   1                   2                   3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P| FMT=15  |   PT=206      |             length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                  SSRC of packet sender                        |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                  SSRC of media source                         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Unique identifier 'R' 'E' 'M' 'B'                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Num SSRC     | BR Exp    |  BR Mantissa                      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   SSRC feedback                                               |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  ...                                                          |
 *
 * @author George Politis
 * @author Boris Grozev
 */
class RtcpFbRembPacket(
    buffer: ByteArray,
    offset: Int,
    length: Int
) : PayloadSpecificRtcpFbPacket(buffer, offset, length) {
    /**
     * The exponential scaling of the mantissa for the maximum total media
     * bit rate value, ignoring all packet overhead.
     */
    val exp: Int
        get() = getBrExp(buffer, offset)

    /**
     * The mantissa of the maximum total media bit rate (ignoring all packet
     * overhead) that the sender of the REMB estimates.  The BR is the estimate
     * of the traveled path for the SSRCs reported in this message.
     */
    val mantissa: Int
        get() = getBrMantissa(buffer, offset)

    val numSsrc: Int
        get() = getNumSsrc(buffer, offset)

    /**
     * one or more SSRC entries which this feedback message applies to.
     */
    val ssrcs: List<Long> by lazy {
        (0 until numSsrc).map {
            getSsrc(buffer, offset, it)
        }.toList()
    }

    override fun clone(): RtcpFbRembPacket {
        return RtcpFbRembPacket(buffer.cloneFromPool(), offset, length)
    }

    companion object {
        const val FMT = 15
        const val SIZE_BYTES = HEADER_SIZE + 4

        const val REMB_OFFSET = FCI_OFFSET
        const val NUM_SSRC_OFFSET = FCI_OFFSET + 4
        const val BR_OFFSET = NUM_SSRC_OFFSET + 1
        const val SSRCS_OFFSET = NUM_SSRC_OFFSET + 4

        fun getBrExp(buf: ByteArray, baseOffset: Int): Int =
            buf.getBitsAsInt(baseOffset + BR_OFFSET, 0, 6)
        fun getBrMantissa(buf: ByteArray, baseOffset: Int): Int =
            (buf.getBitsAsInt(baseOffset + BR_OFFSET, 6, 2) shl 2) + buf.getShortAsInt(baseOffset + BR_OFFSET + 1)
        fun getNumSsrc(buf: ByteArray, baseOffset: Int): Int =
            buf.getByteAsInt(baseOffset + NUM_SSRC_OFFSET)
        fun getSsrc(buf: ByteArray, baseOffset: Int, ssrcIndex: Int) =
            buf.getIntAsLong(baseOffset + SSRCS_OFFSET + ssrcIndex * 4)

        fun getExpAndMantissa(brBps: Long): Pair<Int, Int> {
            return Pair(0, 0)
        }
    }
}

class RtcpFbRembPacketBuilder(
    val rtcpHeader: RtcpHeaderBuilder = RtcpHeaderBuilder(),
    val ssrcs: List<Long> = emptyList(),
    val exp: Int,
    val mantissa: Int
) {

    constructor(
        /**
         * Bitrate in bits per seconds
         */
        brBps: Long
    ) : this(getExpAndMantissa = RtcpFbRembPacket.getExpAndMantissa(brBps))

    constructor(expAndMantissa: Pair<Int, Int>)
        : this(exp = expAndMantissa.first, mantissa = expAndMantissa.second)


    fun build(): RtcpFbRembPacket {
        val buf = BufferPool.getArray();
        writeTo(buf, 0)
        return RtcpFbRembPacket(buf, 0, len)
    }

    fun writeTo(buf: ByteArray, offset: Int): Int {
        rtcpHeader.apply {
            packetType = PayloadSpecificRtcpFbPacket.PT
            reportCount = RtcpFbRembPacket.FMT
            length = RtpUtils.calculateRtcpLengthFieldValue(RtcpFbPliPacket.SIZE_BYTES)
        }
        rtcpHeader.writeTo(buf, offset)
        RtcpFbPacket.setMediaSourceSsrc(buf, offset, 0) //
        buf[REMB_OFFSET + 0] = 'R'.toByte()
        buf[REMB_OFFSET + 1] = 'E'.toByte()
        buf[REMB_OFFSET + 2] = 'M'.toByte()
        buf[REMB_OFFSET + 3] = 'B'.toByte()

    }
}