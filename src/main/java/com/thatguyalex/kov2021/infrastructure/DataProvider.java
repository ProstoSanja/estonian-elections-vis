package com.thatguyalex.kov2021.infrastructure;

import com.thatguyalex.kov2021.infrastructure.classes.ResultData;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import retrofit2.Retrofit;
import retrofit2.converter.jaxb.JaxbConverterFactory;

@Slf4j
public class DataProvider {

    private ResultData resultData;

    private ElectionService electionService;

    //https://vis-media-api.ria.ee/KOV_2021/RESULTS.xml

    public DataProvider() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://localhost:12345")
                .addConverterFactory(JaxbConverterFactory.create())
                .build();
        electionService = retrofit.create(ElectionService.class);
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 0)
    @SneakyThrows
    private void updateDta() {
        log.info("fetching update");
        resultData = electionService.getResults().execute().body();
    }

    public ResultData getData() {
        return resultData;
    }

}
