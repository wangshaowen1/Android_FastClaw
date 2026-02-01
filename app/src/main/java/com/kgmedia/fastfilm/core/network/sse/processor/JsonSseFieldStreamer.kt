package com.kgmedia.fastfilm.core.network.sse.processor

import kotlin.text.iterator

class JsonSseFieldStreamer(
    private val onTitleUpdate: (String) -> Unit,
    private val onMdUpdate: (String) -> Unit
) {

    private enum class State {
        SEEK_KEY,
        IN_KEY,
        SEEK_COLON,
        SEEK_VALUE,
        IN_VALUE,
        SKIP_VALUE
    }

    private enum class Target { NONE, TITLE, MD }

    private var state = State.SEEK_KEY
    private var target = Target.NONE

    private val keyBuf = StringBuilder(32)
    private val valueBuf = StringBuilder(1024)

    private var escape = false
    private var braceLevel = 0

    // Markdown 节流
    private var lastMdEmitTime = 0L
    private val MD_THROTTLE_MS = 80L

    fun onChunk(chunk: String) {
        for (c in chunk) {
            feed(c)
        }
    }

    private fun feed(c: Char) {
        // 维护 JSON 层级（只在非字符串状态下）
        if (state != State.IN_VALUE && state != State.IN_KEY) {
            when (c) {
                '{' -> braceLevel++
                '}' -> braceLevel--
            }
        }

        when (state) {

            State.SEEK_KEY -> {
                if (c == '"' && braceLevel == 1) {
                    keyBuf.setLength(0)
                    escape = false
                    state = State.IN_KEY
                }
            }

            State.IN_KEY -> {
                if (escape) {
                    keyBuf.append(c)
                    escape = false
                } else when (c) {
                    '\\' -> escape = true
                    '"' -> state = State.SEEK_COLON
                    else -> keyBuf.append(c)
                }
            }

            State.SEEK_COLON -> {
                if (c == ':') state = State.SEEK_VALUE
            }

            State.SEEK_VALUE -> {
                when {
                    c == '"' -> {
                        target = when (keyBuf.toString()) {
                            "episode_title" -> Target.TITLE
                            "episode_md" -> Target.MD
                            else -> Target.NONE
                        }
                        valueBuf.setLength(0)
                        escape = false
                        state = State.IN_VALUE
                    }
                    !c.isWhitespace() -> {
                        target = Target.NONE
                        state = State.SKIP_VALUE
                    }
                }
            }

            State.IN_VALUE -> {
                if (escape) {
                    when (c) {
                        'n' -> valueBuf.append('\n')
                        't' -> valueBuf.append('\t')
                        'r' -> valueBuf.append('\r')
                        '\\' -> valueBuf.append('\\')
                        '"' -> valueBuf.append('"')
                        else -> {
                            valueBuf.append('\\')
                            valueBuf.append(c)
                        }
                    }
                    escape = false
                    emit(false)
                    return
                }

                when (c) {
                    '\\' -> escape = true
                    '"' -> {
                        emit(true)
                        target = Target.NONE
                        state = State.SEEK_KEY
                    }
                    else -> {
                        valueBuf.append(c)
                        emit(false)
                    }
                }
            }

            State.SKIP_VALUE -> {
                if (c == ',' || c == '}') {
                    state = State.SEEK_KEY
                }
            }
        }
    }

    private fun emit(isFinal: Boolean) {
        when (target) {
            Target.TITLE -> {
                if (isFinal) {
                    onTitleUpdate(valueBuf.toString())
                }
            }
            Target.MD -> {
                val now = System.currentTimeMillis()
                if (isFinal || now - lastMdEmitTime >= MD_THROTTLE_MS) {
                    onMdUpdate(valueBuf.toString())
                    lastMdEmitTime = now
                }
            }
            else -> Unit
        }

        if (isFinal) {
            valueBuf.setLength(0)
        }
    }
}