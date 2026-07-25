package net.kikkirej.hack.flux.gitlab.service

import net.kikkirej.hack.flux.gitlab.dto.UserAddedToTopicDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitlabGroupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class UserAddedToTopicService(
	private val gitlabGroupService: GitlabGroupService,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	fun handle(dto: UserAddedToTopicDto) {
		val gitlabUsername = dto.gitlabUsername
		if (gitlabUsername == null) {
			logger.info("skipping user-added for {}: no gitlab username", dto.technicalId)
			return
		}

		val group = gitlabGroupService.findGroup(dto.technicalId)
			?: gitlabGroupService.createGroup(dto.technicalId, dto.topicDisplayName)
				.also { logger.info("created group for topic {}", dto.technicalId) }

		gitlabGroupService.addOwner(group, gitlabUsername)
		logger.info("added {} as owner of {}", gitlabUsername, dto.technicalId)
	}
}
