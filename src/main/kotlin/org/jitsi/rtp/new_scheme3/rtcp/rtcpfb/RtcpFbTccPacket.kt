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

import org.jitsi.rtp.extensions.unsigned.subBuffer
import org.jitsi.rtp.new_scheme3.rtcp.RtcpHeader
import org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.fci.tcc.Tcc
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
class RtcpFbTccPacket(
    header: RtcpHeader = RtcpHeader(),
    mediaSourceSsrc: Long = -1,
    //TODO(brian): expose tcc fci as read-only except for in modifyFci
    val fci: Tcc = Tcc(),
    backingBuffer: ByteBuffer? = null
) : TransportLayerFbPacket(header, mediaSourceSsrc, fci, backingBuffer) {

    val numPackets: Int get() = fci.numPackets

    fun modifyFci(block: Tcc.() -> Unit) {
        //TODO: dirty
        with (fci) {
            block()
        }
    }

    override fun clone(): RtcpFbTccPacket {
        TODO()
    }

    companion object {
        const val FMT = 15

        fun fromBuffer(buf: ByteBuffer): RtcpFbTccPacket {
            val header = RtcpHeader.create(buf)
            val mediaSourceSsrc = RtcpFbPacket.getMediaSourceSsrc(buf)
            val fci = Tcc.fromBuffer(buf.subBuffer(RtcpFbPacket.FCI_OFFSET))

            return RtcpFbTccPacket(header, mediaSourceSsrc, fci, buf)
        }
    }
}