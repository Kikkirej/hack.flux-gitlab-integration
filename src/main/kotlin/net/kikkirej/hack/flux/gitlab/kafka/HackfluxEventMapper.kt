package net.kikkirej.hack.flux.gitlab.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import net.kikkirej.hack.flux.gitlab.config.HackfluxProperties
import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.dto.UserRemovedFromTopicDto
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxEventEnvelope
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxUserDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HackfluxEventMapper(
	private val properties: HackfluxProperties,
) {

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
			gitlabUsername = resolveGitlabUsername(envelope.user),
		)

	fun toUserRemovedDto(envelope: HackfluxEventEnvelope): UserRemovedFromTopicDto =
		UserRemovedFromTopicDto(
			technicalId = envelope.topic.uuid,
			gitlabUsername = resolveGitlabUsername(envelope.user),
		)

	private fun resolveGitlabUsername(user: HackfluxUserDto?): String? =
		if (properties.gitlabUsernameFromOidcRef) {
			user?.oidcRef
				?.takeIf { it.contains('@') }
				?.substringBefore('@')
				?.takeIf { it.isNotBlank() }
		} else {
			user?.customFields?.get("gitlab_user")
		}
}
