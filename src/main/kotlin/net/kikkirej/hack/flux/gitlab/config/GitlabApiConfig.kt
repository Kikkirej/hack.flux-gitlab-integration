package net.kikkirej.hack.flux.gitlab.config

import org.gitlab4j.api.GitLabApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GitlabApiConfig {

	@Bean
	fun gitLabApi(properties: GitlabProperties): GitLabApi =
		GitLabApi(properties.url, properties.accessToken)
}
