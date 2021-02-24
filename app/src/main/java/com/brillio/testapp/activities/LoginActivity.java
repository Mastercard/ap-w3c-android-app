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

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.brillio.testapp.BuildConfig;
import com.brillio.testapp.R;
import com.brillio.testapp.biometric_auth.BiometricCallback;
import com.brillio.testapp.biometric_auth.BiometricManager;
import com.brillio.testapp.constant.AppConstants;
import com.brillio.testapp.constant.SecurityConstant;
import com.brillio.testapp.remote.RetrofitClient;
import com.brillio.testapp.util.CommonUtil;
import com.brillio.testapp.util.LogUtil;
import com.brillio.testapp.util.SecurityService;
import com.mastercard.w3c.paymenthandler.PaymentHandler;
import com.mastercard.w3c.paymentmethoddata.PaymentMethodData;
import com.mastercard.w3c.paymentrequest.PaymentRequestEvent;
import com.mastercard.w3c.util.PaymentUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A login screen that offers login via username/password.
 */
@SuppressLint("ByteOrderMark")
public class LoginActivity extends AppCompatActivity implements OnClickListener, BiometricCallback {

    private final String logTag = LoginActivity.class.getSimpleName();
    private EditText mEmailView;
    private EditText mPasswordView;
    private ProgressBar mProgressView;
    private Bundle bundle;
    private PaymentHandler paymentHandler = null;
    private BiometricManager mBiometricManager;

    private String req = null;
    private String sign;
    private String hashData;

    private StringBuilder sbReq;
    private PaymentRequestEvent requestObject = null;
    private String selectedBrowserForPayment = "";
    private Boolean isValidReq = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Get the default instance of the Framework
        paymentHandler = PaymentHandler.getInstance();

        //Initializing the Mastercard W3C Framework
        //this -> is the context of the App Context to initialize
        paymentHandler.init(this, getString(R.string.artefact_enviroment_dev));

        initViews();
        Uri data1 = getIntent().getData();
        if (data1 != null) {

            req = data1.getQueryParameter("requestData");
            sbReq = new StringBuilder();
            selectedBrowserForPayment = data1.getQueryParameter("browser");

            if (req != null) {
                processIncomingData();
            }
            processAndSendApplinkLinkData(data1);

        } else {
            authenticateUser();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.email_sign_in_button) {
            if (CommonUtil.isNetworkAvailable(LoginActivity.this)) {
                doLogin();
            } else {
                CommonUtil.showToast(LoginActivity.this, getResources().getString(R.string.no_network_available));
            }
        }
    }

    /**
     * Validates the input values
     *
     * @return - boolean if inputs are valid
     */
    private boolean isInputValid() {
        boolean isValid = false;

        if (mEmailView.getText().toString().isEmpty()) {
            mEmailView.setError(getString(R.string.error_blank_username));
        }
        if (mPasswordView.getText().toString().isEmpty()) {
            mPasswordView.setError(getString(R.string.error_blank_password));
        }
        if (!mEmailView.getText().toString().isEmpty() && !mPasswordView.getText().toString().isEmpty()) {
            isValid = true;
        }
        return isValid;
    }


    /**
     * Initializes the views
     */
    private void initViews() {
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(this);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * does the login functionality once validation is done
     */
    private void doLogin() {
        if (isInputValid()) {
            CommonUtil.hideKeyBoard(this);
            mProgressView.setVisibility(View.VISIBLE);
            new Handler().postDelayed(delayedRunnable, 2000);
        }
    }

    /**
     * Displays the Biometric Prompt Dialog
     */
    public void authenticateUser() {
        mBiometricManager = new BiometricManager.BiometricBuilder(LoginActivity.this)
                .setTitle(getString(R.string.app_name))
                .setSubtitle(getString(R.string.biometric_dialog_subtitle))
                .setDescription(getString(R.string.biometric_dialog_desc))
                .setNegativeButtonText(getString(R.string.biometric_dialog_cancel_btn))
                .build();

        mBiometricManager.authenticate(LoginActivity.this);
    }


    /**
     * Navigates to respective screens based on event type register , unregister / payment
     */
    private void proceedToNextScreen() {
        Intent intent = new Intent();
        if (bundle != null && bundle.getString(AppConstants.BUNDLE_KEY_EVENT_TYPE).equalsIgnoreCase("payment")) {
            intent.setClass(LoginActivity.this, PaymentActivity.class);
        } else {
            intent.setClass(LoginActivity.this, MasterCardRegistrationActivity.class);
        }
        if (bundle == null) {
            bundle = new Bundle();
            bundle.putString(AppConstants.BUNDLE_KEY_EVENT_TYPE, "normal");
        }
        intent.putExtras(bundle);
        finish();
        startActivity(intent);
    }

    Runnable delayedRunnable = new Runnable() {
        @Override
        public void run() {
            mProgressView.setVisibility(View.GONE);
            proceedToNextScreen();
        }
    };

    /**
     * Process the App Link Data and sends to respective screens
     *
     * @param data1 - Incoming Applink URI data
     */
    private void processAndSendApplinkLinkData(Uri data1) {
        Set<String> queryParameterNames = data1.getQueryParameterNames();
        if (queryParameterNames != null && !queryParameterNames.isEmpty()) {
            bundle = new Bundle();
            if (queryParameterNames.contains(AppConstants.REGISTRATION_STATUS) || queryParameterNames.contains(AppConstants.UNREGISTRATION_STATUS)) {
                Intent intent = new Intent(this, MasterCardRegistrationActivity.class);
                intent.putExtra("reg_unreg_uri", data1);
                finish();
                startActivity(intent);
            } else {
                validatePaymentRequest();
            }

        }
    }


    /**
     * Process the incoming data by decoding the data
     */
    public void processIncomingData() {
        byte[] decodedData = null;
        try {
            decodedData = Base64.decode((req.toString().getBytes("UTF-8")), Base64.DEFAULT);
            Log.v("decodedData", decodedData.toString());
            String data = new String(decodedData, "UTF-8");

            JSONObject json = (JSONObject) new JSONTokener(data).nextValue();
            sign = (String) json.get("signedtoken");
            hashData = (String) json.get("hashData");
            sbReq.append((String) json.get("orderObject"));

            byte[] reqDecode = null;

            reqDecode = android.util.Base64.decode((sbReq.toString().getBytes("UTF-8")), android.util.Base64.DEFAULT);

            String reqData = new String(reqDecode, "UTF-8");

            //Set the request Object
            requestObject = PaymentUtil.getRequestObject(reqData);

        } catch (UnsupportedEncodingException | JSONException e) {
            LogUtil.error(logTag, "processIncomingData, UnsupportedEncodingException | JSONException :" + e.getMessage());
        }
    }

    /**
     * Validates the Payment Request by verifying the hash and sign data
     */
    public void validatePaymentRequest() {
        Log.v("req", req);
        Log.v("hashData", hashData);
        Log.v("sign", sign);
        Log.v("payementRequest", sbReq.toString());

        String BASE_API_URL = getString(R.string.base_api_url);
        mProgressView.setVisibility(View.VISIBLE);

        JSONObject reqjson = new JSONObject();
        try {
            reqjson.put("paymentRequest", sbReq.toString());
            reqjson.put("hashData", hashData);
        } catch (JSONException e) {
            mProgressView.setVisibility(View.GONE);
            LogUtil.error(logTag, "validatePaymentRequest, JSONException :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());

        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient
                .getClient(BASE_API_URL)
                .create(RetrofitClient.RetrofitInterface.class);
        retrofitInterface.verifysignhash(
                sign,
                body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        mProgressView.setVisibility(View.GONE);
                        Log.v("Retrofit  :", String.valueOf(response.isSuccessful()));
                        Log.v("Retrofit  toString :", response.toString());
                        Log.v("Retrofit  raw :", response.raw().toString());
                        try {
                            String resp = response.body().string();
                            Log.v("Retrofit  body resp:", resp);
                            JSONObject det = new JSONObject(resp);
                            if (det.getString("verificationResult").contains("SUCCESS")) {
                                Log.v("Retrofit  details :", "Success");
                                isValidReq = true;
                                bundle.putString(AppConstants.BUNDLE_KEY_EVENT_TYPE, AppConstants.PAYMENT);
                                bundle.putString(AppConstants.BUNDLE_KEY_EVENT_VALUE, sbReq.toString());
                                bundle.putString(AppConstants.BUNDLE_KEY_BROWSER_NAME, selectedBrowserForPayment);
                                callVerifyCertificate();
                            } else {
                                sendUnAuthResponse();
                                isValidReq = false;
                                Log.v("Retrofit  Failure:", response.message());
                            }
                        } catch (IOException | JSONException e) {
                            LogUtil.error(logTag, "validatePaymentRequest, IOException | JSONException :" + e.getMessage());
                            isValidReq = false;
                            sendUnAuthResponse();
                        }

                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        mProgressView.setVisibility(View.GONE);
                        isValidReq = false;
                        sendUnAuthResponse();
                    }
                });

    }


    /**
     * Validates the Merchant Certificate used for Payment Credentials Encryption
     */
    private void callVerifyCertificate() {

        mProgressView.setVisibility(View.VISIBLE);
        String cert = requestObject.getMerchantCertificateURL();

        Log.v("Cert", cert);

        String base_api_url = getApplicationContext().getString(R.string.base_api_url);
        JSONObject reqjson = new JSONObject();

        try {
            reqjson.put("merchantCertificateURL", cert);
            reqjson.put("walletId", BuildConfig.WalletId);
            reqjson.put("paymentType", "mcpba");

        } catch (JSONException e) {
            LogUtil.error(logTag, "callVerifyCertificate, JSONException :" + e.getMessage());
        }

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), reqjson.toString());
        RetrofitClient.RetrofitInterface retrofitInterface = RetrofitClient.getClient(base_api_url).create(RetrofitClient.RetrofitInterface.class);
        retrofitInterface.validatecertificate(body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                mProgressView.setVisibility(View.GONE);
                Log.v("respData", response.raw().toString());
                try {
                    String data = response.body().string();
                    Log.v("Cert Response", data);
                    JSONObject det = new JSONObject(data);
                    if (det.getString("status").contains("success")) {
                        authenticateUser();
                    } else {
                        sendInValidCertResponse();
                    }

                } catch (JSONException | IOException e) {
                    mProgressView.setVisibility(View.GONE);
                    LogUtil.error(logTag, "callVerifyCertificate, JSONException | IOException :" + e.getMessage());
                    sendInValidCertResponse();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                Log.e("failure response", t.toString());
                mProgressView.setVisibility(View.GONE);
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
                respondFailedResponse(getString(R.string.merchant_cert_validation_fail_code), getString(R.string.merchant_cert_validation_fail_message));
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    /**
     * Send UnAuthorised Payment Response
     */
    private void sendUnAuthResponse() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.payment_validation_fail));
        builder.setCancelable(false);
        builder.setPositiveButton(getString(R.string.ok_message), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                respondFailedResponse(getString(R.string.auth_failure_code), getString(R.string.auth_failure_message));
                dialog.dismiss();
                finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    /**
     * Returns the Encrypted Cancel Response
     *
     * @param rawResponse - response before encryption
     * @return - encrypted response
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
            LogUtil.error(logTag, "getEncryptedCancelResponse, UnsupportedEncodingException :" + e.getMessage());
        }

        return base64Response;
    }


    /**
     * Prepares and returns the Cancel Payment response which will used in respondWith() method
     *
     * @param paymentRequestEvent Payment Request Event
     * @return - Returns the String format of Cancel Payment Response JSON
     */
    private String prepareValidationFailResponse(PaymentRequestEvent paymentRequestEvent) {
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

            jsonObjectParent.put("code", "AHI102");
            jsonObjectParent.put("message", "Payment Validation has failed");
            cancelResponse = jsonObjectParent.toString();
        } catch (JSONException e) {
            LogUtil.error(logTag, "prepareCancelResponse , json exe :" + e.getMessage());
        } catch (Exception e1) {
            LogUtil.error(logTag, "prepareCancelResponse , exe :" + e1.getMessage());
        }
        return cancelResponse;
    }


    /***
     * Prepares and returns the Failed Payment response which will used in respondWith() method
     * @param code Failed Message Code
     * @param message Failed Message
     */
    private void respondFailedResponse(String code, String message) {

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
            LogUtil.error(logTag, "respondFailedResponse, JSONException | UnsupportedEncodingException :" + e.getMessage());
        }

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
        if (CommonUtil.isNetworkAvailable(LoginActivity.this)) {
            proceedToNextScreen();
        } else {
            CommonUtil.showToast(LoginActivity.this, getResources().getString(R.string.no_network_available));
        }
    }

    @Override
    public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

    }

    @Override
    public void onAuthenticationError(int errorCode, CharSequence errString) {

    }

}

