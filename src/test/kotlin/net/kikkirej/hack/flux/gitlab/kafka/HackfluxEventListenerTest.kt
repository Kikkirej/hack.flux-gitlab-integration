package net.kikkirej.hack.flux.gitlab.kafka

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.dto.UserRemovedFromTopicDto
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxEventEnvelope
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxTopicDto
import net.kikkirej.hack.flux.gitlab.service.UserAddedToTopicService
import net.kikkirej.hack.flux.gitlab.service.UserRemovedFromTopicService
import org.junit.jupiter.api.Test

class HackfluxEventListenerTest {

	private val eventMapper = mockk<HackfluxEventMapper>()
	private val userAddedToTopicService = mockk<UserAddedToTopicService>()
	private val userRemovedFromTopicService = mockk<UserRemovedFromTopicService>()
	private val listener = HackfluxEventListener(eventMapper, userAddedToTopicService, userRemovedFromTopicService)

	@Test
	fun `dispatches a user_added event to UserAddedToTopicService`() {
		val envelope = HackfluxEventEnvelope("user_added", HackfluxTopicDto("my-topic", "Pizza"))
		val dto = UserAddedToTopicDto("my-topic", "Pizza", "alice")
		every { eventMapper.parseEnvelope("raw-json") } returns envelope
		every { eventMapper.toUserAddedDto(envelope) } returns dto
		every { userAddedToTopicService.handle(dto) } returns Unit

		listener.onMessage("raw-json")

		verify(exactly = 1) { userAddedToTopicService.handle(dto) }
		verify(exactly = 0) { userRemovedFromTopicService.handle(any()) }
	}

	@Test
	fun `dispatches a user_removed event to UserRemovedFromTopicService`() {
		val envelope = HackfluxEventEnvelope("user_removed", HackfluxTopicDto("my-topic", "Pizza"))
		val dto = UserRemovedFromTopicDto("my-topic", "alice")
		every { eventMapper.parseEnvelope("raw-json") } returns envelope
		every { eventMapper.toUserRemovedDto(envelope) } returns dto
		every { userRemovedFromTopicService.handle(dto) } returns Unit

		listener.onMessage("raw-json")

		verify(exactly = 1) { userRemovedFromTopicService.handle(dto) }
		verify(exactly = 0) { userAddedToTopicService.handle(any()) }
	}

	@Test
	fun `skips an unrecognized event type without invoking any service`() {
		val envelope = HackfluxEventEnvelope("topic_renamed", HackfluxTopicDto("my-topic", "Pizza"))
		every { eventMapper.parseEnvelope("raw-json") } returns envelope

		listener.onMessage("raw-json")

		verify(exactly = 0) { userAddedToTopicService.handle(any()) }
		verify(exactly = 0) { userRemovedFromTopicService.handle(any()) }
	}

	@Test
	fun `does nothing and does not throw when the payload cannot be parsed`() {
		every { eventMapper.parseEnvelope("raw-json") } returns null

		listener.onMessage("raw-json")

		verify(exactly = 0) { userAddedToTopicService.handle(any()) }
		verify(exactly = 0) { userRemovedFromTopicService.handle(any()) }
	}
}
