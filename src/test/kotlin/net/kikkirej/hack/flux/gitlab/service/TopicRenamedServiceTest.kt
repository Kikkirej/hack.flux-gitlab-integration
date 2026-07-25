package net.kikkirej.hack.flux.gitlab.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kikkirej.hack.flux.gitlab.dto.TopicRenamedDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitlabGroupService
import org.gitlab4j.api.models.Group
import org.junit.jupiter.api.Test

class TopicRenamedServiceTest {

	private val gitlabGroupService = mockk<GitlabGroupService>()
	private val service = TopicRenamedService(gitlabGroupService)

	@Test
	fun `skips when the group does not exist`() {
		every { gitlabGroupService.findGroup("my-topic") } returns null

		service.handle(TopicRenamedDto("my-topic", "New Name"))

		verify(exactly = 0) { gitlabGroupService.renameGroup(any(), any()) }
	}

	@Test
	fun `renames the group when it exists`() {
		val group = Group().withId(1L)
		every { gitlabGroupService.findGroup("my-topic") } returns group
		every { gitlabGroupService.renameGroup(group, "New Name") } returns Unit

		service.handle(TopicRenamedDto("my-topic", "New Name"))

		verify { gitlabGroupService.renameGroup(group, "New Name") }
	}
}
