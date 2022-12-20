package es.unizar.urlshortener.infrastructure.delivery

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Source: https://www.davideaversa.it/blog/document-kotlin-spring-application-springdoc-openapi/
 *
 */
@Configuration
class OpenApiConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .components(Components())
            .info(
                Info()
                    .title("UrlShortener API")
                    .description("Documentation for the shortener API methods")
            )
    }
}
