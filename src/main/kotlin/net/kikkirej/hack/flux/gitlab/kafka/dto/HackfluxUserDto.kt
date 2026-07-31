package net.kikkirej.hack.flux.gitlab.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HackfluxUserDto(
	@JsonProperty("custom_fields") val customFields: Map<String, String>? = null,
	@JsonProperty("oidcref") val oidcRef: String? = null,
)
