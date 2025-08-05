package com.demo.formapp;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String NETWORK_IP = "http://192.1.170.64:";
    private static final String LLM_API = NETWORK_IP + "5000/";
    private static final String PADDLE_API = NETWORK_IP + "8866/";

    private static Retrofit retrofit;

    public static Retrofit getClient(String model_url) {
        if(model_url.equalsIgnoreCase("llm")){
            model_url = LLM_API;
        } else {
            model_url = PADDLE_API;
        }
        if (retrofit == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor()
                    .setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .connectTimeout(120, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(120, TimeUnit.SECONDS)
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(model_url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
