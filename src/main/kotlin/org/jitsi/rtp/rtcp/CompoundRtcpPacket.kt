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

package org.jitsi.rtp.rtcp

class CompoundRtcpPacket(
    buffer: ByteArray,
    offset: Int,
    length: Int
) : RtcpPacket(buffer, offset, length) {

    val packets: List<RtcpPacket> by lazy {
        var bytesRemaining = length
        var currOffset = offset
        val rtcpPackets = mutableListOf<RtcpPacket>()
        while (bytesRemaining > RtcpHeader.SIZE_BYTES) {
            val rtcpPacket = RtcpPacket.parse(buffer, currOffset)
            rtcpPackets.add(rtcpPacket)
            currOffset += rtcpPacket.length
            bytesRemaining -= rtcpPacket.length
        }
        rtcpPackets
    }

    override fun clone(): RtcpPacket = CompoundRtcpPacket(cloneBuffer(), offset, length)
}
