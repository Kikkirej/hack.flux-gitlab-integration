package net.kikkirej.hack.flux.gitlab.kafka

import net.kikkirej.hack.flux.gitlab.service.UserAddedToTopicService
import net.kikkirej.hack.flux.gitlab.service.UserRemovedFromTopicService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "hackflux.kafka", name = ["topic"])
class HackfluxEventListener(
	private val eventMapper: HackfluxEventMapper,
	private val userAddedToTopicService: UserAddedToTopicService,
	private val userRemovedFromTopicService: UserRemovedFromTopicService,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	@KafkaListener(topics = ["\${hackflux.kafka.topic}"])
	fun onMessage(rawJson: String) {
		val envelope = eventMapper.parseEnvelope(rawJson) ?: return

		when (envelope.eventType) {
			"user_added" -> userAddedToTopicService.handle(eventMapper.toUserAddedDto(envelope))
			"user_removed" -> userRemovedFromTopicService.handle(eventMapper.toUserRemovedDto(envelope))
			else -> logger.info("skipping unrecognized hackflux event type: {}", envelope.eventType)
		}
	}
}
