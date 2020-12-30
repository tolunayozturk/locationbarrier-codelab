package com.tolunayozturk.barrierdemo;

import android.content.Context;
import android.util.Log;

import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.huawei.hmf.tasks.Task;

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

    public void openCloudDbZone() {
        if (mCloudDBZone == null) {
            Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
            openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
                Log.i(TAG, "openCloudDbZone: success");
                mCloudDBZone = cloudDBZone;
            }).addOnFailureListener(e -> Log.e(TAG, "openCloudDbZone: " + e.getMessage(), e));
        }
    }

    public void upsertUser(User user) {
        if (mCloudDBZone == null) {
            Log.w(TAG, "mCloudDBZone is null");
            return;
        }

        mCloudDBZone.executeUpsert(user)
                .addOnSuccessListener(cloudDBZoneResult -> {
                    Log.i(TAG, "upsertUser: " + user.getUserId() + " success");
                }).addOnFailureListener(e -> Log.e(TAG, "upsertUser: " + e.getMessage(), e));
    }
}
