package net.kikkirej.hack.flux.gitlab.service

import net.kikkirej.hack.flux.gitlab.dto.UserRemovedFromTopicDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitLabGroupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserRemovedFromTopicService(
	private val gitlabGroupService: GitLabGroupService,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	fun handle(dto: UserRemovedFromTopicDto) {
		val gitlabUsername = dto.gitlabUsername
		if (gitlabUsername == null) {
			logger.info("skipping user-removed for {}: no gitlab username", dto.technicalId)
			return
		}

		if (!gitlabGroupService.userExists(gitlabUsername)) {
			logger.warn("skipping user-removed for {}: gitlab user {} does not exist", dto.technicalId, gitlabUsername)
			return
		}

		val group = gitlabGroupService.findGroup(dto.technicalId)
		if (group == null) {
			logger.info("skipping user-removed for {}: group does not exist", dto.technicalId)
			return
		}

		gitlabGroupService.removeMember(group, gitlabUsername)
		logger.info("removed {} from {}", gitlabUsername, dto.technicalId)
	}
}
