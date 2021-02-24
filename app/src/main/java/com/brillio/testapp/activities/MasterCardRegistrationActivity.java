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

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brillio.testapp.BuildConfig;
import com.brillio.testapp.R;
import com.brillio.testapp.constant.AppConstants;
import com.brillio.testapp.util.CommonUtil;
import com.brillio.testapp.util.LogUtil;
import com.brillio.testapp.util.PrefsHelper;
import com.mastercard.w3c.exception.WalletIdNotFoundException;
import com.mastercard.w3c.paymenthandler.PaymentHandler;
import com.mastercard.w3c.util.PaymentUtil;

import java.util.Map;
import java.util.Set;

public class MasterCardRegistrationActivity extends AppCompatActivity implements View.OnClickListener {

    private final String logTag = MasterCardRegistrationActivity.class.getSimpleName();

    private PaymentHandler paymentHandler;
    private Button btRegister;
    private Button btUnregister;
    private LinearLayout lLMessageHolder;
    private ProgressBar mProgressView;
    private TextView tvVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_card_registration);

        paymentHandler = PaymentHandler.getInstance();

        initViews();

        tvVersion.setText(BuildConfig.VERSION_NAME);

        lLMessageHolder.removeAllViews();

        /**
         * Process incoming Applink Data (Uri) and persist into Shared Preference
         */
        if (getIntent().getExtras() != null) {
            Uri regUnregUri = getIntent().getParcelableExtra("reg_unreg_uri");
            if (regUnregUri != null) {
                Set<String> queryParameterNames = regUnregUri.getQueryParameterNames();
                if (queryParameterNames != null && !queryParameterNames.isEmpty()) {
                    if (queryParameterNames.contains(AppConstants.REGISTRATION_STATUS)) {
                        ////Retrieve the Status
                        String status = PaymentUtil.fetchParamsFromUri(regUnregUri, AppConstants.REGISTRATION_STATUS);
                        //Retrieve the Status Message
                        String statusMsg = paymentHandler.getMessageAndStatus(status);
                        //Retrieve the Browser
                        String browser = PaymentUtil.fetchParamsFromUri(regUnregUri, AppConstants.BROWSER);
                        PrefsHelper.saveString(this, "reg/" + status + "/" + browser + "/" + statusMsg, browser);
                    } else if (queryParameterNames.contains(AppConstants.UNREGISTRATION_STATUS)) {
                        ////Retrieve the Status
                        String status = PaymentUtil.fetchParamsFromUri(regUnregUri, AppConstants.UNREGISTRATION_STATUS);
                        //Retrieve the Status Message
                        String statusMsg = paymentHandler.getMessageAndStatus(status);
                        //Retrieve the Browser
                        String browser = PaymentUtil.fetchParamsFromUri(regUnregUri, AppConstants.BROWSER);
                        PrefsHelper.saveString(this, "unreg/" + status + "/" + browser + "/" + statusMsg, browser);
                    }
                }
            }
        }

        updateRegisterUnRegisterStatus();

    }

    @Override
    public void onClick(View v) {
        if (CommonUtil.isNetworkAvailable(MasterCardRegistrationActivity.this)) {
            int i = v.getId();
            if (i == R.id.register) {
                try {
                    paymentHandler.register(BuildConfig.WalletId, getString(R.string.payment_type));
                    finish();
                } catch (WalletIdNotFoundException e) {
                    LogUtil.error(logTag, "exception while register :" + e.getMessage());
                }
            } else if (i == R.id.bt_unregister) {
                try {
                    finish();
                    paymentHandler.unregister(BuildConfig.WalletId, getString(R.string.payment_type));
                } catch (WalletIdNotFoundException e) {
                    LogUtil.error(logTag, "exception while unregister :" + e.getMessage());
                }
            }
        } else {
            CommonUtil.showToast(MasterCardRegistrationActivity.this, getResources().getString(R.string.no_network_available));
        }
    }

    /**
     * Initializes the views
     */
    private void initViews() {
        lLMessageHolder = findViewById(R.id.message_holder);
        btUnregister = findViewById(R.id.bt_unregister);
        btUnregister.setOnClickListener(this);
        btRegister = findViewById(R.id.register);
        btRegister.setOnClickListener(this);
        mProgressView = findViewById(R.id.registeration_progress);
        tvVersion = findViewById(R.id.tvVersion);
    }

    /**
     * Display the status message in the UI for Register / Un Register
     *
     * @param messageLabel - Message to be displayed
     */
    private void addStatusMessage(String messageLabel) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        TextView textView = new TextView(this);
        textView.setTextColor(getResources().getColor(R.color.green_color));
        textView.setText(messageLabel);
        textView.setLayoutParams(layoutParams);
        lLMessageHolder.addView(textView);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(MasterCardRegistrationActivity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }

    /**
     * Displaying the Status message from persisted values of Register / Un Register in the UI from Shared Preferences
     */
    private void updateRegisterUnRegisterStatus() {

        Map<String, ?> keys = PrefsHelper.getInstance(this).getAll();
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(PrefsHelper.KEY_CHROME)) {
                LogUtil.debug(logTag, "updateRegisterUnRegisterStatus , inside chrome: ");
                String[] browserData = PrefsHelper.getString(this, PrefsHelper.KEY_CHROME).split("/");
                if (browserData[0].equalsIgnoreCase("reg")) {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_CHROME);
                } else {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_CHROME);
                }
            } else if (entry.getKey().equalsIgnoreCase(PrefsHelper.KEY_EDGE)) {
                LogUtil.debug(logTag, "updateRegisterUnRegisterStatus , inside edge: ");
                String[] browserData = PrefsHelper.getString(this, PrefsHelper.KEY_EDGE).split("/");
                if (browserData[0].equalsIgnoreCase("reg")) {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_EDGE);
                } else {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_EDGE);
                }
            } else if (entry.getKey().equalsIgnoreCase(PrefsHelper.KEY_FIREFOX)) {
                LogUtil.debug(logTag, "updateRegisterUnRegisterStatus , inside firefox: ");
                String[] browserData = PrefsHelper.getString(this, PrefsHelper.KEY_FIREFOX).split("/");
                if (browserData[0].equalsIgnoreCase("reg")) {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_FIREFOX);
                } else {
                    addStatusMessage(browserData[3] + " _" + PrefsHelper.KEY_FIREFOX);
                }
            }
        }

        updateButtonStatus();
    }

    /**
     * Updates the button click status based on Register / Unregister status
     */
    private void updateButtonStatus() {
        if (canUnregister()) {
            btUnregister.setEnabled(true);
            btUnregister.setBackgroundColor(Color.parseColor("#E8B406"));
        } else {
            btUnregister.setEnabled(false);
            btUnregister.setBackgroundColor(Color.parseColor("#D5DBDB"));
        }
    }

    /**
     * Returns true if minimum one browser is Registered with payment
     *
     * @return - boolean value registration status
     */
    boolean canUnregister() {
        Map<String, ?> keys = PrefsHelper.getInstance(this).getAll();
        boolean allUnRegisteredEnabled = false;
        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            String storedData = PrefsHelper.getString(this, entry.getKey());
            String[] data = storedData.split("/");
            if (data[0].equalsIgnoreCase("reg")) {
                boolean rStatus = false;
                if (data[1].equalsIgnoreCase("MC010") | data[1].equalsIgnoreCase("MC001")) {
                    rStatus = true;
                }
                allUnRegisteredEnabled = allUnRegisteredEnabled || rStatus;
            }
        }
        return allUnRegisteredEnabled;
    }
}
