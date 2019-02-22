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

package org.jitsi.rtp.new_scheme3.rtcp.rtcpfb.fci.tcc

import io.kotlintest.IsolationMode
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.extensions.compareToFromBeginning
import org.jitsi.rtp.extensions.subBuffer
import org.jitsi.rtp.extensions.toHex
import org.jitsi.rtp.util.byteBufferOf
import java.nio.ByteBuffer

internal class PacketStatusChunkTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    data class RunLengthChunkSample(
        val chunkType: PacketStatusChunkType,
        val symbol: PacketStatusSymbol,
        val runLength: Int,
        val buffer: ByteBuffer
    )

    val runLengthChunkSample1 = RunLengthChunkSample(
        chunkType = PacketStatusChunkType.RUN_LENGTH_CHUNK,
        symbol = TwoBitPacketStatusSymbol.RECEIVED_SMALL_DELTA,
        runLength = 200,
        buffer = byteBufferOf(0x20, 0xC8)
    )

    data class StatusVectorChunkSample(
        val chunkType: PacketStatusChunkType,
        val symbolSizeBits: Int,
        val symbolList: List<PacketStatusSymbol>,
        val buffer: ByteBuffer
    )

    val statusVectorChunkSample1 = StatusVectorChunkSample(
        chunkType = PacketStatusChunkType.STATUS_VECTOR_CHUNK,
        symbolSizeBits = 1,
        symbolList = listOf(
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.NOT_RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED,
            OneBitPacketStatusSymbol.RECEIVED
        ),
        buffer = byteBufferOf(0xB3, 0x33)
    )

    init {
        "Creating a RunLengthChunk" {
            "from a buffer" {
                val runLengthChunk = RunLengthChunk.fromBuffer(runLengthChunkSample1.buffer)
                should("parse the values correctly") {
                    runLengthChunk.statusSymbol shouldBe runLengthChunkSample1.symbol
                    runLengthChunk.runLength shouldBe runLengthChunkSample1.runLength
                }
                should("leave the buffer's position after the parsed data") {
                    runLengthChunkSample1.buffer.position() shouldBe runLengthChunkSample1.buffer.limit()
                }
                "and then getting its buffer" {
                    val serializedBuf = runLengthChunk.getBuffer()
                    should("serialize the fields correctly") {
                        serializedBuf.compareToFromBeginning(runLengthChunkSample1.buffer) shouldBe 0
                    }
                }
                "and then serializing it to an existing buffer" {
                    val existingBuf = ByteBuffer.allocate(10)
                    existingBuf.position(5)
                    runLengthChunk.serializeTo(existingBuf)
                    should("write the values to the right place") {
                        existingBuf.subBuffer(5, PacketStatusChunk.SIZE_BYTES)
                                .compareToFromBeginning(runLengthChunkSample1.buffer) shouldBe 0
                    }
                    should("leave the buffer's position after the written data") {
                        existingBuf.position() shouldBe (5 + PacketStatusChunk.SIZE_BYTES)
                    }
                }
           }
            "from values" {
                val runLengthChunk = RunLengthChunk(runLengthChunkSample1.symbol, runLengthChunkSample1.runLength)
                should("set the fields correctly") {
                    runLengthChunk.statusSymbol shouldBe runLengthChunkSample1.symbol
                    runLengthChunk.runLength shouldBe runLengthChunkSample1.runLength
                }
            }
        }
        "Creating a StatusVectorChunk" {
            "from a buffer" {
                val statusVectorChunk = StatusVectorChunk.fromBuffer(statusVectorChunkSample1.buffer)
                should("parse the values correctly") {
                    statusVectorChunk.symbolSizeBits shouldBe 1
                    statusVectorChunk.packetStatusSymbols shouldContainExactly statusVectorChunkSample1.symbolList
                }
                should("leave the buffer's position after the parsed data") {
                    statusVectorChunkSample1.buffer.position() shouldBe statusVectorChunkSample1.buffer.limit()
                }
                "and then getting its buffer" {
                    val serializedBuf = statusVectorChunk.getBuffer()
                    should("serialize the fields correctly") {
                        serializedBuf.compareToFromBeginning(statusVectorChunkSample1.buffer) shouldBe 0
                    }
                }
                "and then serializing it to an existing buffer" {
                    val existingBuf = ByteBuffer.allocate(10)
                    existingBuf.position(5)
                    statusVectorChunk.serializeTo(existingBuf)
                    should("write the values to the right place") {
                        existingBuf.subBuffer(5, PacketStatusChunk.SIZE_BYTES)
                                .compareToFromBeginning(statusVectorChunkSample1.buffer) shouldBe 0
                    }
                    should("leave the buffer's position after the written data") {
                        existingBuf.position() shouldBe (5 + PacketStatusChunk.SIZE_BYTES)
                    }
                }
            }
            "from values" {
                val runLengthChunk = RunLengthChunk(runLengthChunkSample1.symbol, runLengthChunkSample1.runLength)
                should("set the fields correctly") {
                    runLengthChunk.statusSymbol shouldBe runLengthChunkSample1.symbol
                    runLengthChunk.runLength shouldBe runLengthChunkSample1.runLength
                }
            }
        }
    }
}