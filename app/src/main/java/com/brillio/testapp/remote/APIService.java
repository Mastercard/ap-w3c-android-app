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

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import com.brillio.testapp.util.LogUtil;

import com.brillio.testapp.R;
import com.brillio.testapp.util.PrefsHelper;
import com.brillio.testapp.util.SecurityService;
import com.mastercard.w3c.address.ShippingAddress;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class APIService {
    private static final String logTag = APIService.class.getSimpleName();

    public static String getEncryptData(String merchantCertificate, String paymentResponse, final Context appContext) {

        String base64Response = null;

        callEncryptAPI(merchantCertificate, paymentResponse, appContext);
        try {
            String resp = PrefsHelper.getencryptedPaymentResponse(appContext);
            if (!resp.isEmpty()) {
                JSONObject det = new JSONObject(resp);
                base64Response = det.getString("paymentResponse");
            } else {
                base64Response = SecurityService.encypt(merchantCertificate, paymentResponse);
            }
        } catch (JSONException e) {
            LogUtil.error(logTag,"JSONException in getEncryptData :" + e.getMessage());
        }
        return base64Response;
    }

    private static void callEncryptAPI(String merchantCertificate, String paymentResponse, final Context appContext) {
        String base_api_url = appContext.getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();
        Log.v("Payment Response", paymentResponse);
        try {
            reqjson.put("merchantCertificate", merchantCertificate);
            reqjson.put("paymentResponse", paymentResponse);
        } catch (JSONException e) {
            LogUtil.error(logTag,"JSONException in callEncryptAPI :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);

        retrofitInterface.getEncryptedResponse(body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                try {
                    Log.v("Encrypted", response.body().string());
                    PrefsHelper.setencryptedPaymentResponse(response.body().string(), appContext);
                } catch (IOException e) {
                    LogUtil.error(logTag,"IOException in callEncryptAPI :" + e.getMessage());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
            }
        });
    }


    public static JSONObject getShippingAddress(ShippingAddress shippingAddress) {
        if (shippingAddress != null) {
            JSONObject jsonBillingObj = new JSONObject();
            try {
                jsonBillingObj.put("recipient", shippingAddress.getRecipient());
                jsonBillingObj.put("organization", shippingAddress.getOrganization());
                jsonBillingObj.put("country", shippingAddress.getCountry());
                jsonBillingObj.put("region", shippingAddress.getRegion());
                jsonBillingObj.put("city", shippingAddress.getCity());
                jsonBillingObj.put("postalCode", shippingAddress.getPostalCode());
                jsonBillingObj.put("addressLine", shippingAddress.getAddressLine());
                jsonBillingObj.put("phone", shippingAddress.getPhone());
                jsonBillingObj.put("sortingCode", shippingAddress.getSortingCode());
                jsonBillingObj.put("dependentLocality", shippingAddress.getDependentLocality());

            } catch (JSONException e) {
                LogUtil.error(logTag,"JSONException in getShippingAddress :" + e.getMessage());
            }
            return jsonBillingObj;
        } else {
            return null;
        }
    }


    public static JSONObject getBillingAddress(String payerPhone, String payerName) {
        JSONObject jsonBillingObj = new JSONObject();
        try {
            jsonBillingObj.put("addressLine", "JP Nagar");
            jsonBillingObj.put("city", "Bangalore");
            jsonBillingObj.put("country", "IN");
            jsonBillingObj.put("dependentLocality", "");
            jsonBillingObj.put("organization", "");
            jsonBillingObj.put("phone", payerPhone);
            jsonBillingObj.put("postalCode", "560078");
            jsonBillingObj.put("recipient", payerName);
            jsonBillingObj.put("region", "Karnataka");
            jsonBillingObj.put("sortingCode", "");
        } catch (JSONException e) {
            LogUtil.error(logTag,"JSONException in getBillingAddress :" + e.getMessage());
        }
        return jsonBillingObj;
    }


    public static void getAPIPaymentResponse(String paymentMethod, String walletID, final Context appContext) {
        String base_api_url = appContext.getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();
        try {
            reqjson.put("paymentType", paymentMethod);
            reqjson.put("walletID", walletID);
        } catch (JSONException e) {
            LogUtil.error(logTag,"JSONException in getAPIPaymentResponse :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);
        retrofitInterface.getPaymentResponse(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                    PrefsHelper.setPaymentResonseDetails(response.body().string(), appContext);
                } catch (IOException e) {
                    LogUtil.error(logTag,"IOException in getAPIPaymentResponse :" + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
            }
        });
    }
}
