package net.kikkirej.hack.flux.gitlab.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gitlab")
data class GitlabProperties(
	val url: String = "https://gitlab.com",
	val accessToken: String,
	val parentGroupPath: String,
)
