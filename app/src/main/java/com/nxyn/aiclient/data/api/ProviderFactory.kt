package com.nxyn.aiclient.data.api

import com.nxyn.aiclient.domain.model.ProviderType

object ProviderFactory {
    fun create(type: ProviderType): AIProvider = when (type) {
        ProviderType.ANTHROPIC -> AnthropicProvider()
        ProviderType.OPENAI_COMPATIBLE -> OpenAICompatibleProvider()
    }
}
