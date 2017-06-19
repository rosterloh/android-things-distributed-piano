package za.co.riggaroo.androidthings.distributedpiano;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.nio.ByteBuffer;

class PianoPresenter implements PianoContract.Presenter, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "PianoPresenter";
    private final GoogleApiClient googleApiClient;
    private static final String DEVICE_NAME = "DistributedPiano";
    private final String serviceId;
    private PianoContract.View view;

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, "Connection initiated from " + endpointId + " " + connectionInfo.getEndpointName());
            Nearby.Connections.acceptConnection(googleApiClient, endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
            Log.d(TAG, "onConnectionResult");
            if (connectionResolution.getStatus().isSuccess()) {
                Log.d(TAG, "Endpoint " + endpointId + " connected");
            } else {
                Log.w(TAG, "Endpoint " + endpointId + " already connected");
            }
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.d(TAG, endpointId + " disconnected");
        }
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            Log.d(TAG, "onPayloadReceived");
            double frequency = toDouble(payload.asBytes());
            if (frequency == -1) {
                view.stopPlayingNote();
                return;
            }
            view.playNote(frequency);
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
            switch (payloadTransferUpdate.getStatus()) {
                case PayloadTransferUpdate.Status.IN_PROGRESS:
                    Log.d(TAG, "onPayloadTransferUpdate " + payloadTransferUpdate.getBytesTransferred() + " bytes transferred");
                    break;
                case PayloadTransferUpdate.Status.SUCCESS:
                    Log.d(TAG, "onPayloadTransferUpdate completed");
                    break;
                case PayloadTransferUpdate.Status.FAILURE:
                    Log.d(TAG, "onPayloadTransferUpdate failed");
                    break;
            }
        }
    };

    PianoPresenter(Context context, String serviceId) throws IOException {
        googleApiClient = new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Nearby.CONNECTIONS_API).build();
        this.serviceId = serviceId;
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "onConnected!");
        startAdvertising();
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "onConnectionSuspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
    }

    private void startAdvertising() {
        Log.d(TAG, "startAdvertising");
        AdvertisingOptions advertisingOptions = new AdvertisingOptions(Strategy.P2P_CLUSTER);
        Nearby.Connections
                .startAdvertising(googleApiClient, DEVICE_NAME, serviceId, connectionLifecycleCallback, advertisingOptions)
                .setResultCallback(new ResultCallback<Connections.StartAdvertisingResult>() {
                    @Override
                    public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                        Log.d(TAG, "startAdvertising:onResult:" + result);
                        if (result.getStatus().isSuccess()) {
                            Log.d(TAG, "startAdvertising:onResult: SUCCESS");

                        } else {
                            Log.d(TAG, "startAdvertising:onResult: FAILURE ");
                            int statusCode = result.getStatus().getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) {
                                Log.d(TAG, "STATUS_ALREADY_ADVERTISING");
                            } else {
                                Log.d(TAG, "STATE_READY");
                            }
                        }
                    }
                });
    }

    private static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    @Override
    public void detachView() {
        this.view = null;
        googleApiClient.disconnect();
    }

    @Override
    public void attachView(final PianoContract.View view) {
        this.view = view;
        googleApiClient.connect();
    }
}
