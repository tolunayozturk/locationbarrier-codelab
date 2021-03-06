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

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.AwarenessBarrier;
import com.huawei.hms.kit.awareness.barrier.BarrierQueryRequest;
import com.huawei.hms.kit.awareness.barrier.BarrierStatus;
import com.huawei.hms.kit.awareness.barrier.BarrierUpdateRequest;
import com.huawei.hms.kit.awareness.barrier.LocationBarrier;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ENTER_BARRIER_LABEL = "ENTER_BARRIER_LABEL";
    private static final String EXIT_BARRIER_LABEL = "EXIT_BARRIER_LABEL";

    private PendingIntent mPendingIntent;
    private LocationBarrierReceiver mBarrierReceiver;
    HiAnalyticsInstance mHiAnalytics;
    CloudDBHelper mCloudDBHelper;

    String userId;
    double latitude = 41.02456;
    double longitude = 28.85843;
    double radius = 250;

    Chronometer mChronometer;
    TextView tv_log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_log = findViewById(R.id.tv_log);
        mChronometer = findViewById(R.id.chronometer);

        mHiAnalytics = HiAnalytics.getInstance(this);
        mCloudDBHelper = CloudDBHelper.getInstance(this);
        userId = getIntent().getStringExtra("userId");

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            printLog("Permission(s) denied!");
            return;
        }

        final String BARRIER_RECEIVER_ACTION = getApplication().getPackageName() + "LOCATION_BARRIER_RECEIVER_ACTION";
        Intent intent = new Intent(BARRIER_RECEIVER_ACTION);
        mPendingIntent = PendingIntent.getBroadcast(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBarrierReceiver = new LocationBarrierReceiver();
        registerReceiver(mBarrierReceiver, new IntentFilter(BARRIER_RECEIVER_ACTION));

        Awareness.getBarrierClient(this).queryBarriers(BarrierQueryRequest.all())
                .addOnSuccessListener(barrierQueryResponse -> {
                    AwarenessBarrier enterBarrier = LocationBarrier.enter(
                            latitude, longitude, radius);
                    AwarenessBarrier exitBarrier = LocationBarrier.exit(
                            latitude, longitude, radius);

                    if (barrierQueryResponse.getBarrierStatusMap().getBarrierLabels()
                            .contains(ENTER_BARRIER_LABEL)) {
                        removeBarrier(ENTER_BARRIER_LABEL, res -> {
                            if (res) {
                                addBarrier(this, ENTER_BARRIER_LABEL, enterBarrier,
                                        mPendingIntent);
                            }
                        });
                    } else {
                        addBarrier(this, ENTER_BARRIER_LABEL, enterBarrier,
                                mPendingIntent);
                    }

                    if (barrierQueryResponse.getBarrierStatusMap().getBarrierLabels()
                            .contains(EXIT_BARRIER_LABEL)) {
                        removeBarrier(EXIT_BARRIER_LABEL, res -> {
                            if (res) {
                                addBarrier(this, EXIT_BARRIER_LABEL, exitBarrier,
                                        mPendingIntent);
                            }
                        });
                    } else {
                        addBarrier(this, EXIT_BARRIER_LABEL, exitBarrier,
                                mPendingIntent);
                    }
                }).addOnFailureListener(e -> Log.e(TAG, e.getMessage(), e));

//        // Test event
//        Bundle bundle = new Bundle();
//        bundle.putString("test_key2", "test_value2");
//        mHiAnalytics.onEvent("TEST_EVENT2", bundle);
    }

    private void addBarrier(Context context, final String label,
                            AwarenessBarrier barrier, PendingIntent pendingIntent) {
        BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
        BarrierUpdateRequest request = builder.addBarrier(label, barrier, pendingIntent).build();
        Awareness.getBarrierClient(context).updateBarriers(request)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "add " + label + " success",
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "add " + label + " success");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "add " + label + " failed",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "add " + label + " failed", e);
                });
    }

    private void removeBarrier(String label, Consumer<Boolean> result) {
        BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
        builder.deleteBarrier(label);

        Awareness.getBarrierClient(this)
                .updateBarriers(builder.build()).addOnSuccessListener(aVoid -> {
            Log.i(TAG, "removeBarrier: success");
            result.accept(true);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "removeBarrier: " + e.getMessage(), e);
            result.accept(false);
        });
    }

    private void printLog(String msg) {
        DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
        String time = formatter.format(new Date(System.currentTimeMillis()));
        tv_log.append("[" + time + "] " + msg + "\n");
    }

    @Override
    protected void onDestroy() {
        if (mBarrierReceiver != null) {
            unregisterReceiver(mBarrierReceiver);
        }
        super.onDestroy();
    }

    final class LocationBarrierReceiver extends BroadcastReceiver {
        private final String TAG = LocationBarrierReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            BarrierStatus barrierStatus = BarrierStatus.extract(intent);
            String label = barrierStatus.getBarrierLabel();
            switch (barrierStatus.getPresentStatus()) {
                case BarrierStatus.TRUE:
                    Log.i(TAG, "[" + label + "]" + " BarrierStatus: TRUE");
                    printLog("[" + label + "]" + " BarrierStatus: TRUE");
                    if (label.equals("ENTER_BARRIER_LABEL")) {
                        mChronometer.setBase(SystemClock.elapsedRealtime());
                        mChronometer.start();
                    } else if (label.equals("EXIT_BARRIER_LABEL")) {
                        mChronometer.stop();
                        double elapsedMillis = SystemClock.elapsedRealtime() -
                                mChronometer.getBase();
                        printLog("Length of stay: " + elapsedMillis / 1000 + " seconds");
                        Log.e(TAG, "Length of stay: " + elapsedMillis);

                        // Send lengthOfStay via a custom event to Analytics
                        Bundle bundle = new Bundle();
                        bundle.putDouble("length_of_stay", elapsedMillis / 1000);
                        mHiAnalytics.onEvent("LENGTH_OF_STAY", bundle);

                        // Send lengthOfStay to CloudDB
                        User user = new User();
                        user.setId(UUID.randomUUID().toString());
                        user.setUserId(userId);
                        user.setLengthOfStay(elapsedMillis / 1000);
                        mCloudDBHelper.upsertUser(user, res -> {
                            if (res) {
                                mCloudDBHelper.queryAverage(avgResult -> {
                                    printLog("Average length of stay: " + avgResult);
                                });
                            }
                        });
                    }
                    break;
                case BarrierStatus.FALSE:
                    Log.i(TAG, "[" + label + "]" + " BarrierStatus: FALSE");
                    printLog("[" + label + "]" + " BarrierStatus: FALSE");
                    break;
                case BarrierStatus.UNKNOWN:
                    Log.i(TAG, "[" + label + "]" + " BarrierStatus: UNKNOWN");
                    printLog("[" + label + "]" + " BarrierStatus: UNKNOWN");
                    break;
            }
        }
    }
}