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

package org.jitsi.rtp.new_scheme.rtcp

import org.jitsi.rtp.new_scheme.CanBecomeModifiable
import org.jitsi.rtp.new_scheme.CanBecomeReadOnly
import org.jitsi.rtp.new_scheme.ConstructableFromBuffer
import org.jitsi.rtp.new_scheme.ModifiablePacket
import org.jitsi.rtp.new_scheme.ReadOnlyPacket
import org.jitsi.rtp.new_scheme.ReadOnlyRtpProtocolPacket
import org.jitsi.rtp.rtcp.RtcpByePacket
import org.jitsi.rtp.rtcp.RtcpHeader
import org.jitsi.rtp.util.ByteBufferUtils
import java.nio.ByteBuffer


abstract class ReadOnlyRtcpPacket : ReadOnlyRtpProtocolPacket() {
    abstract val header: ReadOnlyRtcpHeader
    abstract val payload: ByteBuffer
    companion object : ConstructableFromBuffer<ReadOnlyRtcpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ReadOnlyRtcpPacket {
            val packetType = RtcpHeader.getPacketType(buf)
            return when (packetType) {
//                RtcpSrPacket.PT -> RtcpSrPacket(buf)
//                RtcpRrPacket.PT -> RtcpRrPacket(buf)
//                RtcpSdesPacket.PT -> RtcpSdesPacket(buf)
                RtcpByePacket.PT -> ReadOnlyRtcpByePacket.fromBuffer(buf)
//                in RtcpFbPacket.PACKET_TYPES -> RtcpFbPacket.fromBuffer(buf)
                else -> throw Exception("Unsupported RTCP packet type $packetType")
            }
        }
    }
}

abstract class ModifiableRtcpPacket : ModifiablePacket {
    abstract var header: ModifiableRtcpHeader
//    abstract var payload: ByteBuffer
    companion object : ConstructableFromBuffer<ModifiableRtcpPacket> {
        override fun fromBuffer(buf: ByteBuffer): ModifiableRtcpPacket {
            val packetType = RtcpHeader.getPacketType(buf)
            return when (packetType) {
//                RtcpSrPacket.PT -> RtcpSrPacket(buf)
//                RtcpRrPacket.PT -> RtcpRrPacket(buf)
//                RtcpSdesPacket.PT -> RtcpSdesPacket(buf)
                RtcpByePacket.PT -> ModifiableRtcpByePacket.fromBuffer(buf)
//                in RtcpFbPacket.PACKET_TYPES -> RtcpFbPacket.fromBuffer(buf)
                else -> throw Exception("Unsupported RTCP packet type $packetType")
            }
        }

    }

}