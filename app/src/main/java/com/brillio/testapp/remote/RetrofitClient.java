/*
 * Copyright (c) 2021 Mastercard
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.brillio.testapp.remote;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public class RetrofitClient {

    private static Retrofit retrofit = null;

    public static Retrofit getClient(String base_api_url) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(base_api_url)
                    .build();
        }
        return retrofit;
    }

    public interface RetrofitInterface {

        /**
         * Generates the Payment response
         *
         * @param body - Payment request
         * @return - Payment response
         */
        @POST("/paymentresponse")
        Call<ResponseBody> getPaymentResponse(@Body RequestBody body);

        /**
         * Encrypts the payment response
         *
         * @param body - Payload contains the Payment Response and Merchant Certificate
         * @return - Encrypted data.
         */
        @POST("/encrypt")
        Call<ResponseBody> getEncryptedResponse(@Body RequestBody body);

        /**
         * Validates the Payment request
         *
         * @param body - Payload contains Payment Request and HashData
         * @return - Status of the validation
         */
        @POST("/validaterequest")
        Call<ResponseBody> validaterequest(@Body RequestBody body);

        /**
         * Verifies the signature and validates the Payment request
         *
         * @param signature - X-JWS-Signature
         * @param body      - Payload contains Payment Request and HashData
         * @return - Status of the validation
         */
        @POST("/verifysignhash")
        Call<ResponseBody> verifysignhash(@Header("X-JWS-Signature") String signature,
                                          @Body RequestBody body);

        /**
         * Signs the Payment response
         *
         * @param body - Payment response
         * @return - JWS-Signature
         */
        @POST("/prsign")
        Call<ResponseBody> signPaymentResponse(@Body RequestBody body);

        /**
         * Validates the merchant certificate
         *
         * @param body - Merchant Certificate URL
         * @return - Status of the validation
         */
        @POST("/validatecertificate")
        Call<ResponseBody> validatecertificate(@Body RequestBody body);

    }
}


