package net.kikkirej.hack.flux.gitlab.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitLabGroupService
import org.gitlab4j.api.models.Group
import org.junit.jupiter.api.Test

class UserAddedToTopicServiceTest {

	private val gitlabGroupService = mockk<GitLabGroupService>()
	private val service = UserAddedToTopicService(gitlabGroupService)

	@Test
	fun `skips entirely when gitlab username is missing`() {
		service.handle(UserAddedToTopicDto("my-topic", "My Topic", null))

		verify(exactly = 0) { gitlabGroupService.findGroup(any()) }
		verify(exactly = 0) { gitlabGroupService.createGroup(any(), any()) }
		verify(exactly = 0) { gitlabGroupService.addOwner(any(), any()) }
	}

	@Test
	fun `skips entirely when gitlab user does not exist`() {
		every { gitlabGroupService.userExists("alice") } returns false

		service.handle(UserAddedToTopicDto("my-topic", "My Topic", "alice"))

		verify(exactly = 0) { gitlabGroupService.findGroup(any()) }
		verify(exactly = 0) { gitlabGroupService.createGroup(any(), any()) }
		verify(exactly = 0) { gitlabGroupService.addOwner(any(), any()) }
	}

	@Test
	fun `adds owner to the existing group without creating it`() {
		val group = Group().withId(1L)
		every { gitlabGroupService.userExists("alice") } returns true
		every { gitlabGroupService.findGroup("my-topic") } returns group
		every { gitlabGroupService.addOwner(group, "alice") } returns Unit

		service.handle(UserAddedToTopicDto("my-topic", "My Topic", "alice"))

		verify(exactly = 0) { gitlabGroupService.createGroup(any(), any()) }
		verify { gitlabGroupService.addOwner(group, "alice") }
	}

	@Test
	fun `creates the group when missing and then adds owner`() {
		val group = Group().withId(1L)
		every { gitlabGroupService.userExists("alice") } returns true
		every { gitlabGroupService.findGroup("my-topic") } returns null
		every { gitlabGroupService.createGroup("my-topic", "My Topic") } returns group
		every { gitlabGroupService.addOwner(group, "alice") } returns Unit

		service.handle(UserAddedToTopicDto("my-topic", "My Topic", "alice"))

		verify { gitlabGroupService.createGroup("my-topic", "My Topic") }
		verify { gitlabGroupService.addOwner(group, "alice") }
	}
}
