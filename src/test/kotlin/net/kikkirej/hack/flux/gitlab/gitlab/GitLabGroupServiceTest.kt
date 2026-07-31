package net.kikkirej.hack.flux.gitlab.gitlab

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.kikkirej.hack.flux.gitlab.config.GitlabProperties
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.GroupApi
import org.gitlab4j.api.UserApi
import org.gitlab4j.api.models.AccessLevel
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.GroupParams
import org.gitlab4j.api.models.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GitLabGroupServiceTest {

	private val groupApi = mockk<GroupApi>()
	private val userApi = mockk<UserApi>()
	private val gitLabApi = mockk<GitLabApi> {
		every { groupApi } returns this@GitLabGroupServiceTest.groupApi
		every { userApi } returns this@GitLabGroupServiceTest.userApi
	}
	private val properties = GitlabProperties(
		url = "https://gitlab.example.com",
		accessToken = "token",
		parentGroupPath = "hackathons",
	)

	private lateinit var service: GitLabGroupService

	@BeforeEach
	fun setUp() {
		service = GitLabGroupService(gitLabApi, properties)
	}

	@Test
	fun `findGroup returns null when group does not exist`() {
		every { groupApi.getOptionalGroup("hackathons/my-topic") } returns Optional.empty()

		assertNull(service.findGroup("my-topic"))
	}

	@Test
	fun `findGroup returns the group when it exists`() {
		val group = Group().withId(1L)
		every { groupApi.getOptionalGroup("hackathons/my-topic") } returns Optional.of(group)

		assertSame(group, service.findGroup("my-topic"))
	}

	@Test
	fun `createGroup resolves the parent group id and creates a subgroup`() {
		val parent = Group().withId(42L)
		val created = Group().withId(99L)
		every { groupApi.getGroup("hackathons") } returns parent
		every { groupApi.createGroup(match<GroupParams> { true }) } returns created

		val result = service.createGroup("my-topic", "My Topic")

		assertSame(created, result)
		verify {
			groupApi.createGroup(withArg<GroupParams> { params ->
				val formValues = params.getForm(true).formValues
				assertEquals(42L, formValues["parent_id"]?.value)
				assertEquals("My Topic", formValues["name"]?.value)
				assertEquals("my-topic", formValues["path"]?.value)
			})
		}
	}

	@Test
	fun `createGroup truncates a name longer than 127 characters`() {
		val parent = Group().withId(42L)
		val created = Group().withId(99L)
		val longName = "a".repeat(200)
		every { groupApi.getGroup("hackathons") } returns parent
		every { groupApi.createGroup(match<GroupParams> { true }) } returns created

		service.createGroup("my-topic", longName)

		verify {
			groupApi.createGroup(withArg<GroupParams> { params ->
				assertEquals("a".repeat(127), params.getForm(true).formValues["name"]?.value)
			})
		}
	}

	@Test
	fun `renameGroup updates only the group name`() {
		val group = Group().withId(7L)
		every { groupApi.updateGroup(7L, any<GroupParams>()) } returns group

		service.renameGroup(group, "New Name")

		verify {
			groupApi.updateGroup(7L, withArg<GroupParams> { params ->
				assertEquals("New Name", params.getForm(false).formValues["name"]?.value)
			})
		}
	}

	@Test
	fun `renameGroup truncates a name longer than 127 characters`() {
		val group = Group().withId(7L)
		val longName = "b".repeat(200)
		every { groupApi.updateGroup(7L, any<GroupParams>()) } returns group

		service.renameGroup(group, longName)

		verify {
			groupApi.updateGroup(7L, withArg<GroupParams> { params ->
				assertEquals("b".repeat(127), params.getForm(false).formValues["name"]?.value)
			})
		}
	}

	@Test
	fun `userExists returns true when the gitlab user exists`() {
		val user = User().withId(13L)
		every { userApi.getOptionalUser("alice") } returns Optional.of(user)

		assertTrue(service.userExists("alice"))
	}

	@Test
	fun `userExists returns false when the gitlab user does not exist`() {
		every { userApi.getOptionalUser("ghost") } returns Optional.empty()

		assertFalse(service.userExists("ghost"))
	}

	@Test
	fun `addOwner resolves the user and adds them as owner`() {
		val group = Group().withId(7L)
		val user = User().withId(13L)
		every { userApi.getUser("alice") } returns user
		every { groupApi.addMember(7L, 13L, AccessLevel.OWNER) } returns mockk()

		service.addOwner(group, "alice")

		verify { groupApi.addMember(7L, 13L, AccessLevel.OWNER) }
	}

	@Test
	fun `removeMember resolves the user and removes them from the group`() {
		val group = Group().withId(7L)
		val user = User().withId(13L)
		every { userApi.getUser("alice") } returns user
		every { groupApi.removeMember(7L, 13L) } returns Unit

		service.removeMember(group, "alice")

		verify { groupApi.removeMember(7L, 13L) }
	}
}
