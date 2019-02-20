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

package org.jitsi.rtp.new_scheme3

import java.nio.ByteBuffer
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

interface Mutable

abstract class Immutable {
    /**
     * Whether or not this [Immutable] instance is shared.  A shared [Immutable]
     * can only be modified via making a mutable copy, whereas a non-shared
     * [Immutable] can be converted to a [Mutable] directly.
     */
    protected var shared: Boolean = false

    fun toMutable(): Mutable {
        if (shared) {
            throw Exception("Shared Immutable cannot be converted directly to a Mutable type")
        }
        return doConvertToMutable()
    }

    protected abstract fun doConvertToMutable(): Mutable

    abstract fun getMutableCopy(): Mutable
}

///**
// * A [SharedImmutable] represents an immutable instance that is being shared
// * across many owners, meaning that in order to modify it a copy must be made
// */
//interface SharedImmutableWrapper<MutableType : Mutable> : Immutable {
//    fun getMutableCopy(): MutableType
//}
//
///**
// * An [OwnedImmutable] represents an immutable instance that is owned
// * by the current context, meaning that it can become mutable without
// * needing to be copied because there is no fear of conflicting with
// * other owners.
// */
//interface OwnedImmutableWrapper<MutableType : Mutable> : SharedImmutableWrapper<MutableType> {
//    fun toMutable(): MutableType
//}

//interface CanBecomeMutable<MutableType : Mutable> {
//    fun toMutable(): MutableType
//
//    fun getMutableCopy(): MutableType
//}

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

interface ConstructableFrom<FromType, ConstructedType> {
    fun create(fromType: FromType): ConstructedType
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

/**
 * A delegate to be used for serialized fields, where changing them should set a dirty flag
 * that any prior serialization needs to be updated to reflect this change
 */
class SerializedField<T>(initialValue: T, private var dirtyField: KMutableProperty0<Boolean>) {
    private var value: T = initialValue
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        dirtyField.set(true)
        this.value = value
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

