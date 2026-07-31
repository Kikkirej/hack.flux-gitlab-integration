package net.kikkirej.hack.flux.gitlab.kafka

import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxEventEnvelope
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxTopicDto
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxUserDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HackfluxEventMapperTest {

	private val mapper = HackfluxEventMapper()

	private val userAddedJson = """
		{"hckflx_eventtype":"user_added","hckflx_topic":{"uuid":"d0306161-e912-43e7-883f-6dbbaada8fb8","friendly_name":"Pizza"},"hckflx_user":{"uuid":"not relevant","oidcref":"pizza@example.com","friendly_name":"test","custom_fields":{"gitlab_user":"user1"}}}
	""".trimIndent()

	private val userRemovedJson = """
		{"hckflx_eventtype":"user_removed","hckflx_topic":{"uuid":"d0306161-e912-43e7-883f-6dbbaada8fb8","friendly_name":"Pizza"},"hckflx_user":{"uuid":"not relevant","oidcref":"pizza@example.com","friendly_name":"test","custom_fields":{"gitlab_user":"user2","department":"cat sitting"}}}
	""".trimIndent()

	@Test
	fun `parses the user_added example payload`() {
		val envelope = mapper.parseEnvelope(userAddedJson)

		assertEquals(
			HackfluxEventEnvelope(
				eventType = "user_added",
				topic = HackfluxTopicDto("d0306161-e912-43e7-883f-6dbbaada8fb8", "Pizza"),
				user = HackfluxUserDto(mapOf("gitlab_user" to "user1")),
			),
			envelope,
		)
	}

	@Test
	fun `parses the user_removed example payload including unmapped custom fields`() {
		val envelope = mapper.parseEnvelope(userRemovedJson)

		assertEquals(
			HackfluxEventEnvelope(
				eventType = "user_removed",
				topic = HackfluxTopicDto("d0306161-e912-43e7-883f-6dbbaada8fb8", "Pizza"),
				user = HackfluxUserDto(mapOf("gitlab_user" to "user2", "department" to "cat sitting")),
			),
			envelope,
		)
	}

	@Test
	fun `returns null for malformed json`() {
		assertNull(mapper.parseEnvelope("{not valid json"))
	}

	@Test
	fun `returns null when hckflx_topic uuid is missing`() {
		val json = """{"hckflx_eventtype":"user_added","hckflx_topic":{"friendly_name":"Pizza"}}"""

		assertNull(mapper.parseEnvelope(json))
	}

	@Test
	fun `parses successfully when hckflx_user is absent`() {
		val json = """{"hckflx_eventtype":"user_added","hckflx_topic":{"uuid":"my-topic","friendly_name":"Pizza"}}"""

		val envelope = mapper.parseEnvelope(json)

		assertEquals("user_added", envelope?.eventType)
		assertNull(envelope?.user)
	}

	@Test
	fun `parses successfully with an unrecognized event type`() {
		val json = """{"hckflx_eventtype":"topic_renamed","hckflx_topic":{"uuid":"my-topic","friendly_name":"Pizza"}}"""

		val envelope = mapper.parseEnvelope(json)

		assertEquals("topic_renamed", envelope?.eventType)
	}

	@Test
	fun `maps to UserAddedToTopicDto with the gitlab username from custom fields`() {
		val envelope = mapper.parseEnvelope(userAddedJson)!!

		val dto = mapper.toUserAddedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("Pizza", dto.topicDisplayName)
		assertEquals("user1", dto.gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto with null gitlab username when custom fields has no match`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = HackfluxUserDto(mapOf("department" to "cat sitting")),
		)

		assertNull(mapper.toUserAddedDto(envelope).gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto with null gitlab username when user is absent`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = null,
		)

		assertNull(mapper.toUserAddedDto(envelope).gitlabUsername)
	}

	@Test
	fun `maps to UserRemovedFromTopicDto with the gitlab username from custom fields`() {
		val envelope = mapper.parseEnvelope(userRemovedJson)!!

		val dto = mapper.toUserRemovedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("user2", dto.gitlabUsername)
	}

	@Test
	fun `maps to UserRemovedFromTopicDto with null gitlab username when user is absent`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_removed",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = null,
		)

		assertNull(mapper.toUserRemovedDto(envelope).gitlabUsername)
	}
}
