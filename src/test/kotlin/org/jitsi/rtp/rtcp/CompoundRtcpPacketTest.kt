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

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.rtcp.rtcpfb.payload_specific_fb.RtcpFbPliPacketBuilder
import org.jitsi.rtp.rtcp.rtcpfb.payload_specific_fb.RtcpFbRembPacketBuilder

internal class CompoundRtcpPacketTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "Creating a compound packet from a list of packets" {
            val rr = RtcpRrPacketBuilder(RtcpHeaderBuilder(), mutableListOf()).build()
            val remb = RtcpFbRembPacketBuilder(RtcpHeaderBuilder(), listOf(123), 12345).build()
            val pli = RtcpFbPliPacketBuilder(RtcpHeaderBuilder(), 456).build()

            val compoundRtcpPacket = CompoundRtcpPacket(listOf(rr, remb, pli))
            compoundRtcpPacket.packets.size shouldBe 3
            compoundRtcpPacket.packets[0]::class shouldBe rr::class
            compoundRtcpPacket.packets[0].length shouldBe rr.length

            compoundRtcpPacket.packets[1]::class shouldBe remb::class
            compoundRtcpPacket.packets[1].length shouldBe remb.length

            compoundRtcpPacket.packets[2]::class shouldBe pli::class
            compoundRtcpPacket.packets[2].length shouldBe pli.length
        }
    }
}
