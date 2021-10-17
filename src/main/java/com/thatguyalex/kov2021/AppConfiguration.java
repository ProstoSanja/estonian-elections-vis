package com.thatguyalex.kov2021;

import com.thatguyalex.kov2021.application.DataProcessor;
import com.thatguyalex.kov2021.infrastructure.DataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfiguration {

    @Bean
    DataProvider dataProvider() {
        return new DataProvider();
    }

    @Bean
    DataProcessor dataProcessor(DataProvider dataProvider) {
        return new DataProcessor(dataProvider);
    }

}
