package com.ViDuo.viduo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.concurrent.TimeUnit;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQ_ID = 22;

    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private RtcEngine mRtcEngine;
    private boolean mCallEnd;
    private boolean mMuted;
    private boolean connected= false;

    private FrameLayout mLocalContainer;
    private RelativeLayout mRemoteContainer;
    private SurfaceView mLocalView;
    private SurfaceView mRemoteView;

    private ImageView mCallBtn;
    private ImageView mMuteBtn;
    private ImageView mSwitchCameraBtn;



    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onJoinChannelSuccess(String channel, final int uid, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            connected=true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupRemoteVideo(uid);
                }
            });
        }



        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft();
                }
            });
        }
    };

    private void setupRemoteVideo(int uid) {

        int count = mRemoteContainer.getChildCount();
        View view = null;
        for (int i = 0; i < count; i++) {
            View v = mRemoteContainer.getChildAt(i);
            if (v.getTag() instanceof Integer && ((int) v.getTag()) == uid) {
                view = v;
            }
        }

        if (view != null) {
            return;
        }


        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
        mRemoteContainer.addView(mRemoteView);
        // Initializes the video view of a remote user.
        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
        mRemoteView.setTag(uid);
    }

    private void onRemoteUserLeft() {
        removeRemoteVideo();
        Toast.makeText(this, "User left.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void removeRemoteVideo() {
        if (mRemoteView != null) {
            mRemoteContainer.removeView(mRemoteView);
        }
        // Destroys remote view
        mRemoteView = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();
        Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            public void run() {
                if(!connected) {
                    finish();
                    Toast.makeText(MainActivity.this, "No user found. Try after sometime.", Toast.LENGTH_SHORT).show();
                }
            }
        }, 15000);


        if (checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            initEngineAndJoinChannel();
        }
    }

    private void initUI() {
        mLocalContainer = findViewById(R.id.local_video_view_container);
        mRemoteContainer = findViewById(R.id.remote_video_view_container);

        mCallBtn = findViewById(R.id.btn_call);
        mMuteBtn = findViewById(R.id.btn_mute);
        mSwitchCameraBtn = findViewById(R.id.btn_switch_camera);

    }


    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, requestCode);
            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[1] != PackageManager.PERMISSION_GRANTED ||
                    grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                showLongToast("Need permissions " + Manifest.permission.RECORD_AUDIO +
                        "/" + Manifest.permission.CAMERA + "/" + Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }

            initEngineAndJoinChannel();
        }
    }

    private void showLongToast(final String msg) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initEngineAndJoinChannel() {

        initializeEngine();
        setupVideoConfig();
        setupLocalVideo();
        joinChannel();
    }

    private void initializeEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.appid), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new RuntimeException("Need TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void setupVideoConfig() {
        mRtcEngine.enableVideo();

        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                VideoEncoderConfiguration.VD_640x360,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {

        mLocalView = RtcEngine.CreateRendererView(getBaseContext());
        mLocalView.setZOrderMediaOverlay(true);
        mLocalContainer.addView(mLocalView);
         mRtcEngine.setupLocalVideo(new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void joinChannel() {

        String token = getString(R.string.token);
        if (TextUtils.isEmpty(token) || TextUtils.equals(token, "#YOUR ACCESS TOKEN#")) {
            token = null;
        }
        mRtcEngine.joinChannel(token, "DEMO", "Extra Optional Data", 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mCallEnd) {
            connected=false;
            leaveChannel();
        }

        RtcEngine.destroy();
    }

    private void leaveChannel() {
       mRtcEngine.leaveChannel();
    }

    public void onLocalAudioMuteClicked(View view) {
        mMuted = !mMuted;
        mRtcEngine.muteLocalAudioStream(mMuted);
        int res = mMuted ? R.drawable.unmutebtn : R.drawable.mutebtn;
        mMuteBtn.setImageResource(res);
    }

    public void onSwitchCameraClicked(View view) {
        mRtcEngine.switchCamera();
    }

    public void onCallClicked(View view) {
        if (mCallEnd) {
            startCall();
            mCallEnd = false;
        } else {
            endCall();
            mCallEnd = true;
        }

        showButtons(!mCallEnd);
    }


    private void startCall() {
        setupLocalVideo();
        joinChannel();
    }

    private void endCall() {
        removeLocalVideo();
        removeRemoteVideo();
        leaveChannel();
        finish();
    }

    private void removeLocalVideo() {
        if (mLocalView != null) {
            mLocalContainer.removeView(mLocalView);
        }
        mLocalView = null;

    }

    private void showButtons(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        mMuteBtn.setVisibility(visibility);
        mSwitchCameraBtn.setVisibility(visibility);
    }
}

