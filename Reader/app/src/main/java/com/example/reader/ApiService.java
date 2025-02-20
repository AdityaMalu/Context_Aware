package com.example.reader;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {
    @Headers({
            "Content-Type: application/xml",
            "Accept: application/xml"
    })

    @POST("/api/identity/create") // API endpoint
    Call<CreateResponse> createID(@Body CreateRequest request);
}
