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

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.rtcp.RtcpHeaderBuilder

internal class RtcpFbRembPacketTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "Creating an RtcpFbRembPacket" {
            "from values" {
                val rembPacket = RtcpFbRembPacketBuilder(rtcpHeader = RtcpHeaderBuilder(
                        senderSsrc = 4567L
                ),
                        ssrcs = listOf(1234L),
                        brBps = 1_000_000L).build()

                should("set the values correctly") {
                    rembPacket.senderSsrc shouldBe 4567L
                    RtcpFbRembPacket.getBrExp(rembPacket.buffer, 0) shouldBe 2
                    RtcpFbRembPacket.getBrMantissa(rembPacket.buffer, 0) shouldBe 250000
                    rembPacket.ssrcs shouldBe listOf(1234L)
                    rembPacket.bitrate shouldBe 1_000_000L
                }
            }
        }
    }
}
