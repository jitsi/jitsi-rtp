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

package org.jitsi.rtp.new_scheme3.rtcp.data

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.util.BitBuffer
import java.nio.ByteBuffer

internal class RtcpHeaderDataTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "Creating RtcpHeaderData" {
            "from values" {
                val headerData = RtcpHeaderData(
                    version = 3,
                    hasPadding = true,
                    reportCount = 1,
                    packetType = 200,
                    length = 65535,
                    senderSsrc = 0xFFFFFFFF
                )
                should("set all the values correctly") {
                    headerData.version shouldBe 3
                    headerData.hasPadding shouldBe true
                    headerData.reportCount shouldBe 1
                    headerData.packetType shouldBe 200
                    headerData.length shouldBe 65535
                    headerData.senderSsrc shouldBe 0xFFFFFFFF
                }
                "and then retreiving its buffer" {
                    val buf = headerData.getBuffer()
                    should("serialize the data correctly") {
                        buf.compareTo(headerBuf) shouldBe 0
                    }
                }
                "and then serializing to an existing buffer" {
                    val existingBuf = ByteBuffer.allocate(RtcpHeaderData.SIZE_BYTES + 10)
                    existingBuf.position(5)
                    headerData.serializeTo(existingBuf)
                    should("serialize it to the proper place") {
                        val subBuf = existingBuf.subBuffer(5, RtcpHeaderData.SIZE_BYTES)
                        subBuf.compareTo(headerBuf) shouldBe 0
                    }
                    should("leave the buffer's position after the field it just wrote") {
                        existingBuf.position() shouldBe (5 + RtcpHeaderData.SIZE_BYTES)
                    }
                }
            }
            "from a buffer" {
                val header = RtcpHeaderData.create(headerBuf)
                should("parse all the values correctly") {
                    header.version shouldBe 3
                    header.hasPadding shouldBe true
                    header.reportCount shouldBe 1
                    header.packetType shouldBe 200
                    header.length shouldBe 65535
                    header.senderSsrc shouldBe 0xFFFFFFFF
                }
            }
        }
    }
    private val headerBuf = with(ByteBuffer.allocate(8)) {
        val bitBuffer = BitBuffer(this)
        bitBuffer.putBits(3.toByte(), 2) // version = 3
        bitBuffer.putBoolean(true) // padding = true
        bitBuffer.putBits(1.toByte(), 5) // report count = 1
        put(200.toByte()) // packet type = 200
        putShort(0xFFFF.toShort()) // length = 65535
        putInt(0xFFFFFFFF.toInt()) // sender ssrc = 4294967295
        this.rewind() as ByteBuffer
    }
}