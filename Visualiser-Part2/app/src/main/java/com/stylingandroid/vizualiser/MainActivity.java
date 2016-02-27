package com.stylingandroid.vizualiser;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.stylingandroid.vizualiser.permissions.PermissionsActivity;
import com.stylingandroid.vizualiser.permissions.PermissionsChecker;
import com.stylingandroid.vizualiser.renderer.RendererFactory;

public class MainActivity extends AppCompatActivity implements Visualizer.OnDataCaptureListener {

    private static final int CAPTURE_SIZE = 256;
    private static final int REQUEST_CODE = 0;
    static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    private Visualizer visualiser;
    private WaveformView waveformView;

    boolean misPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        waveformView = (WaveformView) findViewById(R.id.waveform_view);
        RendererFactory rendererFactory = new RendererFactory();
        waveformView.setRenderer(rendererFactory.createSimpleWaveformRenderer(Color.GREEN, Color.DKGRAY));

        doBindService();

        Intent music = new Intent();
        music.setClass(this, MusicService.class);
        startService(music);

    }

    @Override
    protected  void onDestroy(){
        mServ.stopMusic();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionsChecker checker = new PermissionsChecker(this);

        if (checker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        } else {
            startVisualiser();
        }
        if (!misPlaying) {
            mServ.resumeMusic();
            misPlaying = true;
        }
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }
    }

    private void startVisualiser() {
        visualiser = new Visualizer(0);
        visualiser.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, false);
        visualiser.setCaptureSize(CAPTURE_SIZE);
        visualiser.setEnabled(true);
    }

    @Override
    public void onWaveFormDataCapture(Visualizer thisVisualiser, byte[] waveform, int samplingRate) {
        if (waveformView != null) {
            waveformView.setWaveform(waveform);
        }
    }

    @Override
    public void onFftDataCapture(Visualizer thisVisualiser, byte[] fft, int samplingRate) {
        // NO-OP
    }

    @Override
    protected void onPause() {
        if (visualiser != null) {
            visualiser.setEnabled(false);
            visualiser.release();
            visualiser.setDataCaptureListener(null, 0, false, false);
        }
        super.onPause();
        if(misPlaying) {
            mServ.pauseMusic();
            misPlaying = false;
        }
    }

    private boolean mIsBound = false;
    private MusicService mServ;
    private ServiceConnection Scon =new ServiceConnection(){

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mServ = ((MusicService.ServiceBinder)binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mServ = null;
        }
    };

    void doBindService(){
        bindService(new Intent(this,MusicService.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService()
    {
        if(mIsBound)
        {
            unbindService(Scon);
            mIsBound = false;
        }
    }
}
