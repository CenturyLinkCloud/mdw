package com.centurylink.mdw.zipkin.tests

import brave.internal.HexCodec

class SpanId(val id: Long) {

    fun hexId(): String? {
        if (id == 0L)
            return null
        val spanRes = CharArray(16)
        HexCodec.writeHexLong(spanRes, 0, id)
        return String(spanRes)
    }

    override fun toString(): String {
        return hexId() ?: "null"
    }
}