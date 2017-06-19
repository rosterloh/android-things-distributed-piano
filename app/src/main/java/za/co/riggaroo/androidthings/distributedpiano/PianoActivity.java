package za.co.riggaroo.androidthings.distributedpiano;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.contrib.driver.pwmspeaker.Speaker;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class PianoActivity extends Activity implements PianoContract.View {

    private static final String TAG = "PianoActivity";
    private final static int REQUEST_PERMISSION_REQ_CODE = 33;
    private Speaker speaker;

    private PianoContract.Presenter presenter;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    private boolean hasRequiredPermissions() {
        return checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRequiredPermissions() {
        if (shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION)) {
            Log.w(TAG, "Location permission is required for this application");
        }
        requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_PERMISSION_REQ_CODE);
    }

    private void startComponents() {
        try {
            speaker = new Speaker(BoardDefaults.getPwmPin());
            presenter = new PianoPresenter(this, getString(R.string.service_id));
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't be opened, lets end this here.");
        }
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
    protected void onDestroy() {
        super.onDestroy();
        try {
            speaker.stop();
            speaker.close();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to stop the piezo", e);
        }
    }


    @Override
    public void playNote(final double frequency) {
        try {
            speaker.play(frequency);
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't play note.", e);
        }
    }

    @Override
    public void stopPlayingNote() {
        try {
            speaker.stop();
        } catch (IOException e) {
            throw new IllegalArgumentException("Piezo can't stop.", e);
        }
    }
}
