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

package org.jitsi.rtp

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.ShouldSpec
import java.nio.ByteBuffer

internal class RtpPacketTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        val rtpHeader = RtpHeader(
            version = 1,
            hasPadding = false,
            csrcCount = 0,
            marker = false,
            payloadType = 96,
            sequenceNumber = 1234,
            timestamp = 123456,
            ssrc = 1234567
        )
        val payload = byteArrayOf(
            0x42, 0x42, 0x42, 0x42,
            0x42, 0x42, 0x42, 0x42
        )

        val buf = ByteBuffer.allocate(1500)
        buf.put(rtpHeader.getBuffer())
        buf.put(payload)

        buf.flip()

        "parsing a packet from a buffer" {
            val rtpPacket = RtpPacket(buf)
            should("parse the header correctly") {
                rtpPacket.header shouldBe rtpHeader
            }
            should("parse the payload correctly") {
                rtpPacket.payload.limit() shouldBe 8
                rtpPacket.payload shouldBe ByteBuffer.wrap(payload)
            }

            "and then adding a header extension" {
                val ext = TccHeaderExtension(5, 10)
                rtpPacket.header.addExtension(5, ext)
                // Get the buffer of the packet we added the extension to and parse it into
                // a new packet (since that's the easiest way to verify it and parsing a buffer
                // into an RtpPacket is tested elsewhere)
                val newPacket = RtpPacket(rtpPacket.getBuffer())

                should("add the extension correctly") {
                    newPacket.header.hasExtension shouldBe true
                    val newExt = newPacket.header.getExtension(ext.id)
                    newExt shouldNotBe null
                    newExt as RtpHeaderExtension
                    newExt.id shouldBe ext.id
                    newExt.lengthBytes shouldBe ext.lengthBytes
                    for (i in 0 until ext.lengthBytes) {
                        newExt.data.get(i) shouldBe ext.data.get(i)
                    }
                }
                should("not modify the payload") {
                    val newPayload = newPacket.payload
                    for (i in 0 until payload.size) {
                        newPayload.get(i) shouldBe payload.get(i)
                    }
                }
            }

        }

        "from another buf" {
            // sender ssrc should be 2656546059
            val packetBuf = byteArrayOf(
                0x80.toByte(), 0xC8.toByte(), 0x00.toByte(), 0x06.toByte(),
                0x9E.toByte(), 0x57.toByte(), 0xAD.toByte(), 0x0B.toByte(),
                0x6F.toByte(), 0x88.toByte(), 0x3D.toByte(), 0x57.toByte(),
                0xD1.toByte(), 0x1C.toByte(), 0x10.toByte(), 0xF9.toByte(),
                0x35.toByte(), 0x56.toByte(), 0x9D.toByte(), 0x1E.toByte(),
                0x28.toByte(), 0x06.toByte(), 0x3F.toByte(), 0x76.toByte(),
                0x61.toByte(), 0x4E.toByte(), 0xA1.toByte(), 0x00.toByte(),
                0xB6.toByte(), 0x73.toByte(), 0xFE.toByte(), 0x86.toByte(),
                0x74.toByte(), 0x47.toByte(), 0x77.toByte(), 0x8A.toByte(),
                0x92.toByte(), 0x52.toByte(), 0x00.toByte(), 0x36.toByte(),
                0x13.toByte(), 0x89.toByte(), 0x3A.toByte(), 0x88.toByte(),
                0xD2.toByte(), 0x3F.toByte(), 0x2F.toByte(), 0x6F.toByte(),
                0x9D.toByte(), 0x62.toByte(), 0xCD.toByte(), 0x41.toByte(),
                0xC8.toByte(), 0x59.toByte(), 0x95.toByte(), 0xA4.toByte(),
                0x80.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
                0x28.toByte(), 0xD6.toByte(), 0xFD.toByte(), 0x27.toByte(),
                0x40.toByte(), 0x88.toByte(), 0xED.toByte(), 0xC0.toByte(),
                0x40.toByte(), 0xA4.toByte()
            )

            val p = SrtcpPacket(ByteBuffer.wrap(packetBuf))
        }
    }
}
