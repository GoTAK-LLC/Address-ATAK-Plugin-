package com.gotak.address;

import com.gotak.address.plugin.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The purpose of this activity is to provide a permission request for those permissions that are needed
 * by Services or Activities specifically contained and launched within the plugin.   Unlike the plugin
 * which is loaded in the main class loader and inherits privileges from the core ATAK application, these
 * activities and service and not able to do so.   For good measure this also requests battery optimization
 * disabled.
 */
@SuppressLint("LongLogTag")
public class PluginPermissionActivity extends Activity {

    private static final String TAG = "PluginPermissionActivity";

    // needs to be in both places
    public static final String PLUGIN_PERMISSION_REQUEST_ERROR = "com.gotak.address.pluginPermissionsActivity.ERROR";


    private final String[] permissionList =
            new String[] {
                    "android.permission.POST_NOTIFICATIONS"
            };

    // the number of times a request has been made and not satisfied
    int times = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasPermissions(permissionList)) {
                synchronized (this) {
                    checkAndRequest();
                }
            } else {
                requestPermissions();
            }
        } else {
            synchronized (this) {
                checkAndRequest();
            }
        }
    }


    private boolean hasPermissions(String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED)
                    return false;
            }
        }
        return true;
    }

    @TargetApi(23)
    private void requestPermissions() {
        if (times < 3) {
            times++;
            requestPermissions(permissionList, 315);
        } else {
            final Activity a = this;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.permission_warning);

            builder.setCancelable(false);
            builder.setPositiveButton(R.string.permit_manually,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Intent intent = new Intent();
                            intent.setAction(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", a.getPackageName(),
                                    null);
                            intent.setData(uri);
                            a.startActivity(intent);
                            sendBroadcast(new Intent(PLUGIN_PERMISSION_REQUEST_ERROR));
                            a.finish();
                        }
                    });
            builder.show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < grantResults.length; ++i) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "permission denied: " + permissions[i]);
                requestPermissions();
                return;
            }
        }
        synchronized (this) {
            checkAndRequest();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }



    public void checkAndRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(getPackageName())) {
                finish();
            }

            try {
                Intent intent = new Intent(
                        ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_CODE);
            } catch (Exception e) {
                Log.w(TAG, "Failed to disable battery optimizations.");
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!hasPermissions(permissionList)) {
            sendBroadcast(new Intent(PLUGIN_PERMISSION_REQUEST_ERROR));
            Toast.makeText(this, this.getString(R.string.bad_things), Toast.LENGTH_LONG).show();
        }

        finish();
    }

    public static final int ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS_CODE = 10000;
    public static final String ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";


}

