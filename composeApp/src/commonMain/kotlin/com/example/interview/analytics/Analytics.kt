package com.example.interview.analytics

interface Analytics {
    fun trackEvent(name: String, properties: Map<String, Any> = emptyMap())
}

