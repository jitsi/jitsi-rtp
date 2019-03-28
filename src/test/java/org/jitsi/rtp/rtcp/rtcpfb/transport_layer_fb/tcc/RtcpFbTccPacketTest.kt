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

package org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.tcc

import io.kotlintest.matchers.maps.shouldContainKey
import io.kotlintest.matchers.withClue
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.util.byteBufferOf
import org.jitsi.rtp.rtcp.RtcpHeaderBuilder
import org.jitsi.rtp.rtcp.rtcpfb.transport_layer_fb.tcc2.RtcpFbTccPacket2Builder
import org.jitsi.test_helpers.matchers.haveSameContentAs

class RtcpFbTccPacketTest : ShouldSpec() {
    private val tccRleData = byteBufferOf(
        //V=2,P=false,FMT=15,PT=205,L=7(32 bytes)
        0x8f, 0xcd, 0x00, 0x07,
        //Sender SSRC = 839852602
        0x32, 0x0f, 0x22, 0x3a,
        //Media source SSRC = 2397376430
        0x8e, 0xe5, 0x0f, 0xae,
        // Base seq num = 1969, packet status count = 9
        0x07, 0xb1, 0x00, 0x09,
        //Reference Time: 1683633 = 107752512ms, feedback packet count = 87
        0x19, 0xb0, 0xb1, 0x57,
        // Chunks
        // RLE, small delta, length = 9
        0x20, 0x09,
        // Deltas (9): 54, 0, 6, 5, 6, 5, 6, 5, 6
        0xd8, 0x00,
        0x18, 0x14, 0x18, 0x14,
        0x18, 0x14, 0x18,
        //  Recv delta padding
        0x00
    )
    val expectedTccRlePacketInfo = mapOf<Int, Long> (
        1969 to 107752512 + 54,
        1970 to 107752512 + 54 + 0,
        1971 to 107752512 + 54 + 0 + 6,
        1972 to 107752512 + 54 + 0 + 6 + 5,
        1973 to 107752512 + 54 + 0 + 6 + 5 + 6,
        1974 to 107752512 + 54 + 0 + 6 + 5 + 6 + 5,
        1975 to 107752512 + 54 + 0 + 6 + 5 + 6 + 5 + 6,
        1976 to 107752512 + 54 + 0 + 6 + 5 + 6 + 5 + 6 + 5,
        1977 to 107752512 + 54 + 0 + 6 + 5 + 6 + 5 + 6 + 5 + 6
    )

    // This also has a negative delta
    private val tccMixedChunkTypeData = byteBufferOf(
        //V=2,P=false,FMT=15,PT=205,L=9(40 bytes)
        0x8f, 0xcd, 0x00, 0x09,
        //Sender SSRC = 839852602
        0x32, 0x0f, 0x22, 0x3a,
        //Media source SSRC = 2397376430
        0x8e, 0xe5, 0x0f, 0xae,
        //Base seq num = 5376, packet status count = 12
        0x15, 0x00, 0x00, 0x0c,
        //Reference Time: 1684065 = 107780160ms, feedback packet count = 88
        0x19, 0xb2, 0x61, 0x58,
        //Chunks
        // RLE: small delta, length = 9
        0x20, 0x09,
        // SV, 2 bit symbols: LD, SD, SD
        0xe5, 0x00,
        // Deltas (12)
        // 2, 0, 0, 0
        0x08, 0x00, 0x00, 0x00,
        // 22, 1, 0, 0
        0x58, 0x04, 0x00, 0x00,
        // 8, -1, 1
        0x20, 0xff, 0xfc, 0x04,
        // 0
        0x00,
        // Recv delta padding
        0x00, 0x00, 0x00
    )
    val expectedTccMixedChunkTypePacketInfo = mapOf<Int, Long> (
        5376 to 107780160 + 2,
        5377 to 107780160 + 2 + 0,
        5378 to 107780160 + 2 + 0 + 0,
        5379 to 107780160 + 2 + 0 + 0 + 0,
        5380 to 107780160 + 2 + 0 + 0 + 0 + 22,
        5381 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1,
        5382 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0,
        5383 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0 + 0,
        5384 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0 + 0 + 8,
        5385 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0 + 0 + 8 + -1,
        5386 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0 + 0 + 8 + -1 + 1,
        5387 to 107780160 + 2 + 0 + 0 + 0 + 22 + 1 + 0 + 0 + 8 + -1 + 1 + 0
    )

    private val tccSvChunkData = byteBufferOf(
        //V=2,P=false,FMT=15,PT=205,length=5(24 bytes)
        0x8f, 0xcd, 0x00, 0x05,
        //Sender SSRC: 839852602
        0x32, 0x0f, 0x22, 0x3a,
        //Media source SSRC: 2397376430
        0x8e, 0xe5, 0x0f, 0xae,
        //Base seq num = 6227, packet status count = 2
        0x18, 0x53, 0x00, 0x02,
        //Reference Time: 1684126 (107784064ms), feedback packet count = 162
        0x19, 0xb2, 0x9e, 0xa2,
        // Chunks
        // SV chunk, 2 bit symbols: NR, SD
        0xc4, 0x00,
        // Deltas (1)
        // 00
        0x00,
        // Recv delta padding
        0x00
    )
    val expectedTccSvChunkPacketInfo = mapOf<Int, Long> (
        6227 to -1,
        6228 to 107784064 + 27
    )

    init {
        "Parsing an RtcpFbTccPacket" {
            "with RLE" {
                val rtcpFbTccPacket = RtcpFbTccPacket(tccRleData.array(), tccRleData.arrayOffset(), tccRleData.limit())
                should("parse the values correctly") {
                    rtcpFbTccPacket.forEach { (seqNum, recvTimestamp) ->
                        expectedTccRlePacketInfo shouldContainKey seqNum
                        expectedTccRlePacketInfo[seqNum] shouldBe recvTimestamp
                    }
                }
            }
            "with mixed chunk types and a negative delta" {
                val rtcpFbTccPacket = RtcpFbTccPacket(tccMixedChunkTypeData.array(), tccMixedChunkTypeData.arrayOffset(), tccMixedChunkTypeData.limit())
                should("parse the values correctly") {
                    rtcpFbTccPacket.forEach { (seqNum, recvTimestamp) ->
                        expectedTccMixedChunkTypePacketInfo shouldContainKey seqNum
                        withClue("seqNum $seqNum timestamp") {
                            recvTimestamp shouldBe expectedTccMixedChunkTypePacketInfo[seqNum]
                        }
                    }
                }
            }
        }
        "Creating an RtcpFbTccPacket" {
            val rtcpFbTccPacketBuilder = RtcpFbTccPacketBuilder(
                rtcpHeader = RtcpHeaderBuilder(
                    senderSsrc = 839852602
                ),
                mediaSourceSsrc = 2397376430,
                feedbackPacketCount = 162
            )
            rtcpFbTccPacketBuilder.addPacket(6228, 107784064) shouldBe true
            rtcpFbTccPacketBuilder.addPacket(6227, -1) shouldBe true

            val packet = rtcpFbTccPacketBuilder.build()
            should("serialize the data correctly") {
                val x = packet.buffer
                packet.buffer should haveSameContentAs(tccSvChunkData.array())
            }
            "With a delta that's too big" {
                rtcpFbTccPacketBuilder.addPacket(6229, 107784064 + 10000) shouldBe false

            }
        }
        "f:blah" {
            val packets = mutableListOf(
                7 to 1553754063147000,
                8 to 1553754063149000,
                9 to 1553754063150000,
                10 to 1553754063151000,
                11 to 1553754063152000,
                12 to 1553754063154000,
                13 to 1553754063156000,
                14 to 1553754063156000,
                15 to 1553754063158000,
                16 to 1553754063160000,
                17 to 1553754063161000,
                18 to 1553754063163000,
                19 to 1553754063164000,
                20 to 1553754063166000,
                21 to 1553754063167000,
                22 to 1553754063201000,
                23 to 1553754063204000,
                24 to 1553754063208000,
                25 to 1553754063213000,
                26 to 1553754063218000,
                27 to 1553754063223000,
                28 to 1553754063230000,
                29 to 1553754063230000,
                30 to 1553754063234000,
                31 to 1553754063240000,
                32 to 1553754063241000,
                33 to 1553754063246000,
                34 to 1553754063251000,
                35 to 1553754063256000,
                36 to 1553754063260000,
                37 to 1553754063267000,
                38 to 1553754063268000,
                39 to 1553754063384000
            )
            val rtcpFbTccPacketBuilder = RtcpFbTccPacket2Builder(
                baseSeqNo = 7
            )
            packets.forEach { (seqNum, timestampUs) ->
                rtcpFbTccPacketBuilder.addReceivedPacket(seqNum, timestampUs)
            }
            val pkt = rtcpFbTccPacketBuilder.build()
            println(pkt.toHex())
        }
        "Creating an RtcpFbTccPacket2" {
            val rtcpFbTccPacketBuilder = RtcpFbTccPacket2Builder(
                rtcpHeader = RtcpHeaderBuilder(
                    senderSsrc = 839852602
                ),
                mediaSourceSsrc = 2397376430,
                feedbackPacketSeqNum = 162,
                baseSeqNo = 6227
            )
            rtcpFbTccPacketBuilder.addReceivedPacket(6228, 107784064) shouldBe true
//            rtcpFbTccPacketBuilder.addReceivedPacket(6227, -1) shouldBe true

            val packet = rtcpFbTccPacketBuilder.build()
            should("serialize the data correctly") {
                val x = packet.buffer
                packet.buffer should haveSameContentAs(tccSvChunkData.array())
            }
            "With a delta that's too big" {
                rtcpFbTccPacketBuilder.addReceivedPacket(6229, 107784064 + 10000) shouldBe false
            }
        }
    }

}