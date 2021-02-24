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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brillio.testapp.BuildConfig;
import com.brillio.testapp.R;
import com.brillio.testapp.biometric_auth.BiometricCallback;
import com.brillio.testapp.biometric_auth.BiometricManager;
import com.brillio.testapp.constant.SecurityConstant;
import com.brillio.testapp.remote.APIService;
import com.brillio.testapp.remote.RetrofitClient;
import com.brillio.testapp.util.CommonUtil;
import com.brillio.testapp.util.LogUtil;
import com.brillio.testapp.util.PrefsHelper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


public class W3CPaymentActivity extends AppCompatActivity implements View.OnClickListener, BiometricCallback {

    private final String logTag = W3CPaymentActivity.class.getSimpleName();
    private String topLevelOrigin;
    private String paymentRequestOrigin;
    private String paymentRequestId;
    private String total;
    private String methodDataTitle;
    private String methodDataValue;
    private String methodNameValue;
    private RelativeLayout rlContentLayout;
    private RelativeLayout rlLoginLayout;
    private BiometricManager mBiometricManager;
    private final String paymentMethod = "mcpba";
    private ProgressBar pbPayment;
    private String merchantCertificateURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_w3_cpayment);
        authenticateUser();
        Bundle extras = getIntent().getExtras();
        LogUtil.debug(logTag, "onCreate , extras :" + extras);
        if (getIntent() != null) {
            LogUtil.debug(logTag, "onCreate , action :" + getIntent().getAction());
        }
        getRequestParametersFromBundle(extras);
        initViews();
    }

    /**
     * Initialize the view of the Activity
     */
    private void initViews() {
        rlContentLayout = findViewById(R.id.rl_content_layout);
        rlLoginLayout = findViewById(R.id.rl_login_layout);
        TextView mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(this);
        Button buttonCancel = findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(this);
        Button buttonPay = findViewById(R.id.buttonPay);
        buttonPay.setOnClickListener(this);

        TextView tvTopLevelOrigin = findViewById(R.id.textTopLevelOriginValue);
        TextView tvPaymentRequestOrigin = findViewById(R.id.textPaymentRequestOriginValue);
        TextView tvPaymentRequestId = findViewById(R.id.textPaymentRequestIdValue);
        TextView tvMethodData = findViewById(R.id.textMethodDataValue);
        TextView tvMethodDataTitle = findViewById(R.id.textMethodDataTitle);
        TextView tvTotal = findViewById(R.id.textTotalValue);
        TextView tvMethodNameValue = findViewById(R.id.textMethodNameValue);
        pbPayment = findViewById(R.id.pbPayment);

        tvTopLevelOrigin.setText(topLevelOrigin);
        tvPaymentRequestOrigin.setText(paymentRequestOrigin);
        tvPaymentRequestId.setText(paymentRequestId);
        tvMethodData.setText(methodDataValue);
        tvMethodDataTitle.setText(methodDataTitle);
        tvTotal.setText(total);
        tvMethodNameValue.setText(methodNameValue);
    }


    /**
     * Method to get the Request Parameters from the Bundle
     *
     * @param requestBundle Request Bundle
     */
    private void getRequestParametersFromBundle(Bundle requestBundle) {

        topLevelOrigin = requestBundle.getString("topLevelOrigin");
        paymentRequestOrigin = requestBundle.getString("paymentRequestOrigin");
        paymentRequestId = requestBundle.getString("paymentRequestId");
        String totalJson = requestBundle.getString("total");
        String methodData = requestBundle.getString("data");

        try {
            JSONObject totalObject = new JSONObject(totalJson);
            String amount = totalObject.getString("value");
            String currency = totalObject.getString("currency");
            total = amount + " " + currency;
            JSONObject methodDataJson = new JSONObject(methodData);
            methodDataTitle = getString(R.string.title_merchant_name);
            methodDataValue = methodDataJson.getString("merchantName");
            merchantCertificateURL = methodDataJson.getString("merchantCertificateURL");
        } catch (JSONException e) {
            LogUtil.error(logTag, "getRequestParametersFromBundle() ,exe while parsing JSON");
        }

        methodNameValue = requestBundle.getString("methodName");
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.buttonPay) {
            processPaymentEvent();
        } else if (i == R.id.buttonCancel) {
            processCancelEvent();
        } else if (i == R.id.email_sign_in_button) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    rlLoginLayout.setVisibility(View.GONE);
                    rlContentLayout.setVisibility(View.VISIBLE);
                }
            }, 1000);
        }
    }


    /**
     * Method to process the Payment Event
     */
    private void processPaymentEvent() {
        callVerifyCertificate();
    }


    /**
     * Method to get the PaymentResponse for W3C Response
     *
     * @return Payment Response String
     */
    private String preparePaymentResponseW3C() {
        String paymentResponse = null;

        try {
            JSONObject jsonObjectParent = new JSONObject();
            jsonObjectParent.put("methodName", methodNameValue);

            //Get the Payment Response Details
            JSONObject paymentResp = new JSONObject(PrefsHelper.getPaymentResonseDetails(getApplicationContext()));
            //Log.v("json", paymentResp.toString());
            JSONObject detailsObj = paymentResp.getJSONObject("details");

            for (int i = 0; i < detailsObj.names().length(); i++) {
                jsonObjectParent.put(detailsObj.names().getString(i), detailsObj.get(detailsObj.names().getString(i)));
            }

            paymentResponse = jsonObjectParent.toString().replaceAll("\\\\", "");
        } catch (JSONException e) {
            LogUtil.error(logTag, "preparePaymentResponse , json exe :" + e.getMessage());
        } catch (Exception e1) {
            LogUtil.error(logTag, "preparePaymentResponse , exe :" + e1.getMessage());
        }

        Log.v("paymentresp w3c", paymentResponse);

        return paymentResponse;
    }


    /**
     * Method to process the Cancel Event
     */
    protected void processCancelEvent() {
        Intent result = new Intent();
        setResult(RESULT_CANCELED, result);
        finish();
    }


    /**
     * Displays the Biometric Prompt Dialog
     */
    public void authenticateUser() {
        mBiometricManager = new BiometricManager.BiometricBuilder(W3CPaymentActivity.this)
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric_dialog_subtitle))
                .setDescription(getString(R.string.biometric_dialog_desc))
                .setNegativeButtonText(getString(R.string.biometric_dialog_cancel_btn))
                .build();

        mBiometricManager.authenticate(W3CPaymentActivity.this);
    }


    /**
     * Method to call an API to Encrypt the Payment Response
     *
     * @param paymentResponse Payment Response
     */
    private void callEncryptAPI(String paymentResponse) {
        APIService.getAPIPaymentResponse(paymentMethod, BuildConfig.WalletId, getApplicationContext());
        pbPayment.setVisibility(View.VISIBLE);

        try {
            paymentResponse = Base64.encodeToString(paymentResponse.getBytes(SecurityConstant.CHARSET_NAME), Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            LogUtil.debug(logTag, "callEncryptAPI , UnsupportedEncodingException :" + e.getMessage());
        }

        String base_api_url = getApplicationContext().getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();
        Log.v("Payment Response", paymentResponse);
        try {
            reqjson.put("merchantCertificateURL", merchantCertificateURL);
            reqjson.put("paymentResponse", paymentResponse);
        } catch (JSONException e) {
            LogUtil.debug(logTag, "callEncryptAPI , JSONException :" + e.getMessage());
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
                    LogUtil.error(logTag, "callEncryptAPI , JSONException | IOException :" + e.getMessage());
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
     * Method to call an API to Signs the Encrypted Payment Response
     *
     * @param encryptedPR
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
                    LogUtil.error(logTag, "callSignAPI , JSONException | IOException :" + e.getMessage());
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
     * Construct the final Payment Response
     *
     * @param code        Message Code
     * @param sign        Sign of Payment Response
     * @param encryptedPR Encrypted Payment Response
     * @param message     Message
     */
    private void respondPaymentResponse(String code, String sign, String encryptedPR, String message) {

        JSONObject jsonObjectResp = new JSONObject();
        JSONObject jsonObjectMessage = new JSONObject();
        try {
            jsonObjectResp.put("requestId", paymentRequestId);
            jsonObjectResp.put("isW3C", true);
            jsonObjectResp.put("paymentresponse", encryptedPR);
            jsonObjectResp.put("sign", sign);
            jsonObjectMessage.put("message", message);
            jsonObjectMessage.put("code", code);
            jsonObjectResp.put("message", jsonObjectMessage);
            String paymentResponse = jsonObjectResp.toString().replaceAll("\\\\", "");
            sendResponse(paymentResponse);
        } catch (JSONException e) {
            LogUtil.error(logTag, "respondPaymentResponse , JSONException :" + e.getMessage());
        }
    }


    /**
     * Method to send response to Intent
     *
     * @param details Final details object
     */
    private void sendResponse(String details) {

        Log.v("details", details);
        Intent result = new Intent();
        Bundle extras = new Bundle();
        extras.putString("methodName", methodNameValue);
        extras.putString("details", details);
        result.putExtras(extras);
        Log.v("result", extras.toString());
        setResult(RESULT_OK, result);
        finish();
    }


    @Override
    public void onSdkVersionNotSupported() {
        CommonUtil.showToast(this, getString(R.string.biometric_error_sdk_not_supported));
    }

    @Override
    public void onBiometricAuthenticationNotSupported() {
        CommonUtil.showToast(this, getString(R.string.biometric_error_hardware_not_supported));
    }

    @Override
    public void onBiometricAuthenticationNotAvailable() {
        CommonUtil.showToast(this, getString(R.string.biometric_error_fingerprint_not_available));
    }

    @Override
    public void onBiometricAuthenticationPermissionNotGranted() {
        CommonUtil.showToast(this, getString(R.string.biometric_error_permission_not_granted));
    }

    @Override
    public void onBiometricAuthenticationInternalError(String error) {
        CommonUtil.showToast(this, error);
    }

    @Override
    public void onAuthenticationFailed() {
    }

    @Override
    public void onAuthenticationCancelled() {
        CommonUtil.showToast(this, getString(R.string.biometric_cancelled));
        mBiometricManager.cancelAuthentication();
    }

    @Override
    public void onAuthenticationSuccessful() {
        CommonUtil.showToast(this, getString(R.string.biometric_success));
        rlLoginLayout.setVisibility(View.GONE);
        rlContentLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {

    }

    /**
     * Validates the Merchant Certificate used for Payment Credentials Encryption
     */
    private void callVerifyCertificate() {

        pbPayment.setVisibility(View.VISIBLE);

        String base_api_url = getApplicationContext().getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();

        try {
            reqjson.put("merchantCertificateURL", merchantCertificateURL);
            reqjson.put("walletId", BuildConfig.WalletId);
            reqjson.put("paymentType", "mcpba");

        } catch (JSONException e) {
            LogUtil.error(logTag, "callVerifyCertificate , JSONException :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);
        retrofitInterface.validatecertificate(body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                pbPayment.setVisibility(View.GONE);
                Log.v("respData", response.raw().toString());
                try {
                    String data = response.body().string();
                    Log.v("Cert Response", data);
                    JSONObject det = new JSONObject(data);
                    if (det.getString("status").contains("success")) {
                        callEncryptAPI(preparePaymentResponseW3C());
                    } else {
                        sendInValidCertResponse();
                    }

                } catch (JSONException | IOException e) {
                    pbPayment.setVisibility(View.GONE);
                    LogUtil.error(logTag, "callVerifyCertificate , JSONException | IOException :" + e.getMessage());
                    sendInValidCertResponse();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
                pbPayment.setVisibility(View.GONE);
                sendInValidCertResponse();
            }
        });
    }


    /**
     * Send Invalid Certificate Payment Response
     */
    private void sendInValidCertResponse() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.merchant_cert_validation_fail));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok_message), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                respondPaymentResponse(getString(R.string.merchant_cert_validation_fail_code), null, null, getString(R.string.merchant_cert_validation_fail_message));
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
