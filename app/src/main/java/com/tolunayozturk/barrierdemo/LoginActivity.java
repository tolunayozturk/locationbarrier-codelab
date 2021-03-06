/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.tolunayozturk.barrierdemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.auth.SignInResult;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.analytics.HiAnalyticsTools;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.support.account.service.AccountAuthService;
import com.huawei.hms.support.hwid.ui.HuaweiIdAuthButton;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int SIGN_IN_REQUEST_CODE = 8888;
    private static final int PERMISSION_REQUEST_CODE = 820;

    private AccountAuthService mAuthService;
    private AccountAuthParams mAuthParams;
    HiAnalyticsInstance mHiAnalytics;
    AGConnectUser mAGConnectUser;
    CloudDBHelper mCloudDBHelper;

    HuaweiIdAuthButton huaweiIdAuthButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        huaweiIdAuthButton = findViewById(R.id.btn_login);

        HiAnalyticsTools.enableLog();
        mHiAnalytics = HiAnalytics.getInstance(this);
        mCloudDBHelper = CloudDBHelper.getInstance(this);

        mHiAnalytics.setAnalyticsEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    PERMISSION_REQUEST_CODE);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }

        mAuthParams = new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setId().setUid().setProfile().setAuthorizationCode().createParams();
        mAuthService = AccountAuthManager.getService(this, mAuthParams);

        huaweiIdAuthButton.setOnClickListener(view -> signIn());
    }

    private void signIn() {
        startActivityForResult(mAuthService.getSignInIntent(), SIGN_IN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST_CODE) {
            Task<AuthAccount> authAccountTask = AccountAuthManager.parseAuthResultFromIntent(data);
            authAccountTask.addOnSuccessListener(authAccount -> {
                Log.i(TAG, "authAccountTask: signIn via AccountKit success" +
                        authAccount.getUnionId());
                mHiAnalytics.setUserId(authAccount.getUnionId());

                mAGConnectUser = AGConnectAuth.getInstance().getCurrentUser();
                if (mAGConnectUser != null) {
                    Log.i(TAG, "signIn via AuthService success " + mAGConnectUser.getUid());
                    mCloudDBHelper.openCloudDbZone(res -> {
                        if (res) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra("userId", authAccount.getUnionId());
                            startActivity(intent);
                        }
                    });
                } else {
                    AGConnectAuth.getInstance().signInAnonymously()
                            .addOnSuccessListener(signInResult -> {
                                Log.i(TAG, "signInAnonymously success " + signInResult.getUser().getUid());
                                mAGConnectUser = signInResult.getUser();
                                mCloudDBHelper.openCloudDbZone(res -> {
                                    if (res) {
                                        Intent intent = new Intent(this, MainActivity.class);
                                        intent.putExtra("userId", authAccount.getUnionId());
                                        startActivity(intent);
                                    }
                                });
                            }).addOnFailureListener(e -> Log.e(TAG, e.getMessage(), e));
                }
            }).addOnFailureListener(e -> Log.e(TAG, "authAccountTask: " + e.getMessage(), e));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isPermissionGranted = true;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    isPermissionGranted = false;
                }
            }

            if (!isPermissionGranted) {
                Toast.makeText(this, "Permission(s) denied! " +
                                "You must give necessary permissions for this demo app to run properly.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permission(s) granted! ", Toast.LENGTH_SHORT).show();
            }
        }
    }
}