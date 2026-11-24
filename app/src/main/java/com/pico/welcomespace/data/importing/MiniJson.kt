package com.pico.welcomespace.data.importing

/**
 * 纯 Kotlin 最小 JSON 解析器（无第三方依赖，§13 决策 1 派生）。
 * 仅服务于 glTF JSON 的结构化读取：
 * 返回 Map<String, Any?> / List<Any?> / String / Double / Boolean / null。
 */
internal object MiniJson {

    /** 继承 ModelParseException，Pipeline 统一归类为 PARSE_FAILED（§8.1 格式类）。 */
    class JsonParseException(message: String) : ModelParseException(message)

    fun parse(text: String): Any? {
        val reader = ValueReader(text)
        val value = reader.readValue()
        reader.skipWhitespace()
        return value
    }

    private class ValueReader(private val text: String) {
        var pos = 0

        fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        fun readValue(): Any? {
            skipWhitespace()
            if (pos >= text.length) throw JsonParseException("意外的结尾")
            return when (val c = text[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> readString()
                't' -> readLiteral("true", true)
                'f' -> readLiteral("false", false)
                'n' -> readLiteral("null", null)
                else -> {
                    if (c == '-' || c.isDigit()) readNumber()
                    else throw JsonParseException("非法字符 '$c' @ $pos")
                }
            }
        }

        private fun readLiteral(word: String, value: Any?): Any? {
            if (!text.startsWith(word, pos)) throw JsonParseException("非法字面量 @ $pos")
            pos += word.length
            return value
        }

        private fun readObject(): LinkedHashMap<String, Any?> {
            val result = LinkedHashMap<String, Any?>()
            pos++ // '{'
            skipWhitespace()
            if (pos < text.length && text[pos] == '}') {
                pos++
                return result
            }
            while (true) {
                skipWhitespace()
                if (pos >= text.length || text[pos] != '"') throw JsonParseException("对象键缺失 @ $pos")
                val key = readString()
                skipWhitespace()
                if (pos >= text.length || text[pos] != ':') throw JsonParseException("缺少冒号 @ $pos")
                pos++
                result[key] = readValue()
                skipWhitespace()
                if (pos >= text.length) throw JsonParseException("对象未闭合")
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return result
                    }
                    else -> throw JsonParseException("对象分隔符非法 @ $pos")
                }
            }
        }

        private fun readArray(): ArrayList<Any?> {
            val result = ArrayList<Any?>()
            pos++ // '['
            skipWhitespace()
            if (pos < text.length && text[pos] == ']') {
                pos++
                return result
            }
            while (true) {
                result.add(readValue())
                skipWhitespace()
                if (pos >= text.length) throw JsonParseException("数组未闭合")
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return result
                    }
                    else -> throw JsonParseException("数组分隔符非法 @ $pos")
                }
            }
        }

        private fun readString(): String {
            pos++ // '"'
            val builder = StringBuilder()
            while (pos < text.length) {
                val c = text[pos]
                when {
                    c == '"' -> {
                        pos++
                        return builder.toString()
                    }
                    c == '\\' -> {
                        pos++
                        if (pos >= text.length) throw JsonParseException("转义符未闭合")
                        when (val e = text[pos]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            // form feed 用 0x0C 构造，避免源码中出现控制字符
                            'f' -> builder.append(0x0C.toChar())
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (pos + 4 >= text.length) throw JsonParseException("unicode 转义不完整")
                                val hex = text.substring(pos + 1, pos + 5)
                                val code = hex.toIntOrNull(16)
                                    ?: throw JsonParseException("unicode 转义非法: $hex")
                                builder.append(code.toChar())
                                pos += 4
                            }
                            else -> throw JsonParseException("非法转义 $e @ $pos")
                        }
                        pos++
                    }
                    else -> {
                        builder.append(c)
                        pos++
                    }
                }
            }
            throw JsonParseException("字符串未闭合")
        }

        private fun readNumber(): Double {
            val start = pos
            if (pos < text.length && text[pos] == '-') pos++
            while (pos < text.length && (text[pos].isDigit() || text[pos] in ".eE+-")) pos++
            val token = text.substring(start, pos)
            return token.toDoubleOrNull() ?: throw JsonParseException("数字非法: $token")
        }
    }
}
