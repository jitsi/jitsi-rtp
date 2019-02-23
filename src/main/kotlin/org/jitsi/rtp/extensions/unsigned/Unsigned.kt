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

package org.jitsi.rtp.extensions.unsigned

import org.jitsi.rtp.extensions.subBuffer
import java.nio.Buffer
import java.nio.ByteBuffer

/**
 * NOTE(brian): many operations are still tedious with unsigned types.  This file
 * contains extensions to make them easier to work with.  It's true that some of
 * these may lead to some weird behavior, but I think it should be rare enough
 * in our use cases that we can manage.
 */

@ExperimentalUnsignedTypes
operator fun Int.compareTo(other: UInt): Int =
    this.toUInt().compareTo(other)

@ExperimentalUnsignedTypes
operator fun UInt.minus(other: Int): UInt =
    this.minus(other.toUInt())

@ExperimentalUnsignedTypes
operator fun UInt.plus(other: Int): UInt =
    this.plus(other.toUInt())

@ExperimentalUnsignedTypes
operator fun UInt.times(other: Int): UInt =
    this.times(other.toUInt())


@ExperimentalUnsignedTypes
fun ByteBuffer.getUByte() = get().toUByte()
@ExperimentalUnsignedTypes
fun ByteBuffer.getUByte(index: Int) = get(index).toUByte()
@ExperimentalUnsignedTypes
fun ByteBuffer.putUByte(value: UByte) = put(value.toByte())
@ExperimentalUnsignedTypes
fun ByteBuffer.putUByte(index: Int, value: UByte) = put(index, value.toByte())

@ExperimentalUnsignedTypes
fun ByteBuffer.getUShort() = short.toUShort()
@ExperimentalUnsignedTypes
fun ByteBuffer.getUShort(index: Int) = getShort(index).toUShort()
@ExperimentalUnsignedTypes
fun ByteBuffer.putUShort(value: UShort) = putShort(value.toShort())
@ExperimentalUnsignedTypes
fun ByteBuffer.putUShort(index: Int, value: UShort) = putShort(index, value.toShort())

@ExperimentalUnsignedTypes
fun ByteBuffer.getUInt() = int.toUInt()
@ExperimentalUnsignedTypes
fun ByteBuffer.getUInt(index: Int) = getInt(index).toUInt()
@ExperimentalUnsignedTypes
fun ByteBuffer.putUInt(value: UInt) = putInt(value.toInt())
@ExperimentalUnsignedTypes
fun ByteBuffer.putUInt(index: Int, value: UInt) = putInt(index, value.toInt())

@ExperimentalUnsignedTypes
fun ByteBuffer.getULong() = long.toULong()
@ExperimentalUnsignedTypes
fun ByteBuffer.getULong(index: Int) = getLong(index).toULong()
@ExperimentalUnsignedTypes
fun ByteBuffer.putULong(value: ULong) = putLong(value.toLong())
@ExperimentalUnsignedTypes
fun ByteBuffer.putULong(index: Int, value: ULong) = putLong(index, value.toLong())

fun ByteBuffer.incrementPosition(value: Int) {
    position(position() + value)
}
fun ByteBuffer.decrementPosition(value: Int) {
    position(position() - value)
}


@ExperimentalUnsignedTypes
fun ByteBuffer.incrementPosition(value: UInt) {
    incrementPosition(value.toInt())
}

@ExperimentalUnsignedTypes
fun ByteBuffer.uposition(): UInt = position().toUInt()

@ExperimentalUnsignedTypes
fun ByteBuffer.ulimit(): UInt = limit().toUInt()

@ExperimentalUnsignedTypes
fun ByteBuffer.position(value: UInt): Buffer =
    position(value.toInt())

@ExperimentalUnsignedTypes
fun allocateByteBuffer(capacity: UInt): ByteBuffer =
    ByteBuffer.allocate(capacity.toInt())

@ExperimentalUnsignedTypes
fun ByteBuffer.subBuffer(startPosition: UInt): ByteBuffer = subBuffer(startPosition.toInt())

@ExperimentalUnsignedTypes
fun ByteBuffer.subBuffer(startPosition: UInt, size: UInt): ByteBuffer = subBuffer(startPosition.toInt(), size.toInt())


fun Byte.toPositiveInt(): Int = toInt() and 0xFF
fun Short.toPositiveInt(): Int = toInt() and 0xFFFF
fun Int.toPositiveLong(): Long = toLong() and 0xFFFFFFFF
