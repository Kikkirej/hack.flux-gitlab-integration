package net.kikkirej.hack.flux.gitlab.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "hackflux")
data class HackfluxProperties(
	val gitlabUsernameFromOidcRef: Boolean = true,
)
