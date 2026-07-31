package net.kikkirej.hack.flux.gitlab.service

import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitLabGroupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserAddedToTopicService(
	private val gitlabGroupService: GitLabGroupService,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	fun handle(dto: UserAddedToTopicDto) {
		val gitlabUsername = dto.gitlabUsername
		if (gitlabUsername == null) {
			logger.info("skipping user-added for {}: no gitlab username", dto.technicalId)
			return
		}

		if (!gitlabGroupService.userExists(gitlabUsername)) {
			logger.warn("skipping user-added for {}: gitlab user {} does not exist", dto.technicalId, gitlabUsername)
			return
		}

		val group = gitlabGroupService.findGroup(dto.technicalId)
			?: gitlabGroupService.createGroup(dto.technicalId, dto.topicDisplayName)
				.also { logger.info("created group for topic {}", dto.technicalId) }

		gitlabGroupService.addOwner(group, gitlabUsername)
		logger.info("added {} as owner of {}", gitlabUsername, dto.technicalId)
	}
}
