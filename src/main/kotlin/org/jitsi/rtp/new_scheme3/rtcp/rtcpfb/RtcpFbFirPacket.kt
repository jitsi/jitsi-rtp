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

package org.jitsi.rtp.new_scheme3.rtcp.rtcpfb

import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.new_scheme3.Packet
import org.jitsi.rtp.new_scheme3.rtcp.RtcpHeader
import org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.fci.Fir
import java.nio.ByteBuffer

class RtcpFbFirPacket(
    header: RtcpHeader = RtcpHeader(),
    mediaSourceSsrc: Long = -1,
    private val fci: Fir = Fir(),
    backingBuffer: ByteBuffer? = null
) : PayloadSpecificFbPacket(header, mediaSourceSsrc, fci, backingBuffer) {

    override fun clone(): Packet {
        TODO()
    }

    companion object {
        const val FMT = 4

        fun fromValues(
            header: RtcpHeader = RtcpHeader(),
            mediaSourceSsrc: Long = -1,
            commandSeqNum: Int = -1
        ): RtcpFbFirPacket {
            val fci = Fir(mediaSourceSsrc, commandSeqNum)
            return RtcpFbFirPacket(header, mediaSourceSsrc, fci)
        }

        fun fromBuffer(buf: ByteBuffer): RtcpFbFirPacket {
            val header = RtcpHeader.create(buf)
            val mediaSourceSsrc = RtcpFbPacket.getMediaSourceSsrc(buf)
            val fci = Fir.fromBuffer(buf.subBuffer(RtcpFbPacket.FCI_OFFSET))

            return RtcpFbFirPacket(header, mediaSourceSsrc, fci, buf)
        }
    }
}