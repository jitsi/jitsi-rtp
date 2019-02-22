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

package org.jitsi.rtp.new_scheme3.rtcp.rtcpfb

import io.kotlintest.specs.ShouldSpec
import org.jitsi.rtp.util.byteBufferOf

internal class RtcpFbTccPacketTest : ShouldSpec() {
    val bufFromCall = byteBufferOf(
        0x8F, 0xCD, 0x00, 0x05,
        0x32, 0x0F, 0x22, 0x3A,
        0x8E, 0xE5, 0x0F, 0xAE,
        0x00, 0x21, 0x00, 0x01,
        0x19, 0xAE, 0xB1, 0x02,
        0x20, 0x01, 0xB8, 0x00
    )

    init {
        val tcc = RtcpFbTccPacket.fromBuffer(bufFromCall)
    }
}