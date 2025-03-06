package com.example.reader;

import org.bouncycastle.crypto.DSA;

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

    @POST("/api/healthcare/create")
    Call<CreateResponse> createHealthCareID(@Body CreateRequest request);

    @POST("/api/ticketing/create")
    Call<CreateResponse> createTicket(@Body CreateRequest request);

    @POST("/api/identity/display")
    Call<DisplayResponse> displayDetails(@Body DisplayRequest request);

    @POST("/api/healthcare/display")
    Call<DisplayResponse> displayHealthDetails(@Body DisplayRequest request);
}
