package com.hkurokawa.shareappsnearby;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_RESOLVE_ERROR = 1024;
    @Nullable
    private GoogleApiClient apiClient;
    private boolean resolvingError;
    private Message message;
    private MessageListener messageListener;
    @Bind(R.id.info)
    TextView info;
    @Bind(R.id.apps_list)
    RecyclerView appsList;
    private AppsListAdapter appsListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Timber.plant(new Timber.DebugTree());

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        appsList.setLayoutManager(new LinearLayoutManager(this));
        appsListAdapter = new AppsListAdapter(this);
        appsList.setAdapter(appsListAdapter);

        final AppInfoList list = new AppInfoList();
        final PackageManager pm = getPackageManager();
        for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0 || (ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 1) {
                list.add(new AppInfo(ai.packageName, pm.getApplicationLabel(ai).toString()));
            }
        }
        try {
            message = new Message(list.toBytes());
        } catch (IOException e) {
            Timber.e(e, "Failed to serialize application list: %s", e.getMessage());
        }
        messageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                byte[] content = message.getContent();
                Timber.i("Received message [%s]", content);
                try {
                    final AppInfoList list = AppInfoList.fromBytes(content);
                    if (appsListAdapter != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appsListAdapter.setApps(list);
                            }
                        });
                    }
                } catch (Exception e) {
                    Timber.e(e, "Failed to parse the given message [%s]: %s", content, e.getMessage());
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.apiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Nearby.MESSAGES_API)
                .build();
        this.apiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.apiClient != null) {
            if (this.apiClient.isConnected()) {
                // Clean up when the user leaves the activity.
                Nearby.Messages.unpublish(apiClient, message)
                        .setResultCallback(new ErrorCheckingCallback("unpublish()"));
                Nearby.Messages.unsubscribe(apiClient, messageListener)
                        .setResultCallback(new ErrorCheckingCallback("unsubscribe()"));
            }
            this.apiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Nearby.Messages.getPermissionStatus(apiClient).setResultCallback(
                new ErrorCheckingCallback("getPermissionStatus", new Runnable() {
                    @Override
                    public void run() {
                        publishAndSubscribe();
                    }
                })
        );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            resolvingError = false;
            if (resultCode == RESULT_OK) {
                // Permission granted or error resolved successfully then we proceed
                // with publish and subscribe..
                publishAndSubscribe();
            } else {
                // This may mean that user had rejected to grant nearby permission.
                Timber.e("Failed to resolve error with code: %d", resultCode);
            }
        }
    }

    private void publishAndSubscribe() {
        // We automatically subscribe to messages from nearby devices once
        // GoogleApiClient is connected. If we arrive here more than once during
        // an activity's lifetime, we may end up with multiple calls to
        // subscribe(). Repeated subscriptions using the same MessageListener
        // are ignored.
        Nearby.Messages.publish(apiClient, message)
                .setResultCallback(new ErrorCheckingCallback("publish()"));
        Nearby.Messages.subscribe(apiClient, messageListener)
                .setResultCallback(new ErrorCheckingCallback("subscribe()"));
    }

    /**
     * A simple ResultCallback that logs when errors occur.
     * It also displays the Nearby opt-in dialog when necessary.
     */
    private class ErrorCheckingCallback implements ResultCallback<Status> {
        private final String method;
        private final Runnable runOnSuccess;

        private ErrorCheckingCallback(String method) {
            this(method, null);
        }

        private ErrorCheckingCallback(String method, @Nullable Runnable runOnSuccess) {
            this.method = method;
            this.runOnSuccess = runOnSuccess;
        }

        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                Timber.i("%s succeeded.", method);
                if (runOnSuccess != null) {
                    runOnSuccess.run();
                }
            } else {
                // Currently, the only resolvable error is that the device is not opted
                // in to Nearby. Starting the resolution displays an opt-in dialog.
                if (status.hasResolution()) {
                    if (!resolvingError) {
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
                            resolvingError = true;
                        } catch (IntentSender.SendIntentException e) {
                            Timber.e("%s failed with exception: %s", method, e);
                        }
                    } else {
                        // This will be encountered on initial startup because we do
                        // both publish and subscribe together.
                        Timber.i("%s failed with status: %s while resolving error.", method, status);
                    }
                } else {
                    Timber.e("%s failed with : %s resolving error: %s", method, status, resolvingError);
                }
            }
        }
    }
}
