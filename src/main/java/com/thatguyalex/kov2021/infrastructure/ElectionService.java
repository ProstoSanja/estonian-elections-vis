package com.thatguyalex.kov2021.infrastructure;

import com.thatguyalex.kov2021.infrastructure.classes.ResultData;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ElectionService {

    @GET("KOV_2021/RESULTS.xml")
    Call<ResultData> getResults();

}
