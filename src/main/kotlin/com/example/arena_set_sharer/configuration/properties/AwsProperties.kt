package com.example.arena_set_sharer.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "aws")
class AwsProperties {

    var region: String = ""
    var ses: Ses = Ses()

    class Ses {
        var email: String = ""
    }
}