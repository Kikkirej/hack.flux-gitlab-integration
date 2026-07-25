package net.kikkirej.hack.flux.gitlab.gitlab

import net.kikkirej.hack.flux.gitlab.config.GitlabProperties
import org.gitlab4j.api.GitLabApi
import org.gitlab4j.api.models.AccessLevel
import org.gitlab4j.api.models.Group
import org.gitlab4j.api.models.GroupParams
import org.springframework.stereotype.Service

@Service
class GitlabGroupService(
	private val gitLabApi: GitLabApi,
	private val properties: GitlabProperties,
) {

	fun findGroup(technicalId: String): Group? =
		gitLabApi.groupApi.getOptionalGroup(groupPath(technicalId)).orElse(null)

	fun createGroup(technicalId: String, displayName: String): Group {
		val parentId = gitLabApi.groupApi.getGroup(properties.parentGroupPath).id
		return gitLabApi.groupApi.createGroup(
			GroupParams()
				.withName(displayName)
				.withPath(technicalId)
				.withParentId(parentId)
		)
	}

	fun renameGroup(group: Group, displayName: String) {
		gitLabApi.groupApi.updateGroup(group.id, GroupParams().withName(displayName))
	}

	fun addOwner(group: Group, gitlabUsername: String) {
		val userId = gitLabApi.userApi.getUser(gitlabUsername).id
		gitLabApi.groupApi.addMember(group.id, userId, AccessLevel.OWNER)
	}

	fun removeMember(group: Group, gitlabUsername: String) {
		val userId = gitLabApi.userApi.getUser(gitlabUsername).id
		gitLabApi.groupApi.removeMember(group.id, userId)
	}

	private fun groupPath(technicalId: String) = "${properties.parentGroupPath}/$technicalId"
}
