package com.thatguyalex.rk2023

import com.thatguyalex.monitoring.infrastructure.jdbc.JsonbReadingConverter
import com.thatguyalex.monitoring.infrastructure.jdbc.JsonbWritingConverter
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ConfigurationPropertiesScan(basePackages = ["com.thatguyalex"])
@EnableJdbcRepositories(basePackages = ["com.thatguyalex.rk2023", "com.thatguyalex.monitoring"])
class ElectionVisConfiguration : AbstractJdbcConfiguration() {

    @Bean
    override fun jdbcCustomConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(listOf(
            JsonbWritingConverter(),
            JsonbReadingConverter(),
        ))
    }

    @Bean
    @Primary
    fun applicationEventMulticaster(): ApplicationEventMulticaster {
        val eventMulticaster = SimpleApplicationEventMulticaster()
        val taskExecutor = SimpleAsyncTaskExecutor()
        taskExecutor.setVirtualThreads(true)
        eventMulticaster.setTaskExecutor(taskExecutor)
        return eventMulticaster
    }
}
