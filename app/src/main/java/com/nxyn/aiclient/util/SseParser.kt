package com.nxyn.aiclient.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.BufferedSource

object SseParser {
    fun parse(source: BufferedSource): Flow<String> = flow {
        while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            if (line.startsWith("data:")) {
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isNotEmpty()) emit(data)
            }
        }
    }
}
