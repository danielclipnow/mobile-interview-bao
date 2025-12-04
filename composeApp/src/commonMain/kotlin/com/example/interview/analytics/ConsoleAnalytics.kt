package com.example.interview.analytics

class ConsoleAnalytics : Analytics {
    override fun trackEvent(name: String, properties: Map<String, Any>) {
        val propertiesStr = if (properties.isEmpty()) {
            ""
        } else {
            " | Properties: ${properties.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        }
        println("[Analytics] Event: $name$propertiesStr")
    }
}

