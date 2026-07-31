package net.kikkirej.hack.flux.gitlab.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.dto.UserRemovedFromTopicDto
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxEventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HackfluxEventMapper {

	private val logger = LoggerFactory.getLogger(javaClass)
	private val objectMapper = ObjectMapper()

	fun parseEnvelope(rawJson: String): HackfluxEventEnvelope? =
		try {
			objectMapper.readValue(rawJson, HackfluxEventEnvelope::class.java)
		} catch (ex: Exception) {
			logger.warn("could not parse hackflux event payload: {}", rawJson, ex)
			null
		}

	fun toUserAddedDto(envelope: HackfluxEventEnvelope): UserAddedToTopicDto =
		UserAddedToTopicDto(
			technicalId = envelope.topic.uuid,
			topicDisplayName = envelope.topic.friendlyName,
			gitlabUsername = envelope.user?.customFields?.get("gitlab_user"),
		)

	fun toUserRemovedDto(envelope: HackfluxEventEnvelope): UserRemovedFromTopicDto =
		UserRemovedFromTopicDto(
			technicalId = envelope.topic.uuid,
			gitlabUsername = envelope.user?.customFields?.get("gitlab_user"),
		)
}
