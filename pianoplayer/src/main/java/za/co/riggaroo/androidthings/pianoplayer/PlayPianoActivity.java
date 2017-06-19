package za.co.riggaroo.androidthings.pianoplayer;

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import za.co.riggaroo.androidthings.pianoplayer.keyboard.KeyBoardListener;
import za.co.riggaroo.androidthings.pianoplayer.keyboard.KeyboardView;
import za.co.riggaroo.androidthings.pianoplayer.keyboard.ScrollStripView;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class PlayPianoActivity extends AppCompatActivity implements PlayPianoContract.View {

    private static final String TAG = "PlayPianoActivity";
    private final static int REQUEST_PERMISSION_REQ_CODE = 33;

    PlayPianoContract.Presenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_piano);

        if (hasRequiredPermissions()) {
            startComponents();
        } else {
            requestRequiredPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_REQ_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.ACCESS_COARSE_LOCATION permission. Now we may proceed with advertising.
                    startComponents();
                    presenter.attachView(this);
                } else {
                    Log.w(TAG, "Required permissions not granted");
                }
                break;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean hasRequiredPermissions() {
        return checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestRequiredPermissions() {
        if (shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION)) {
            Log.w(TAG, "Location permission is required for this application");
        }
        requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
    }

    private void startComponents() {
        presenter = new PlayPianoPresenter(this, getString(R.string.service_id));

        KeyboardView keyboardView = (KeyboardView) findViewById(R.id.piano_view);
        ScrollStripView scrollStrip = (ScrollStripView) findViewById(R.id.scroll_strip);
        scrollStrip.bindKeyboard(keyboardView);
        keyboardView.setMidiListener(new KeyBoardListener() {
            @Override
            public void onNoteOff(final int channel, final int note, final int velocity) {
                Log.d(TAG, "onNoteOff, channel:" + channel + " note:" + note + ". velocity:" + velocity);
                presenter.noteStopped(note);
            }

            @Override
            public void onNoteOn(final int channel, final int note, final int velocity) {
                Log.d(TAG, "onNoteOn, channel:" + channel + " note:" + note + ". velocity:" + velocity);

                presenter.notePlayed(note);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        if (presenter != null)
            presenter.attachView(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (presenter != null)
            presenter.detachView();
    }


    @Override
    public void showConnectedToMessage(final String endpointName) {
        Toast.makeText(getApplicationContext(), getString(R.string.connected_to, endpointName), Toast.LENGTH_LONG)
                .show();

    }

    @Override
    public void showApiNotConnected() {
        Toast.makeText(getApplicationContext(), getString(R.string.google_api_not_connected), Toast.LENGTH_LONG).show();
    }

}
