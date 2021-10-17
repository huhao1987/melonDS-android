package hh.game.usrcheatreader

/**
 * Copyright (c) 2016, xperia64
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Jusrcheat nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * Change the EndianUtils to kotlin extension for easy use
 */
fun ByteArray.little2int(): Int {
    if (size != 4) {
        println("Error: Bad Int Length")
    }
    return this[3].toInt() shl 24 and -0x1000000 or (this[2].toInt() shl 16 and 0xFF0000) or (this[1].toInt() shl 8 and 0xFF00) or (this[0].toInt() and 0xFF)
}

fun Int.int2little(): ByteArray {
    return byteArrayOf(
        (this and 0xFF).toByte(),
        (this shr 8 and 0xFF).toByte(), (this shr 16 and 0xFF).toByte(), (this shr 24 and 0xFF).toByte()
    )
}

fun ByteArray.little2short(): Short {
    if (this.size != 2) {
        println("Error: Bad Short Length")
    }
    return (this[1].toInt() shl 8 and 0xFF00 or (this[0].toInt() and 0xFF)).toShort()
}

fun short2little(s: Short): ByteArray? {
    return byteArrayOf((s.toInt() and 0xFF) as Byte, (s.toInt() shr 8 and 0xFF) as Byte)
}

//TODO need to remove
// Note: &3 = %4
fun Long.alignto4(): Int {
    return (4 - (this and 3) and 3).toInt()
}

//Align the number to 4, it means the offset is 4-number
fun Int.align():Int{
    if(this%4>0)
    return ((4-this%4)+this)
    else return this
}
fun alignstr(len: Int): Int {
    return 4 - (len and 3) + len
}

fun String.str2byte(padding: Boolean): ByteArray? {
    val b: ByteArray
    b = if (padding) {
        ByteArray(alignstr(length))
    } else {
        ByteArray(length + 1)
    }
    for (i in b.indices) {
        if (i < length) {
            b[i] = this[i].toByte()
        } else {
            b[i] = 0
        }
    }
    return b
}

fun str2byte(s1: String, s2: String, padding: Boolean): ByteArray? {
    val b: ByteArray
    b = if (padding) {
        ByteArray(alignstr(s1.length + 1 + s2.length))
    } else {
        ByteArray(s1.length + 1 + s2.length + 1)
    }
    for (i in b.indices) {
        if (i < s1.length) {
            b[i] = s1[i].toByte()
        } else if (i == s1.length) {
            b[i] = 0
        } else if (i - (s1.length + 1) < s2.length) {
            b[i] = s2[i - (s1.length + 1)].toByte()
        } else {
            b[i] = 0
        }
    }
    return b
}