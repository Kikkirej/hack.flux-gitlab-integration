package net.kikkirej.hack.flux.gitlab.dto

data class UserAddedToTopicDto(
	val technicalId: String,
	val topicDisplayName: String,
	val gitlabUsername: String?,
)
