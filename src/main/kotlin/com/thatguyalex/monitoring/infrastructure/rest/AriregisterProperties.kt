package com.thatguyalex.monitoring.infrastructure.rest

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ariregister")
data class AriregisterProperties(
    val url: String,
    val username: String,
    val password: String,
)
