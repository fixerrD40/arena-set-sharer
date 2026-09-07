package com.example.arena_set_sharer.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration(proxyBeanMethods = false)
class WebMvcConfiguration {

    /**
     * Electron uses Origin app://localhost; browser ng serve uses localhost:4200;
     * the deployed SPA is same-origin with APP_HOST and does not need CORS.
     */
    @Bean
    fun corsConfigurationSource(
        @Value("\${app.base-url}") baseUrl: String
    ): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf(
                baseUrl.trimEnd('/'),
                "http://localhost:*",
                "https://localhost:*",
                "app://localhost",
                "capacitor://localhost",
                "ionic://localhost"
            )
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Configuration
    class WebConfig(
        private val objectMapper: ObjectMapper
    ) : WebMvcConfigurer {
        override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
            converters.add(0, MappingJackson2HttpMessageConverter(objectMapper))
        }
    }
}
