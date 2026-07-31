package net.kikkirej.hack.flux.gitlab.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kikkirej.hack.flux.gitlab.dto.UserRemovedFromTopicDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitlabGroupService
import org.gitlab4j.api.models.Group
import org.junit.jupiter.api.Test

class UserRemovedFromTopicServiceTest {

	private val gitlabGroupService = mockk<GitlabGroupService>()
	private val service = UserRemovedFromTopicService(gitlabGroupService)

	@Test
	fun `skips entirely when gitlab username is missing`() {
		service.handle(UserRemovedFromTopicDto("my-topic", null))

		verify(exactly = 0) { gitlabGroupService.findGroup(any()) }
		verify(exactly = 0) { gitlabGroupService.removeMember(any(), any()) }
	}

	@Test
	fun `skips entirely when gitlab user does not exist`() {
		every { gitlabGroupService.userExists("alice") } returns false

		service.handle(UserRemovedFromTopicDto("my-topic", "alice"))

		verify(exactly = 0) { gitlabGroupService.findGroup(any()) }
		verify(exactly = 0) { gitlabGroupService.removeMember(any(), any()) }
	}

	@Test
	fun `skips when the group does not exist`() {
		every { gitlabGroupService.userExists("alice") } returns true
		every { gitlabGroupService.findGroup("my-topic") } returns null

		service.handle(UserRemovedFromTopicDto("my-topic", "alice"))

		verify(exactly = 0) { gitlabGroupService.removeMember(any(), any()) }
	}

	@Test
	fun `removes the member from the existing group`() {
		val group = Group().withId(1L)
		every { gitlabGroupService.userExists("alice") } returns true
		every { gitlabGroupService.findGroup("my-topic") } returns group
		every { gitlabGroupService.removeMember(group, "alice") } returns Unit

		service.handle(UserRemovedFromTopicDto("my-topic", "alice"))

		verify { gitlabGroupService.removeMember(group, "alice") }
	}
}
