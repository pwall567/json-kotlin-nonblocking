/*
 * @(#) JSONCoStringify.kt
 *
 * json-kotlin-nonblocking Non-blocking JSON serialization for Kotlin
 * Copyright (c) 2020 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.json

import kotlin.reflect.KProperty
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.BitSet
import java.util.Calendar
import java.util.Date
import java.util.Enumeration
import java.util.UUID

import net.pwall.json.JSONSerializerFunctions.findToJSON
import net.pwall.json.JSONSerializerFunctions.formatCalendar
import net.pwall.json.JSONSerializerFunctions.isSealedSubclass
import net.pwall.util.pipeline.IntCoAcceptor
import net.pwall.util.pipeline.output
import net.pwall.util.pipeline.outputHex
import net.pwall.util.pipeline.outputInt
import net.pwall.util.pipeline.outputLong

/**
 * Non-blocking JSON Serialization.
 *
 * @author  Peter Wall
 */
object JSONCoStringify {

    suspend fun IntCoAcceptor<*>.outputJSON(obj: Any?, config: JSONConfig = JSONConfig.defaultConfig) {

        if (obj == null) {
            output("null")
            return
        }

        config.findToJSONMapping(obj::class)?.let {
            outputJSONValue(it(obj))
            return
        }

        when (obj) {
            is JSONValue -> outputJSONValue(obj)
            is CharSequence -> outputJSONString(obj)
            is CharArray -> {
                output('"')
                for (ch in obj)
                    outputJSONChar(ch)
                output('"')
            }
            is Char -> {
                output('"')
                outputJSONChar(obj)
                output('"')
            }
            is Number -> outputJSONNumber(obj, config)
            is Boolean -> output(if (obj) "true" else "false")
            is Array<*> -> outputJSONArray(obj, config)
            is Pair<*, *> -> outputJSONPair(obj, config)
            is Triple<*, *, *> -> outputJSONTriple(obj, config)
            else -> {
                findToJSON(obj::class)?.let {
                    try {
                        outputJSONValue(it.call(obj))
                        return
                    }
                    catch (e: Exception) {
                        throw JSONException("Error in custom toJSON - ${obj::class.simpleName}", e)
                    }
                }
                outputJSONObject(obj, config)
            }
        }

    }

    private suspend fun IntCoAcceptor<*>.outputJSONNumber(number: Number, config: JSONConfig) {
        when (number) {
            is Int -> outputInt(number)
            is Short, is Byte -> outputInt(number.toInt())
            is Long -> outputLong(number)
            is Float, is Double -> output(number.toString())
            is BigInteger -> {
                if (config.bigIntegerString) {
                    output('"')
                    output(number.toString())
                    output('"')
                }
                else
                    output(number.toString())
            }
            is BigDecimal -> {
                if (config.bigDecimalString) {
                    output('"')
                    output(number.toString())
                    output('"')
                }
                else
                    output(number.toString())
            }
            else -> outputJSONObject(number, config)
        }
    }

    private suspend fun IntCoAcceptor<*>.outputJSONArray(array: Array<*>, config: JSONConfig) {
        if (array.isArrayOf<Char>()) {
            output('"')
            for (ch in array)
                outputJSONChar(ch as Char)
            output('"')
        }
        else {
            output('[')
            if (array.isNotEmpty()) {
                for (i in array.indices) {
                    if (i > 0)
                        output(',')
                    outputJSON(array[i], config)
                }
            }
            output(']')
        }
    }

    private suspend fun IntCoAcceptor<*>.outputJSONPair(pair: Pair<*, *>, config: JSONConfig) {
        output('[')
        outputJSON(pair.first, config)
        output(',')
        outputJSON(pair.second, config)
        output(']')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONTriple(triple: Triple<*, *, *>, config: JSONConfig) {
        output('[')
        outputJSON(triple.first, config)
        output(',')
        outputJSON(triple.second, config)
        output(',')
        outputJSON(triple.third, config)
        output(']')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONObject(obj: Any, config: JSONConfig) {
        when (obj) {
            is Iterable<*> -> outputJSONIterator(obj.iterator(), config)
            is Iterator<*> -> outputJSONIterator(obj, config)
            is Sequence<*> -> outputJSONIterator(obj.iterator(), config)
            is Enumeration<*> -> outputJSONEnumeration(obj, config)
            is Map<*, *> -> outputJSONMap(obj, config)
            // Channel? Flow?
            is Enum<*>,
            is java.sql.Date,
            is java.sql.Time,
            is java.sql.Timestamp,
            is Instant,
            is LocalDate,
            is LocalDateTime,
            is OffsetTime,
            is OffsetDateTime,
            is ZonedDateTime,
            is Year,
            is YearMonth,
            is Duration,
            is Period,
            is URI,
            is URL,
            is UUID -> outputJSONString(obj.toString())
            is Calendar -> outputJSONString(formatCalendar(obj))
            is Date -> outputJSONString(formatCalendar(Calendar.getInstance().apply { time = obj }))
            is BitSet -> outputJSONBitSet(obj)
            else -> {
                output('{')
                var continuation = false
                val objClass = obj::class
                if (objClass.isSealedSubclass()) {
                    outputJSONString(config.sealedClassDiscriminator)
                    output(':')
                    outputJSONString(objClass.simpleName ?: "null")
                    continuation = true
                }
                val includeAll = config.hasIncludeAllPropertiesAnnotation(objClass.annotations)
                val statics: Collection<KProperty<*>> = objClass.staticProperties
                if (objClass.isData && objClass.constructors.isNotEmpty()) {
                    // data classes will be a frequent use of serialization, so optimise for them
                    val constructor = objClass.constructors.first()
                    for (parameter in constructor.parameters) {
                        val member = objClass.members.find { it.name == parameter.name }
                        if (member is KProperty<*>)
                            continuation = outputUsingGetter(member, parameter.annotations, obj, config, includeAll,
                                    continuation)
                    }
                    // now check whether there are any more properties not in constructor
                    for (member in objClass.members) {
                        if (member is KProperty<*> && !statics.contains(member) &&
                                !constructor.parameters.any { it.name == member.name })
                            continuation = outputUsingGetter(member, member.annotations, obj, config, includeAll,
                                    continuation)
                    }
                }
                else {
                    for (member in objClass.members) {
                        if (member is KProperty<*> && !statics.contains(member)) {
                            val combinedAnnotations = ArrayList(member.annotations)
                            objClass.constructors.firstOrNull()?.parameters?.find { it.name == member.name }?.let {
                                combinedAnnotations.addAll(it.annotations)
                            }
                            continuation = outputUsingGetter(member, combinedAnnotations, obj, config, includeAll,
                                    continuation)
                        }
                    }
                }
                output('}')
            }
        }
    }

    private suspend fun IntCoAcceptor<*>.outputUsingGetter(member: KProperty<*>, annotations: List<Annotation>?,
            obj: Any, config: JSONConfig, includeAll: Boolean, continuation: Boolean): Boolean {
        if (!config.hasIgnoreAnnotation(annotations)) {
            val name = config.findNameFromAnnotation(annotations) ?: member.name
            val wasAccessible = member.isAccessible
            member.isAccessible = true
            try {
                val v = member.getter.call(obj)
                if (v != null || config.hasIncludeIfNullAnnotation(annotations) || config.includeNulls || includeAll) {
                    if (continuation)
                        output(',')
                    outputJSONString(name)
                    output(':')
                    outputJSON(v, config)
                    return true
                }
            }
            catch (e: JSONException) {
                throw e
            }
            catch (e: Exception) {
                throw JSONException("Error getting property ${member.name} from ${obj::class.simpleName}", e)
            }
            finally {
                member.isAccessible = wasAccessible
            }
        }
        return continuation
    }

    private suspend fun IntCoAcceptor<*>.outputJSONIterator(iterator: Iterator<*>, config: JSONConfig) {
        output('[')
        if (iterator.hasNext()) {
            while (true) {
                outputJSON(iterator.next(), config)
                if (!iterator.hasNext())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONEnumeration(enumeration: Enumeration<*>, config: JSONConfig) {
        output('[')
        if (enumeration.hasMoreElements()) {
            while (true) {
                outputJSON(enumeration.nextElement(), config)
                if (!enumeration.hasMoreElements())
                    break
                output(',')
            }
        }
        output(']')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONMap(map: Map<*, *>, config: JSONConfig) {
        output('{')
        map.entries.iterator().let {
            if (it.hasNext()) {
                while (true) {
                    val ( key, value ) = it.next()
                    outputJSONString(key.toString())
                    output(':')
                    outputJSON(value, config)
                    if (!it.hasNext())
                        break
                    output(',')
                }
            }
        }
        output('}')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONBitSet(bitSet: BitSet) {
        output('[')
        var continuation = false
        for (i in 0 until bitSet.length()) {
            if (bitSet.get(i)) {
                if (continuation)
                    output(',')
                outputInt(i)
                continuation = true
            }
        }
        output(']')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONValue(jsonValue: JSONValue?) {
        when (jsonValue) {
            null -> output("null")
            is JSONString -> outputJSONString(jsonValue.get())
            is JSONInt -> outputInt(jsonValue.get())
            is JSONLong -> outputLong(jsonValue.get())
            is JSONBoolean,
            is JSONFloat,
            is JSONDouble,
            is JSONDecimal -> output(jsonValue.toString())
            is JSONZero -> output('0')
            is JSONArray -> {
                output('[')
                jsonValue.iterator().let {
                    if (it.hasNext()) {
                        while (true) {
                            outputJSONValue(it.next())
                            if (!it.hasNext())
                                break
                            output(',')
                        }
                    }
                }
                output(']')
            }
            is JSONObject -> {
                output('{')
                jsonValue.entries.iterator().let {
                    if (it.hasNext()) {
                        while (true) {
                            it.next().let { (key, value) ->
                                outputJSONString(key)
                                output(':')
                                outputJSONValue(value)
                            }
                            if (!it.hasNext())
                                break
                            output(',')
                        }
                    }
                }
                output('}')
            }
        }
    }

    private suspend fun IntCoAcceptor<*>.outputJSONString(cs: CharSequence) {
        output('"')
        for (ch in cs)
            outputJSONChar(ch)
        output('"')
    }

    private suspend fun IntCoAcceptor<*>.outputJSONChar(ch: Char) {
        when (ch) {
            '"', '\\' -> {
                output('\\')
                output(ch)
            }
            in ' '..'\u007F' -> output(ch)
            '\n' -> output("\\n")
            '\t' -> output("\\t")
            '\r' -> output("\\r")
            '\b' -> output("\\b")
            '\u000C' -> output("\\f")
            else -> {
                output("\\u")
                outputHex(ch.toShort())
            }
        }
    }

}
