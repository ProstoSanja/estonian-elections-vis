package com.thatguyalex.rk2023

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.event.ApplicationEventMulticaster
import org.springframework.context.event.SimpleApplicationEventMulticaster
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ConfigurationPropertiesScan
class Rk2023Configuration {
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
