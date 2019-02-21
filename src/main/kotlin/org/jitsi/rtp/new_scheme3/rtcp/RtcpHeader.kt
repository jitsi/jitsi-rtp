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

import org.jitsi.rtp.new_scheme3.ImmutableAlias
import org.jitsi.rtp.new_scheme3.SerializableData
import org.jitsi.rtp.new_scheme3.rtcp.data.RtcpHeaderData
import java.nio.ByteBuffer

abstract class ImmutableRtcpHeader internal constructor(
    protected val headerData: RtcpHeaderData = RtcpHeaderData()
) : SerializableData(), kotlin.Cloneable {
    override val sizeBytes: Int
        get() = headerData.sizeBytes

    val version: Int by ImmutableAlias(headerData::version)
    val hasPadding: Boolean by ImmutableAlias(headerData::hasPadding)
    val reportCount: Int by ImmutableAlias(headerData::reportCount)
    val packetType: Int by ImmutableAlias(headerData::packetType)
    val length: Int by ImmutableAlias(headerData::length)
    val senderSsrc: Long by ImmutableAlias(headerData::senderSsrc)

    private var dirty: Boolean = true

    constructor(
        version: Int = 2,
        hasPadding: Boolean = false,
        reportCount: Int = -1,
        packetType: Int = -1,
        length: Int = -1,
        senderSsrc: Long = -1
    ) : this(RtcpHeaderData(
        version, hasPadding, reportCount,
        packetType, length, senderSsrc))

    protected fun doModify(block: RtcpHeaderData.() -> Unit) {
        with (headerData) {
            block()
        }
        dirty = true
    }

    override fun serializeTo(buf: ByteBuffer) {
        headerData.serializeTo(buf)
    }
}

class RtcpHeader(
    headerData: RtcpHeaderData = RtcpHeaderData()
) : ImmutableRtcpHeader(headerData) {

    public override fun clone(): RtcpHeader =
            RtcpHeader(headerData.clone())

    fun modify(block: RtcpHeaderData.() -> Unit) {
        doModify(block)
    }

    override fun serializeTo(buf: ByteBuffer) {
        headerData.serializeTo(buf)
    }

    companion object {
        const val SIZE_BYTES = RtcpHeaderData.SIZE_BYTES
        fun create(buf: ByteBuffer): RtcpHeader {
            val headerData = RtcpHeaderData.create(buf)
            return RtcpHeader(headerData)
        }

        fun fromValues(
            version: Int = 2,
            hasPadding: Boolean = false,
            reportCount: Int = -1,
            packetType: Int = -1,
            length: Int = -1,
            senderSsrc: Long = -1
        ) : RtcpHeader {
            return RtcpHeader(RtcpHeaderData(
                version, hasPadding, reportCount,
                packetType, length, senderSsrc))
        }
    }
}