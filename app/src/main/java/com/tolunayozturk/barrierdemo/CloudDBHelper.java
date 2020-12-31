package com.tolunayozturk.barrierdemo;

import android.content.Context;
import android.util.Log;

import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.huawei.hmf.tasks.Task;

import java.util.function.Consumer;

public class CloudDBHelper {
    private static final String TAG = CloudDBHelper.class.getSimpleName();
    private static CloudDBHelper instance = null;

    private AGConnectCloudDB mCloudDB;
    private CloudDBZoneConfig mConfig;
    private CloudDBZone mCloudDBZone;

    private CloudDBHelper(Context context) {
        AGConnectCloudDB.initialize(context);

        mCloudDB = AGConnectCloudDB.getInstance();
        try {
            mCloudDB.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo());
        } catch (AGConnectCloudDBException e) {
            Log.e(TAG, "CloudDBHelper: createObjectType" + e.getMessage(), e);
        }

        mConfig = new CloudDBZoneConfig("DB",
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
    }

    public static CloudDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new CloudDBHelper(context);
        }

        return instance;
    }

    public void openCloudDbZone(Consumer<Boolean> result) {
        if (mCloudDBZone == null) {
            Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
            openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
                Log.i(TAG, "openCloudDbZone: success");
                mCloudDBZone = cloudDBZone;
                result.accept(true);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "openCloudDbZone: " + e.getMessage(), e);
                result.accept(false);
            });
        }
    }

    public void upsertUser(User user, Consumer<Boolean> result) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "mCloudDBZone is null");
            return;
        }

        mCloudDBZone.executeUpsert(user)
                .addOnSuccessListener(cloudDBZoneResult -> {
                    Log.i(TAG, "upsertUser: " + user.getUserId() + " success");
                    result.accept(true);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "upsertUser: " + e.getMessage(), e);
                    result.accept(false);
                });
    }

    public void queryAverage(Consumer<Double> result) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "mCloudDBZone is null");
            return;
        }

        CloudDBZoneQuery<User> query = CloudDBZoneQuery.where(User.class);
        Task<Double> queryTask = mCloudDBZone.executeAverageQuery(query, "lengthOfStay",
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);

        queryTask.addOnSuccessListener(cloudDBZoneResult -> {
            Log.d(TAG, "queryAverage: success " + cloudDBZoneResult);
            result.accept(cloudDBZoneResult);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryAverage: " + e.getMessage(), e);
            result.accept(null);
        });
    }
}
