package net.kikkirej.hack.flux.gitlab.kafka

import net.kikkirej.hack.flux.gitlab.config.HackfluxProperties
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxEventEnvelope
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxTopicDto
import net.kikkirej.hack.flux.gitlab.kafka.dto.HackfluxUserDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HackfluxEventMapperTest {

	private val mapper = HackfluxEventMapper(HackfluxProperties())
	private val mapperUsingCustomField = HackfluxEventMapper(HackfluxProperties(gitlabUsernameFromOidcRef = false))

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
				user = HackfluxUserDto(customFields = mapOf("gitlab_user" to "user1"), oidcRef = "pizza@example.com"),
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
				user = HackfluxUserDto(customFields = mapOf("gitlab_user" to "user2", "department" to "cat sitting"), oidcRef = "pizza@example.com"),
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
	fun `maps to UserAddedToTopicDto with username derived from oidcref local-part by default`() {
		val envelope = mapper.parseEnvelope(userAddedJson)!!

		val dto = mapper.toUserAddedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("Pizza", dto.topicDisplayName)
		assertEquals("pizza", dto.gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto with null gitlab username when oidcref is absent by default`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = HackfluxUserDto(oidcRef = null),
		)

		assertNull(mapper.toUserAddedDto(envelope).gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto with null gitlab username when oidcref has no at-sign by default`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = HackfluxUserDto(oidcRef = "not-an-email"),
		)

		assertNull(mapper.toUserAddedDto(envelope).gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto ignoring custom_fields when deriving from oidcref by default`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = HackfluxUserDto(customFields = mapOf("gitlab_user" to "user1"), oidcRef = "someone@else.com"),
		)

		assertEquals("someone", mapper.toUserAddedDto(envelope).gitlabUsername)
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
	fun `maps to UserAddedToTopicDto with the gitlab username from custom fields when flag is disabled`() {
		val envelope = mapperUsingCustomField.parseEnvelope(userAddedJson)!!

		val dto = mapperUsingCustomField.toUserAddedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("Pizza", dto.topicDisplayName)
		assertEquals("user1", dto.gitlabUsername)
	}

	@Test
	fun `maps to UserAddedToTopicDto with null gitlab username when custom fields has no match and flag is disabled`() {
		val envelope = HackfluxEventEnvelope(
			eventType = "user_added",
			topic = HackfluxTopicDto("my-topic", "Pizza"),
			user = HackfluxUserDto(customFields = mapOf("department" to "cat sitting"), oidcRef = "someone@else.com"),
		)

		assertNull(mapperUsingCustomField.toUserAddedDto(envelope).gitlabUsername)
	}

	@Test
	fun `maps to UserRemovedFromTopicDto with username derived from oidcref local-part by default`() {
		val envelope = mapper.parseEnvelope(userRemovedJson)!!

		val dto = mapper.toUserRemovedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("pizza", dto.gitlabUsername)
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

	@Test
	fun `maps to UserRemovedFromTopicDto with the gitlab username from custom fields when flag is disabled`() {
		val envelope = mapperUsingCustomField.parseEnvelope(userRemovedJson)!!

		val dto = mapperUsingCustomField.toUserRemovedDto(envelope)

		assertEquals("d0306161-e912-43e7-883f-6dbbaada8fb8", dto.technicalId)
		assertEquals("user2", dto.gitlabUsername)
	}
}
