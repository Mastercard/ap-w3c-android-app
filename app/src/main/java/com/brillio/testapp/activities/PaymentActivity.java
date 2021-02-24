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
package com.brillio.testapp.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brillio.testapp.BuildConfig;
import com.brillio.testapp.R;
import com.brillio.testapp.constant.AppConstants;
import com.brillio.testapp.constant.SecurityConstant;
import com.brillio.testapp.remote.APIService;
import com.brillio.testapp.remote.RetrofitClient;
import com.brillio.testapp.util.CommonUtil;
import com.brillio.testapp.util.LogUtil;
import com.brillio.testapp.util.PrefsHelper;
import com.brillio.testapp.util.SecurityService;
import com.mastercard.w3c.data.Data;
import com.mastercard.w3c.exception.ArtefactException;
import com.mastercard.w3c.paymenthandler.PaymentHandler;
import com.mastercard.w3c.paymentmethoddata.PaymentMethodData;
import com.mastercard.w3c.paymentrequest.PaymentRequestEvent;
import com.mastercard.w3c.total.Total;
import com.mastercard.w3c.util.PaymentUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

//Activity to handle the W3C Supplement Payment
public class PaymentActivity extends AppCompatActivity {

    private final String logTag = PaymentActivity.class.getSimpleName();
    private TextView tvMerchantId;
    private TextView tvMerchantName;
    private TextView tvTotalAmount;
    private PaymentRequestEvent requestObject = null;
    private PaymentHandler paymentHandler = null;
    private String selectedBrowserForPayment = "";
    private Button btProceed;
    private Button btReject;
    private ProgressBar pbPayment;

    private final String paymentMethod = "mcpba";
    private String BASE_API_URL = "";

    private JSONObject shippingAddress = null;

    private String merchantCertURL = "";
    private Boolean requestBillingAddress = false;

    private String paymentResp = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_payment);
        initViews();

        paymentHandler = PaymentHandler.getInstance();

        BASE_API_URL = getString(R.string.base_api_url);

        /**
         * Process the Incoming App Link Data (Uri)
         */
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String amount = extras.getString(AppConstants.BUNDLE_KEY_EVENT_VALUE);
            Log.v("dataObject", amount);
            selectedBrowserForPayment = extras.getString(AppConstants.BUNDLE_KEY_BROWSER_NAME);
            byte[] decodedData = null;
            try {
                decodedData = android.util.Base64.decode((amount.getBytes("UTF-8")), android.util.Base64.DEFAULT);
                Log.v("decodedData", decodedData.toString());
                String data = new String(decodedData, "UTF-8");
                //Set the request Object
                requestObject = PaymentUtil.getRequestObject(data);
            } catch (UnsupportedEncodingException e) {
                LogUtil.error(logTag, "onCreate() , exe while decoding data(Base64) :" + e.getMessage());
            }
        }
    }

    /**
     * Initializes the views
     */
    private void initViews() {
        tvMerchantId = findViewById(R.id.tv_merchant_id);
        tvMerchantName = findViewById(R.id.tv_merchant_name);
        tvTotalAmount = findViewById(R.id.total_amount);
        btProceed = findViewById(R.id.bt_confirm);
        pbPayment = findViewById(R.id.pbPayment);
        btProceed.setOnClickListener(confrmOnClickListener);
        btReject = findViewById(R.id.bt_reject);
        btReject.setOnClickListener(rejectOnClickListener);
    }

    /**
     * Process for Payment Confirmation
     */
    private View.OnClickListener confrmOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            try {
                if (CommonUtil.isNetworkAvailable(PaymentActivity.this)) {
                    btProceed.setEnabled(false);
                    btReject.setEnabled(false);
                    confirmResponse();
                } else {
                    CommonUtil.showToast(PaymentActivity.this, getResources().getString(R.string.no_network_available));
                }

            } catch (NullPointerException e) {
                LogUtil.error(logTag, "exe :" + e.getMessage());
            }
        }
    };

    /**
     * Prepares and returns the payment response which will used in respondWith() method
     *
     * @param paymentRequestEvent - Payment request object
     * @return - Returns the String format of Payment Response JSON
     */
    private View.OnClickListener rejectOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                if (CommonUtil.isNetworkAvailable(PaymentActivity.this)) {
                    btProceed.setEnabled(false);
                    btReject.setEnabled(false);
                    respondCancelResponse(getString(R.string.cancel_code), getString(R.string.cancel_message));
                } else {
                    CommonUtil.showToast(PaymentActivity.this, getResources().getString(R.string.no_network_available));
                }

            } catch (NullPointerException e) {
                LogUtil.error(logTag, "exe :" + e.getMessage());
            }
        }
    };


    /**
     * Displays Confirmation Dialog for Payment Confirmation
     */
    private void confirmResponse() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.do_u_want_confirm));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.bt_label_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                paymentResp = preparePaymentResponse(requestObject);
                Log.v("Payment Response ", paymentResp);
                if (!paymentResp.isEmpty()) {
                    callEncryptAPI();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                btProceed.setEnabled(true);
                btReject.setEnabled(true);
                dialog.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prepares and returns the payment response which will used in respondWith() method
     *
     * @param paymentRequestEvent - Payment request object
     * @return - Returns the String format of Payment Response JSON
     */
    private String preparePaymentResponse(PaymentRequestEvent paymentRequestEvent) {
        String paymentResponse = null;
        try {
            Log.v("ReqID", paymentRequestEvent.getPaymentRequestId());
            JSONObject jsonObjectParent = new JSONObject();
            ArrayList<PaymentMethodData> paymentMethodData = (ArrayList<PaymentMethodData>) paymentRequestEvent.getPaymentMethodData();
            String methodName = "";
            if (paymentMethodData != null && !paymentMethodData.isEmpty()) {
                Data data1 = paymentMethodData.get(0).getData();
                if (data1 != null) {
                    Log.v("ret", data1.getReturnURL());
                    Log.v("merret", data1.getMerchantCertificateURL());
                    Log.v("billing", String.valueOf(data1.getRequestBillingAddress()));
                    merchantCertURL = data1.getMerchantCertificateURL();
                    requestBillingAddress = data1.getRequestBillingAddress();
                }

                ArrayList<String> supportedMethods = (ArrayList<String>) paymentMethodData.get(0).getSupportedMethods();
                if (supportedMethods != null && !supportedMethods.isEmpty()) {
                    methodName = supportedMethods.get(0);
                }
            }

            Log.v("Method Name", methodName);
            jsonObjectParent.put("methodName", methodName);
            jsonObjectParent.put("requestId", paymentRequestEvent.getPaymentRequestId());
            jsonObjectParent.put("payerEmail", paymentRequestEvent.getPayerEmail());
            jsonObjectParent.put("payerName", paymentRequestEvent.getPayerName());
            jsonObjectParent.put("payerPhone", paymentRequestEvent.getPayerPhone());
            Log.v("Shipping Option", paymentRequestEvent.getshippingOption());
            jsonObjectParent.put("shippingOption", paymentRequestEvent.getshippingOption());
            pbPayment.setVisibility(View.VISIBLE);
            APIService.getAPIPaymentResponse(paymentMethod, BuildConfig.WalletId, getApplicationContext());
            //Get the Payment Response Details
            JSONObject paymentResp = new JSONObject(PrefsHelper.getPaymentResonseDetails(getApplicationContext()));
            JSONObject detailsObj = paymentResp.getJSONObject("details");
            if (requestBillingAddress) {
                detailsObj.put("billingAddress", APIService.getBillingAddress(paymentRequestEvent.getPayerPhone(), paymentRequestEvent.getPayerName()));
            }
            //Added the W3C Parameter
            detailsObj.put("isW3C", false);
            jsonObjectParent.put("details", detailsObj);
            jsonObjectParent.put("shippingAddress", APIService.getShippingAddress(requestObject.getShippingAddress()));
            paymentResponse = jsonObjectParent.toString().replaceAll("\\\\", "");
            Log.v("paymentresponse", paymentResponse);
        } catch (JSONException e) {
            LogUtil.error(logTag, "preparePaymentResponse , json exe :" + e.getMessage());
        } catch (Exception e1) {
            LogUtil.error(logTag, "preparePaymentResponse , exe :" + e1.getMessage());
        } finally {
            pbPayment.setVisibility(View.GONE);
        }
        return paymentResponse;
    }


    public void parseJavaObject(Object obg) {
        Field[] fields = obg.getClass().getDeclaredFields();
        for (Field field : fields) {
            Log.v("Field Name", field.getName());
            Log.v("Field Value", field.toString());
        }
    }

    /**
     * Prepares and returns the Cancel Payment response which will used in respondWith() method
     *
     * @param paymentRequestEvent - Payment request object
     * @return - Returns the String format of Cancel Payment Response JSON
     */
    private String prepareCancelResponse(PaymentRequestEvent paymentRequestEvent) {
        String cancelResponse = null;
        try {
            JSONObject jsonObjectParent = new JSONObject();
            ArrayList<PaymentMethodData> paymentMethodData = (ArrayList<PaymentMethodData>) paymentRequestEvent.getPaymentMethodData();
            String methodName = "";
            if (paymentMethodData != null && !paymentMethodData.isEmpty()) {
                ArrayList<String> supportedMethods = (ArrayList<String>) paymentMethodData.get(0).getSupportedMethods();
                if (supportedMethods != null && !supportedMethods.isEmpty()) {
                    methodName = supportedMethods.get(0);
                }
            }
            jsonObjectParent.put("methodName", methodName);
            jsonObjectParent.put("message", "Payment has been cancelled");
            cancelResponse = jsonObjectParent.toString();
        } catch (JSONException e) {
            LogUtil.error(logTag, "prepareCancelResponse , json exe :" + e.getMessage());
        } catch (Exception e1) {
            LogUtil.error(logTag, "prepareCancelResponse , exe :" + e1.getMessage());
        }
        return cancelResponse;
    }

    /**
     * Get the Encrypted Payment Response
     *
     * @param rawResponse - response before encryption
     * @return - Returns the Encrypted String of Payment Response
     */

    private String getEncryptedPaymentResponse(String rawResponse) {
        String base64Response = null;
        String base64Request = null;
        String cert = requestObject.getMerchantCertificate();
        if (cert == null) {
            cert = requestObject.getMerchantCertificateURL();
        }
        try {
            //Get the Base64 of the Payment Request
            base64Request = Base64.encodeToString(rawResponse.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
            //Get the Encrypted Details from API
            base64Response = APIService.getEncryptData(cert, base64Request, getApplicationContext());
            Log.v("EncData", base64Response);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error(logTag, "getEncryptedPaymentResponse , UnsupportedEncodingException :" + e.getMessage());
        }
        return base64Response;
    }

    /**
     * Get the Encrypted Cancel Response
     *
     * @param rawResponse - response before encryption
     * @return - Returns the Encrypted String of Payment Response
     */

    private String getEncryptedCancelResponse(String rawResponse) {
        String base64Response = null;
        String base64Request = null;
        try {
            //Get the Base64 of the Payment Request
            base64Request = Base64.encodeToString(rawResponse.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
            //Get the Encrypted Details from API
            base64Response = SecurityService.encypt(requestObject.getMerchantCertificate(), base64Request);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error(logTag, "getEncryptedCancelResponse , UnsupportedEncodingException :" + e.getMessage());
        }
        return base64Response;
    }

    /**
     * Parses and displays the Payment Request Data to UI
     *
     * @param data - String format of Payment Request Payload
     */
    private void processAndDisplayPaymentDetails(String data) {
        LogUtil.debug(logTag, "onCreate , data :" + data);
        /**
         * Gets the PaymentRequestEvent POJO from String format of Payment Request JSON Payload
         */
        requestObject = PaymentUtil.getRequestObject(data);
        if (requestObject != null) {
            ArrayList<PaymentMethodData> paymentMethodData = (ArrayList<PaymentMethodData>) requestObject.getPaymentMethodData();
            //tvMerchantId.setText(requestObject.getMerchantID());
            if (paymentMethodData != null && !paymentMethodData.isEmpty()) {
                Data data1 = paymentMethodData.get(0).getData();
                if (data1 != null) {
                    tvMerchantName.setText(data1.getMerchantName());
                }
            }
            Total total = requestObject.getTotal();
            if (total != null) {
                tvTotalAmount.setText(total.getValue() + " " + total.getCurrency());
            }
        }
    }

    /**
     * Send the cancel response
     *
     * @param code    Cancel Response Code
     * @param message Cancel Response Message
     */
    private void respondCancelResponse(String code, String message) {

        JSONObject jsonObjectResp = new JSONObject();
        JSONObject jsonObjectMessage = new JSONObject();
        try {
            jsonObjectMessage.put("requestId", requestObject.getPaymentRequestId());
            jsonObjectMessage.put("message", message);
            jsonObjectMessage.put("code", code);
            jsonObjectResp.put("message", jsonObjectMessage);
            String paymentResponse = jsonObjectResp.toString().replaceAll("\\\\", "");
            String base64EncodedFinalData = Base64.encodeToString(paymentResponse.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
            paymentHandler.respondWith(requestObject.getPaymentRequestOrigin(), base64EncodedFinalData, selectedBrowserForPayment);
        } catch (JSONException | UnsupportedEncodingException e) {
            LogUtil.error(logTag, "respondCancelResponse , exception :" + e.getMessage());
        } catch (ArtefactException e) {
            LogUtil.error(logTag, "respondCancelResponse , ArtefactException :" + e.getMessage());
        }
    }


    /**
     * Fetches certificate from url and return
     *
     * @param url - Uri
     * @return - Processed data in string format
     */
    private String getCertFromURL(String url) {
        StringBuilder response = new StringBuilder();
        URL website = null;
        try {
            website = new URL(url);
        } catch (MalformedURLException e) {
            LogUtil.error(logTag, "getCertFromURL , MalformedURLException :" + e.getMessage());
        }
        URLConnection connection = null;
        try {
            connection = website.openConnection();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            in.close();
        } catch (IOException e) {
            LogUtil.error(logTag, "getCertFromURL , IOException :" + e.getMessage());
        }
        return response.toString();
    }


    /**
     * Encrypt the Payment Response
     */
    private void callEncryptAPI() {
        String paymentRespEncode = "";
        pbPayment.setVisibility(View.VISIBLE);
        try {
            paymentRespEncode = Base64.encodeToString(paymentResp.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            LogUtil.error(logTag, "callEncryptAPI , UnsupportedEncodingException :" + e.getMessage());
        }
        String base_api_url = getApplicationContext().getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();
        Log.v("Payment Response Base64", paymentRespEncode);
        Log.v("Merchant Cert URL", merchantCertURL);
        try {
            reqjson.put("merchantCertificateURL", merchantCertURL);
            reqjson.put("paymentResponse", paymentRespEncode);
        } catch (JSONException e) {
            LogUtil.error(logTag, "callEncryptAPI , JSONException :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);

        retrofitInterface.getEncryptedResponse(body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                pbPayment.setVisibility(View.GONE);

                Log.v("EncDataStr", response.raw().toString());
                try {
                    String data = response.body().string();
                    Log.v("EncData", data);
                    JSONObject json = (JSONObject) new JSONTokener(data).nextValue();
                    String encryptedPR = (String) json.get("paymentResponse");
                    callSignAPI(encryptedPR);
                } catch (JSONException | IOException e) {
                    pbPayment.setVisibility(View.GONE);
                    LogUtil.error(logTag, "callEncryptAPI , exception :" + e.getMessage());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
                pbPayment.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Sign the Payment Response
     *
     * @param encryptedPR Encrypted Payment Response
     */
    private void callSignAPI(final String encryptedPR) {

        String base_api_url = getApplicationContext().getString(R.string.base_api_url);
        RequestBody body = RequestBody.create(MediaType.parse("application/text"), encryptedPR);
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);

        retrofitInterface.signPaymentResponse(body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                try {
                    String data = response.body().string();
                    Log.v("Signed", data);
                    JSONObject json = (JSONObject) new JSONTokener(data).nextValue();
                    String code = (String) json.get("code");
                    if (code.equals("AHI2000")) {
                        String sign = (String) json.get("sign");
                        respondPaymentResponse(code, sign, encryptedPR, "Success");
                    }
                } catch (IOException | JSONException e) {
                    LogUtil.error(logTag, "callSignAPI , IOException | JSONException :" + e.getMessage());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
                respondPaymentResponse("AHI5009", null, null, "The payment request is cancelled by user");
            }
        });
    }

    /**
     * Respond the Payment Response
     *
     * @param code        Code of the Message
     * @param sign        Sign of the Payment Response
     * @param encryptedPR Encrypted Payment Response
     * @param message     Message
     */
    private void respondPaymentResponse(String code, String sign, String encryptedPR, String message) {

        JSONObject jsonObjectResp = new JSONObject();
        JSONObject jsonObjectMessage = new JSONObject();
        try {
            jsonObjectResp.put("paymentresponse", encryptedPR);
            jsonObjectResp.put("sign", sign);
            jsonObjectMessage.put("message", message);
            jsonObjectMessage.put("code", code);
            jsonObjectResp.put("message", jsonObjectMessage);
            String paymentResponse = jsonObjectResp.toString().replaceAll("\\\\", "");
            String base64EncodedFinalData = Base64.encodeToString(paymentResponse.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
            paymentHandler.respondWith(requestObject.getPaymentRequestOrigin(), base64EncodedFinalData, selectedBrowserForPayment);

        } catch (JSONException | UnsupportedEncodingException e) {
            LogUtil.error(logTag, "respondPaymentResponse , JSONException | UnsupportedEncodingException e :" + e.getMessage());
        } catch (ArtefactException e) {
            LogUtil.error(logTag, "respondPaymentResponse , ArtefactException :" + e.getMessage());
        }
    }
}
