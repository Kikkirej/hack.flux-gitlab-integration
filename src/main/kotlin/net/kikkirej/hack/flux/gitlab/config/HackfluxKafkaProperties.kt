package net.kikkirej.hack.flux.gitlab.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hackflux.kafka")
data class HackfluxKafkaProperties(
	val topic: String? = null,
)
