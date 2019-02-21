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

package org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.fci

import io.kotlintest.IsolationMode
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.extensions.unsigned.getUByte
import org.jitsi.rtp.extensions.unsigned.getUInt
import org.jitsi.rtp.util.byteBufferOf
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
internal class FirTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "Creating a FIR FCI" {
            "from values" {
                val fir = Fir(123u, 220u)
                should("set the values correctly") {
                    fir.ssrc shouldBe 123u
                    fir.seqNum shouldBe 220.toUByte()
                }
                "and then getting its buffer" {
                    val buf = fir.getBuffer()
                    should("serialize the data correctly") {
                        buf.getUInt() shouldBe 123u
                        buf.getUByte() shouldBe 220.toUByte()
                    }
                }
                "and then serializing to an existing buffer" {
                    val existingBuf = ByteBuffer.allocate(20)
                    existingBuf.position(10)
                    fir.serializeTo(existingBuf)
                    should("serialize it to the proper place") {
                        existingBuf.getUInt(10) shouldBe 123u
                        existingBuf.get(14) shouldBe 220.toByte()
                    }
                    should("leave the buffer's position after the field it just wrote") {
                        existingBuf.position() shouldBe 18
                    }
                }
            }
            "from a buffer" {
                val buf = byteBufferOf(
                    0x00, 0x00, 0x00, 0x7b,
                    0xDC, 0x00, 0x00, 0x00,
                    0xDE, 0xAD, 0xBE, 0xEF
                )
                val fir = Fir.fromBuffer(buf)
                should("parse the values correctly") {
                    fir.ssrc shouldBe 123u
                    fir.seqNum shouldBe 220.toUByte()
                }
                should("leave the buffer's position after its data") {
                    buf.position() shouldBe 8
                }
            }
        }
    }
}