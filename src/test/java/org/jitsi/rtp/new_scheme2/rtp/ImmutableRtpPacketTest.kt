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

package org.jitsi.rtp.new_scheme2.rtp

import io.kotlintest.IsolationMode
import io.kotlintest.specs.BehaviorSpec
import org.junit.jupiter.api.Assertions.*
import java.nio.ByteBuffer

internal class ImmutableRtpPacketTest : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf


    init {
        given("an immutable RTP packet created from a single buffer") {
            val immutableHeader = ImmutableRtpHeader()
            val payload = ByteBuffer.allocate(20)
            val buf = ByteBuffer.allocate(immutableHeader.sizeBytes + payload.limit())
            val immutablePacket = ImmutableRtpPacket.fromBuffer(buf)
            `when`("the buffer is retrieved") {
                then("it should serialize correctly") {
                    //NOTE: there was a bug where we weren't properly duplicating ByteBuffers
                    // and the sub-buffers within a packet (the one for the header vs
                    // the one for the payload) would conflict when trying to get the buffer
                    // and an exception would be thrown, so this tests that that works
                    // correctly
                    val retrievedBuf = immutablePacket.getBuffer()
                }
            }
        }
    }
}