package com.example.reader;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
public class RetrofitClient {
    private static final String BASE_URL = "https://bedbug-ultimate-katydid.ngrok-free.app";  // Change to your server URL

    private static Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(SimpleXmlConverterFactory.create()) // XML parser
            .build();

    public static ApiService getApiService() {
        return retrofit.create(ApiService.class);
    }
}
