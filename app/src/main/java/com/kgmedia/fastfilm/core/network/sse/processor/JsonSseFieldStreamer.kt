package com.kgmedia.fastfilm.core.network.sse.processor

import com.nhsd.basic.utils.Logger

class JsonSseFieldStreamer(
    // 1. 改为可空参数，默认 null，这样不同场景可以按需传参
    private val onTitleUpdate: ((String) -> Unit)? = null,
    private val onMdUpdate: ((String) -> Unit)? = null,
    // 2. 新增 story_content 的回调
    private val onStoryUpdate: ((String) -> Unit)? = null
) {

    private enum class State {
        SEEK_KEY,
        IN_KEY,
        SEEK_COLON,
        SEEK_VALUE,
        IN_VALUE,
        SKIP_VALUE
    }

    private enum class Target { NONE, TITLE, MD, STORY }

    private var state = State.SEEK_KEY
    private var target = Target.NONE

    private val keyBuf = StringBuilder(32)
    private val valueBuf = StringBuilder(1024)

    private var escape = false
    private var braceLevel = 0

    // === 新增变量，记录上次发送的长度 ===
    private var lastMdEmittedLength = 0

    // Markdown 节流
    private var lastMdEmitTime = 0L
    private val MD_THROTTLE_MS = 80L

    fun onChunk(chunk: String) {
        for (c in chunk) {
            feed(c)
        }
    }

    private fun feed(c: Char) {
        Logger.d("JsonSseFieldStreamer", "Char: $c, State: $state, Level: $braceLevel")
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
                            "title" -> Target.TITLE
                            "md" -> Target.MD
                            "story_content" -> Target.STORY
                            else -> Target.NONE
                        }
                        valueBuf.setLength(0)
                        // === 修改点 2: 开始新字段时，重置发送游标 ===
                        lastMdEmittedLength = 0
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
                    onTitleUpdate?.invoke(valueBuf.toString())
                }
            }
            Target.MD -> {
                val now = System.currentTimeMillis()
                if ((isFinal || now - lastMdEmitTime >= MD_THROTTLE_MS) && valueBuf.length > lastMdEmittedLength) {

                    // 截取增量部分：从 lastIndex 到 currentLength
                    val delta = valueBuf.substring(lastMdEmittedLength)
                    onMdUpdate?.invoke(delta)

                    // 更新游标和时间
                    lastMdEmittedLength = valueBuf.length
                    lastMdEmitTime = now
                }
            }
            Target.STORY -> {
                // 5. 处理 Story，通常 story 也是长文本，建议同样应用节流逻辑
                val now = System.currentTimeMillis()
                if (isFinal || now - lastMdEmitTime >= MD_THROTTLE_MS) {
                    onStoryUpdate?.invoke(valueBuf.toString())
                    lastMdEmitTime = now
                }
            }
            else -> Unit
        }

        if (isFinal) {
            valueBuf.setLength(0)
        }
    }

    fun reset() {
        state = State.SEEK_KEY
        target = Target.NONE
        keyBuf.setLength(0)
        valueBuf.setLength(0)
        escape = false
        braceLevel = 0 // <--- 关键！必须归零
        lastMdEmitTime = 0L

        lastMdEmittedLength = 0
    }
}