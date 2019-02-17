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

package org.jitsi.rtp.new_scheme2

import java.nio.ByteBuffer
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

interface Mutable

interface Immutable

interface CanBecomeMutable<MutableType : Mutable> {
    fun toMutable(): MutableType

    fun getMutableCopy(): MutableType
}

abstract class CanBecomeImmutable<ImmutableType : Immutable> {
    /**
     * When something becomes immutable, the mutable version
     * of it is no longer valid (since we re-use the buffers
     * and letting the mutable version remain valid means
     * that the immutable version could be changed).
     * [locked] denotes whether or not this mutable
     * type can still be modified.
     */
    private var locked: Boolean = false

    protected fun<T> getLockableMutableMemberAlias(prop: KMutableProperty0<T>): LockableMutableAlias<T> {
        return LockableMutableAlias(prop, ::locked)
    }

    protected fun<T> getLockableImmutableMemberAlias(prop: KProperty0<T>): LockableImmutableAlias<T> {
        return LockableImmutableAlias(prop, ::locked)
    }

    fun toImmutable(): ImmutableType {
        val immutable = doGetImmutable()
        locked = true
        return immutable
    }

    protected abstract fun doGetImmutable(): ImmutableType
}

interface ConstructableFromBuffer<ConstructedType> {
    fun fromBuffer(buf: ByteBuffer): ConstructedType
}

interface Convertible<BaseType> {
    fun <NewType : BaseType> convertTo(factory: ConstructableFromBuffer<NewType>): NewType
}

class ImmutableAlias<T>(private val delegate: KProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        delegate.get()
}

class MutableAlias<T>(private val delegate: KMutableProperty0<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        delegate.get()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        delegate.set(value)
    }
}

class LockableImmutableAlias<T>(private val delegate: KProperty0<T>, private val isLocked: KProperty0<Boolean>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isLocked.get()) {
            throw Exception()
        }
        return delegate.get()
    }
}

class LockableMutableAlias<T>(private val delegate: KMutableProperty0<T>, private val isLocked: KProperty0<Boolean>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (isLocked.get()) {
            throw Exception()
        }
        return delegate.get()
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isLocked.get()) {
            throw Exception()
        }
        return delegate.set(value)
    }
}

