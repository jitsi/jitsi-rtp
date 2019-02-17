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
import io.kotlintest.matchers.collections.shouldContainInOrder
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrowUnit
import io.kotlintest.specs.BehaviorSpec

internal class RtpHeaderTest : BehaviorSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        given("an ImmutableRtpHeader") {
            val immutable = ImmutableRtpHeader(
                payloadType = 42,
                sequenceNumber = 1234,
                timestamp = 123456,
                ssrc = 45678,
                csrcs = listOf(123, 456)
            )
            `when`("a mutable copy is created") {
                val mutable = immutable.getMutableCopy()
                and("modified") {
                    mutable.payloadType = 43
                    mutable.sequenceNumber = 1235
                    mutable.timestamp = 123457
                    mutable.ssrc = 45679
                    mutable.csrcs.add(789)
                    then("the immutable instance should not be changed") {
                        immutable.payloadType shouldBe 42
                        immutable.sequenceNumber shouldBe 1234
                        immutable.timestamp shouldBe 123456
                        immutable.ssrc shouldBe 45678
                        immutable.csrcs.shouldContainInOrder(123L, 456L)
                    }
                }
            }
//            `when`("it is modified in place") {
//                immutable.modifyInPlace {
//                    payloadType = 43
//                    sequenceNumber = 1235
//                    timestamp = 123457
//                    ssrc = 45679
//                    csrcs.add(789)
//                }
//                then("the changes should be reflected") {
//                    immutable.payloadType shouldBe 43
//                    immutable.sequenceNumber shouldBe 1235
//                    immutable.timestamp shouldBe 123457
//                    immutable.ssrc shouldBe 45679
//                    immutable.csrcs.shouldContainInOrder(123L, 456L, 789L)
//                }
//            }
        }
        given("a MutableRtpHeader") {
            val mutable = MutableRtpHeader(
                payloadType = 42,
                sequenceNumber = 1234,
                timestamp = 123456,
                ssrc = 45678,
                csrcs = mutableListOf(123, 456)
            )
            `when`("a change is made") {
                mutable.version = 3
                then("the changes should be reflected") {
                    mutable.version shouldBe 3
                }
            }
            `when`("it is converted to mutable") {
                mutable.toImmutable()
                and("a further change is attempted") {
                    then("it should throw an exception") {
                        shouldThrowUnit<Exception> {
                            mutable.ssrc = 789L
                        }
                    }
                }
            }
        }
    }
}