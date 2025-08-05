// OcrApi.java
package com.demo.formapp;

import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface OcrApi {
    @Multipart
    @POST("ocr")
    Call<Map<String, String>> extractKeyValues(
            @Part MultipartBody.Part image
    );
}
