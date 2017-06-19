package za.co.riggaroo.androidthings.pianoplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.nio.ByteBuffer;

class PlayPianoPresenter implements PlayPianoContract.Presenter, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "PlayPianoPresenter";
    private static final String DEVICE_NAME = "PianoPlayer";
    private final GoogleApiClient googleApiClient;
    private final String serviceId;
    private String otherEndpointId;
    private PlayPianoContract.View view;

    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
            Log.d(TAG, "onEndpointFound:" + endpointId + ":" + discoveredEndpointInfo.getEndpointName());
            Nearby.Connections.stopDiscovery(googleApiClient);
            connectTo(endpointId, discoveredEndpointInfo.getEndpointName());
        }

        @Override
        public void onEndpointLost(String endpointId) {
            Log.d(TAG, "onEndpointLost:" + endpointId);
            startDiscovery();
        }
    };

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.d(TAG, "Connection initiated from " + endpointId + " " + connectionInfo.getEndpointName());
            Nearby.Connections.acceptConnection(googleApiClient, endpointId, payloadCallback);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
            Log.d(TAG, "Connection from " + endpointId);
            if (connectionResolution.getStatus().isSuccess()) {
                otherEndpointId = endpointId;
            } else {
                Log.w(TAG, "Connection to " + endpointId + " rejected");
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

    PlayPianoPresenter(Context context, String serviceId) {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(Nearby.CONNECTIONS_API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        this.serviceId = serviceId;
    }

    public void attachView(PlayPianoContract.View view) {
        this.view = view;
        googleApiClient.connect();
    }

    public void detachView() {
        this.view = null;
        googleApiClient.disconnect();
    }

    private boolean isViewAttached() {
        return view != null;
    }

    @Override
    public void notePlayed(final int noteNumber) {
        double frequency = getFrequencyForNote(noteNumber + 28);
        Log.d(TAG, "Frequency:" + frequency);
        sendNote(frequency);
    }

    @Override
    public void noteStopped(final int note) {
        sendStop();
    }

    @Override
    public void onConnected(@Nullable final Bundle bundle) {
        Log.d(TAG, "onConnected!");
        startDiscovery();
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.d(TAG, "onConnectionSuspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult.getErrorCode());
    }

    private void connectTo(final String endpointId, final String endpointName) {
        Log.d(TAG, "connectTo:" + endpointId + ":" + endpointName);
        Nearby.Connections
                .requestConnection(googleApiClient, DEVICE_NAME, endpointId, connectionLifecycleCallback)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!isViewAttached()) {
                            return;
                        }
                        if (status.isSuccess()) {
                            Log.d(TAG, "onConnectionResponse: " + endpointName + " SUCCESS");
                            view.showConnectedToMessage(endpointName);
                        } else {
                            Log.d(TAG, "onConnectionResponse: " + endpointName + " FAILURE");
                        }
                    }
                });
    }

    private void startDiscovery() {
        Log.d(TAG, "startDiscovery");
        Nearby.Connections
                .startDiscovery(googleApiClient, serviceId, endpointDiscoveryCallback, new DiscoveryOptions(Strategy.P2P_CLUSTER))
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!isViewAttached()) {
                            return;
                        }
                        if (status.isSuccess()) {
                            Log.d(TAG, "startDiscovery:onResult: SUCCESS");
                        } else {
                            Log.d(TAG, "startDiscovery:onResult: FAILURE");

                            int statusCode = status.getStatusCode();
                            if (statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                                Log.d(TAG, "STATUS_ALREADY_DISCOVERING");
                            }
                        }
                    }
                });
    }

    /**
     * The function for calculating the frequency that should be played for a certain note.
     * More information about the formula can be found here: https://en.wikipedia.org/wiki/Piano_key_frequencies
     *
     * @param note The number of the note.
     * @return frequency to play on Piezo.
     */
    private double getFrequencyForNote(int note) {
        return Math.pow(2, ((note - 49.0f) / 12.0f)) * 440;
    }

    private void sendNote(final double frequency) {
        if (!googleApiClient.isConnected()) {
            view.showApiNotConnected();

            return;
        }
        Payload payload = Payload.fromBytes(toByteArray(frequency));
        Nearby.Connections.sendPayload(googleApiClient, otherEndpointId, payload);
    }

    private static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    private void sendStop() {
        if (!googleApiClient.isConnected()) {
            view.showApiNotConnected();
            return;
        }
        Payload payload = Payload.fromBytes(toByteArray(-1));
        Nearby.Connections.sendPayload(googleApiClient, otherEndpointId, payload);
    }
}
