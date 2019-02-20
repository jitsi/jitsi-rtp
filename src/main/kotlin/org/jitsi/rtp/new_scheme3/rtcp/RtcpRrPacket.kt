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

package org.jitsi.rtp.new_scheme3.rtcp

import org.jitsi.rtp.new_scheme3.Packet
import java.nio.ByteBuffer

class RtcpRrPacket(
    header: RtcpHeader = RtcpHeader(),
    val reportBlocks: List<RtcpReportBlock> = listOf(),
    backingBuffer: ByteBuffer? = null
) : RtcpPacket(header, backingBuffer) {
    override val sizeBytes: Int
        get() = header.sizeBytes + (reportBlocks.size * RtcpReportBlock.SIZE_BYTES)


    override fun serializeTo(buf: ByteBuffer) {
        super.serializeTo(buf)
        reportBlocks.forEach { it.serializeTo(buf) }
    }

    override fun clone(): Packet {
        TODO()
    }

    companion object {
        const val PT: Int = 201

        fun fromBuffer(buf: ByteBuffer): RtcpRrPacket {
            val header = RtcpHeader.create(buf)
            val reportBlocks = (1..header.reportCount)
                    .map { RtcpReportBlock.fromBuffer(buf) }
                    .toList()
            return RtcpRrPacket(header, reportBlocks, buf)
        }
    }
}