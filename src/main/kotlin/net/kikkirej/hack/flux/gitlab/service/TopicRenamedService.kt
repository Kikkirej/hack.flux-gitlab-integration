package net.kikkirej.hack.flux.gitlab.service

import net.kikkirej.hack.flux.gitlab.dto.TopicRenamedDto
import net.kikkirej.hack.flux.gitlab.gitlab.GitLabGroupService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TopicRenamedService(
	private val gitlabGroupService: GitLabGroupService,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	fun handle(dto: TopicRenamedDto) {
		val group = gitlabGroupService.findGroup(dto.technicalId)
		if (group == null) {
			logger.info("skipping rename for {}: group does not exist", dto.technicalId)
			return
		}

		gitlabGroupService.renameGroup(group, dto.topicDisplayName)
		logger.info("renamed group {} to {}", dto.technicalId, dto.topicDisplayName)
	}
}
