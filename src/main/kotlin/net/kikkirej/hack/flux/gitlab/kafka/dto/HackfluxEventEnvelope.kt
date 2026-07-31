package net.kikkirej.hack.flux.gitlab.kafka.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HackfluxEventEnvelope(
	@JsonProperty("hckflx_eventtype") val eventType: String,
	@JsonProperty("hckflx_topic") val topic: HackfluxTopicDto,
	@JsonProperty("hckflx_user") val user: HackfluxUserDto? = null,
)
