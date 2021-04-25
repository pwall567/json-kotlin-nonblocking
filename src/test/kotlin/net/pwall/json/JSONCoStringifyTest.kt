/*
 * @(#) JSONCoStringifyTest.kt
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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.channels.Channel

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
import java.util.TimeZone
import java.util.UUID

import net.pwall.json.JSONCoStringify.outputJSON
import net.pwall.json.test.JSONExpect.Companion.expectJSON
import net.pwall.util.pipeline.StringCoAcceptor
import java.time.LocalTime
import java.time.MonthDay

class JSONCoStringifyTest {

    @Test fun `should stringify null`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(null)
        expect("null") { stringCoAcceptor.result }
    }

    @Test fun `should use toJSON if specified in JSONConfig`() = runBlocking {
        val config = JSONConfig().apply {
            toJSON<Dummy1> { obj ->
                obj?.let {
                    JSONObject().apply {
                        putValue("a", it.field1)
                        putValue("b", it.field2)
                    }
                }
            }
        }
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(Dummy1("xyz", 888), config)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("a", "xyz")
            property("b", 888)
        }
    }

    @Test fun `should stringify a JSONValue`() = runBlocking {
        val json = JSONObject().apply {
            putValue("a", "Hello")
            putValue("b", 27)
        }
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(json)
        expect("""{"a":"Hello","b":27}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a simple string`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON("abc")
        expect("\"abc\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a string with a newline`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON("a\nc")
        expect("\"a\\nc\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a string with a unicode sequence`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON("a\u2014c")
        expect("\"a\\u2014c\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a single character`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON('X')
        expect("\"X\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a charArray`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(charArrayOf('a', 'b', 'c'))
        expect("\"abc\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an integer`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(123456789)
        expect("123456789") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an integer 0`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(0)
        expect("0") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative integer`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(-888)
        expect("-888") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a long`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(123456789012345678)
        expect("123456789012345678") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative long`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(-111222333444555666)
        expect("-111222333444555666") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a short`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x: Short = 12345
        stringCoAcceptor.outputJSON(x)
        expect("12345") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative short`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x: Short = -4444
        stringCoAcceptor.outputJSON(x)
        expect("-4444") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a byte`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x: Byte = 123
        stringCoAcceptor.outputJSON(x)
        expect("123") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative byte`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x: Byte = -99
        stringCoAcceptor.outputJSON(x)
        expect("-99") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a float`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = 1.2345F
        stringCoAcceptor.outputJSON(x)
        expect("1.2345") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative float`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x: Float = -567.888F
        stringCoAcceptor.outputJSON(x)
        expect("-567.888") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a double`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = 1.23456789
        stringCoAcceptor.outputJSON(x)
        expect("1.23456789") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative double`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = -9.998877665
        stringCoAcceptor.outputJSON(x)
        expect("-9.998877665") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a BigInteger`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = BigInteger.valueOf(123456789000)
        stringCoAcceptor.outputJSON(x)
        expect("123456789000") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a negative BigInteger`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = BigInteger.valueOf(-123456789000)
        stringCoAcceptor.outputJSON(x)
        expect("-123456789000") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a BigInteger as string when specified in JSONConfig`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val config = JSONConfig().apply {
            bigIntegerString = true
        }
        val x = BigInteger.valueOf(123456789000)
        stringCoAcceptor.outputJSON(x, config)
        expect("\"123456789000\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a BigDecimal`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val x = BigDecimal("12345.678")
        stringCoAcceptor.outputJSON(x)
        expect("12345.678") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a BigDecimal as string when specified in JSONConfig`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val config = JSONConfig().apply {
            bigDecimalString = true
        }
        val x = BigDecimal("12345.678")
        stringCoAcceptor.outputJSON(x, config)
        expect("\"12345.678\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Boolean`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(true)
        expect("true") { stringCoAcceptor.result }
        stringCoAcceptor.reset()
        stringCoAcceptor.outputJSON(false)
        expect("false") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an array of characters`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(arrayOf('a', 'b', 'c'))
        expect("\"abc\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an array of integers`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(arrayOf(123, 456, 789))
        expect("[123,456,789]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an array of strings`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(arrayOf("Hello", "World"))
        expect("""["Hello","World"]""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an array of arrays`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(arrayOf(arrayOf(11, 22), arrayOf(33, 44)))
        expect("[[11,22],[33,44]]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an empty array`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(emptyArray<Int>())
        expect("[]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an array of nulls`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(arrayOfNulls<String>(3))
        expect("[null,null,null]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Pair of integers`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(123 to 789)
        expect("[123,789]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Pair of strings`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON("back" to "front")
        expect("""["back","front"]""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Triple of integers`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(Triple(123, 789, 0))
        expect("[123,789,0]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a class with a custom toJson`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(DummyFromJSON(49))
        expect("""{"dec":"49","hex":"31"}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a list of integers`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(listOf(1234, 4567, 7890))
        expect("[1234,4567,7890]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a list of strings`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(listOf("alpha", "beta", "gamma"))
        expect("""["alpha","beta","gamma"]""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a list of strings including null`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(listOf("alpha", "beta", null, "gamma"))
        expect("""["alpha","beta",null,"gamma"]""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a set of strings`() = runBlocking {
        // unfortunately, we don't know in what order a set iterator will return the entries, so...
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(setOf("alpha", "beta", "gamma"))
        val str = stringCoAcceptor.result
        expect(true) { str.startsWith('[') && str.endsWith(']')}
        val items = str.drop(1).dropLast(1).split(',').sorted()
        expect(3) { items.size }
        expect("\"alpha\"") { items[0] }
        expect("\"beta\"") { items[1] }
        expect("\"gamma\"") { items[2] }
    }

    @Test fun `should stringify the results of an iterator`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        stringCoAcceptor.outputJSON(list.iterator())
        expect("[1,1,2,3,5,8,13,21]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a sequence`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val list = listOf(1, 1, 2, 3, 5, 8, 13, 21)
        stringCoAcceptor.outputJSON(list.asSequence())
        expect("[1,1,2,3,5,8,13,21]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify the results of an enumeration`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val list = listOf("tahi", "rua", "toru", "wh\u0101")
        stringCoAcceptor.outputJSON(ListEnum(list))
        expect("""["tahi","rua","toru","wh\u0101"]""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a map of string to string`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val map = mapOf("tahi" to "one", "rua" to "two", "toru" to "three")
        stringCoAcceptor.outputJSON(map)
        val str = stringCoAcceptor.result
        expectJSON(str) {
            count(3)
            property("tahi", "one")
            property("rua", "two")
            property("toru", "three")
        }
    }

    @Test fun `should stringify a map of string to integer`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val map = mapOf("un" to 1, "deux" to 2, "trois" to 3, "quatre" to 4)
        stringCoAcceptor.outputJSON(map)
        val str = stringCoAcceptor.result
        expectJSON(str) {
            count(4)
            property("un", 1)
            property("deux", 2)
            property("trois", 3)
            property("quatre", 4)
        }
    }

    @Test fun `should stringify an enum`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        stringCoAcceptor.outputJSON(DummyEnum.ALPHA)
        expect("\"ALPHA\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an SQL date`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10"
        val date = java.sql.Date.valueOf(str)
        stringCoAcceptor.outputJSON(date)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an SQL time`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "00:21:22"
        val time = java.sql.Time.valueOf(str)
        stringCoAcceptor.outputJSON(time)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an SQL date-time`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10 00:21:22.0"
        val timeStamp = java.sql.Timestamp.valueOf(str)
        stringCoAcceptor.outputJSON(timeStamp)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an Instant`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-09T14:28:51.234Z"
        val instant = Instant.parse(str)
        stringCoAcceptor.outputJSON(instant)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a LocalDate`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10"
        val localDate = LocalDate.parse(str)
        stringCoAcceptor.outputJSON(localDate)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a LocalDateTime`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10T00:09:26.123"
        val localDateTime = LocalDateTime.parse(str)
        stringCoAcceptor.outputJSON(localDateTime)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a LocalTime`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "00:09:26.123"
        val localTime = LocalTime.parse(str)
        stringCoAcceptor.outputJSON(localTime)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an OffsetTime`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "10:15:06.543+10:00"
        val offsetTime = OffsetTime.parse(str)
        stringCoAcceptor.outputJSON(offsetTime)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an OffsetDateTime`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10T10:15:06.543+10:00"
        val offsetDateTime = OffsetDateTime.parse(str)
        stringCoAcceptor.outputJSON(offsetDateTime)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a ZonedDateTime`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04-10T10:15:06.543+10:00[Australia/Sydney]"
        val zonedDateTime = ZonedDateTime.parse(str)
        stringCoAcceptor.outputJSON(zonedDateTime)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Year`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020"
        val year = Year.parse(str)
        stringCoAcceptor.outputJSON(year)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a YearMonth`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "2020-04"
        val yearMonth = YearMonth.parse(str)
        stringCoAcceptor.outputJSON(yearMonth)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a MonthDay`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "--04-23"
        val monthDay = MonthDay.parse(str)
        stringCoAcceptor.outputJSON(monthDay)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Duration`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "PT2H"
        val duration = Duration.parse(str)
        stringCoAcceptor.outputJSON(duration)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Period`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "P3M"
        val period = Period.parse(str)
        stringCoAcceptor.outputJSON(period)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a URI`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "http://pwall.net"
        val uri = URI(str)
        stringCoAcceptor.outputJSON(uri)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a URL`() {
        val str = "http://pwall.net"
        val url = URL(str)
        runBlocking {
            val stringCoAcceptor = StringCoAcceptor()
            stringCoAcceptor.outputJSON(url)
            expect("\"$str\"") { stringCoAcceptor.result }
        }
    }

    @Test fun `should stringify a UUID`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val str = "e24b6740-7ac3-11ea-9e47-37640adfe63a"
        val uuid = UUID.fromString(str)
        stringCoAcceptor.outputJSON(uuid)
        expect("\"$str\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Calendar`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT")).apply {
            set(Calendar.YEAR, 2019)
            set(Calendar.MONTH, 3)
            set(Calendar.DAY_OF_MONTH, 25)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 52)
            set(Calendar.SECOND, 47)
            set(Calendar.MILLISECOND, 123)
            set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        }
        stringCoAcceptor.outputJSON(cal)
        expect("\"2019-04-25T18:52:47.123+10:00\"") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a Date`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2019)
            set(Calendar.MONTH, 3)
            set(Calendar.DAY_OF_MONTH, 25)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 52)
            set(Calendar.SECOND, 47)
            set(Calendar.MILLISECOND, 123)
            set(Calendar.ZONE_OFFSET, 10 * 60 * 60 * 1000)
        }
        val date = cal.time
        stringCoAcceptor.outputJSON(date)
        // NOTE - Java implementations are inconsistent - some will normalise the time to UTC
        // while others preserve the time zone as supplied.  The test below allows for either.
        val expected1 = "\"2019-04-25T18:52:47.123+10:00\""
        val expected2 = "\"2019-04-25T08:52:47.123Z\""
        val result = stringCoAcceptor.result
        expect(true) { result == expected1 || result == expected2 }
    }

    @Test fun `should stringify a BitSet`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val bitSet = BitSet().apply {
            set(1)
            set(3)
        }
        stringCoAcceptor.outputJSON(bitSet)
        expect("[1,3]") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a simple object`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val dummy1 = Dummy1("abcdef", 98765)
        stringCoAcceptor.outputJSON(dummy1)
        expect("""{"field1":"abcdef","field2":98765}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a simple object with extra property`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = "extra123"
        stringCoAcceptor.outputJSON(dummy2)
        expect("""{"field1":"abcdef","field2":98765,"extra":"extra123"}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a simple object with extra property omitting null field`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        stringCoAcceptor.outputJSON(dummy2)
        expect("""{"field1":"abcdef","field2":98765}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify a simple object with extra property including null field when config set`() =
            runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val dummy2 = Dummy2("abcdef", 98765)
        dummy2.extra = null
        val config = JSONConfig().apply {
            includeNulls = true
        }
        stringCoAcceptor.outputJSON(dummy2, config)
        expect("""{"field1":"abcdef","field2":98765,"extra":null}""") { stringCoAcceptor.result }
    }

    @Test fun `should stringify an object of a derived class`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = Derived()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        obj.field3 = 0.012
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            property("field1", "qwerty")
            property("field2", 98765)
            property("field3", BigDecimal("0.012"))
        }
    }

    @Test fun `should stringify an object using name annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithNameAnnotation()
        obj.field1 = "qwerty"
        obj.field2 = 98765
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("field1", "qwerty")
            property("fieldX", 98765)
        }
    }

    @Test fun `should stringify an object using parameter name annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithParamNameAnnotation("abc", 123)
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify an object using custom parameter name annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithCustomNameAnnotation("abc", 123)
        val config = JSONConfig().apply {
            addNameAnnotation(CustomName::class, "symbol")
        }
        stringCoAcceptor.outputJSON(obj, config)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("field1", "abc")
            property("fieldX", 123)
        }
    }

    @Test fun `should stringify a nested object`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj1 = Dummy1("asdfg", 987)
        val obj3 = Dummy3(obj1, "what?")
        stringCoAcceptor.outputJSON(obj3)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("text", "what?")
            property("dummy1") {
                count(2)
                property("field1", "asdfg")
                property("field2", 987)
            }
        }
    }

    @Test fun `should stringify an object with @JSONIgnore`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithIgnore("alpha", "beta", "gamma")
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom ignore annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithCustomIgnore("alpha", "beta", "gamma")
        val config = JSONConfig().apply {
            addIgnoreAnnotation(CustomIgnore::class)
        }
        stringCoAcceptor.outputJSON(obj, config)
        expectJSON(stringCoAcceptor.result) {
            count(2)
            property("field1", "alpha")
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeIfNull`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithIncludeIfNull("alpha", null, "gamma")
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include if null annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithCustomIncludeIfNull("alpha", null, "gamma")
        val config = JSONConfig().apply {
            addIncludeIfNullAnnotation(CustomIncludeIfNull::class)
        }
        stringCoAcceptor.outputJSON(obj, config)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with @JSONIncludeAllProperties`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithIncludeAllProperties("alpha", null, "gamma")
        stringCoAcceptor.outputJSON(obj)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify an object with custom include all properties annotation`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val obj = DummyWithCustomIncludeAllProperties("alpha", null, "gamma")
        val config = JSONConfig().apply {
            addIncludeAllPropertiesAnnotation(CustomIncludeAllProperties::class)
        }
        stringCoAcceptor.outputJSON(obj, config)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            property("field1", "alpha")
            property("field2", null)
            property("field3", "gamma")
        }
    }

    @Test fun `should stringify a sequence of objects coming from a Channel`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val channel = Channel<Dummy1>()
        val job = launch {
            channel.send(Dummy1("first", 123))
            channel.send(Dummy1("second", 456))
            channel.send(Dummy1("third", 789))
            channel.close()
        }
        stringCoAcceptor.outputJSON(channel)
        expectJSON(stringCoAcceptor.result) {
            count(3)
            item(0) {
                property("field1", "first")
                property("field2", 123)
            }
            item(1) {
                property("field1", "second")
                property("field2", 456)
            }
            item(2) {
                property("field1", "third")
                property("field2", 789)
            }
        }
        job.join()
    }

    @Test fun `should fail on use of circular reference`() = runBlocking {
        val stringCoAcceptor = StringCoAcceptor()
        val circular1 = Circular1()
        val circular2 = Circular2()
        circular1.ref = circular2
        circular2.ref = circular1
        val exception = assertFailsWith<JSONException> {
            stringCoAcceptor.outputJSON(circular1)
        }
        expect("Circular reference: field ref in Circular2") { exception.message }
    }

}
