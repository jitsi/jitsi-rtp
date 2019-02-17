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

interface InPlaceModifier

interface CanBecomeMutable<MutableType : Mutable> {
//    companion object {
//        fun <T : Mutable>modifyInPlace(immutable: CanBecomeMutable<T>, block: T.() -> Unit) {
//            with (immutable.toMutable()) {
//                block()
//            }
//        }
//    }

    fun toMutable(): MutableType {
        TODO()
    }

    /**
     * [getInPlaceModifier] should ONLY be used if you know
     * you have exclusive ownership of the instance.
     * If so, this provides a more convenient way of
     * making changes to an immutable instance.
     */
    fun getInPlaceModifier(): InPlaceModifier

    fun getMutableCopy(): MutableType
}

interface CanBecomeImmutable<ImmutableType : Immutable> {
    fun toImmutable(): ImmutableType
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

