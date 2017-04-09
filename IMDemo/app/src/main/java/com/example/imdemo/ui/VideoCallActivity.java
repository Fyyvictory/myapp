package com.example.imdemo.ui;

import android.content.DialogInterface;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.example.imdemo.widget.MyChronometer;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.media.EMLocalSurfaceView;
import com.hyphenate.media.EMOppositeSurfaceView;
import com.hyphenate.util.EMLog;
import com.superrtc.sdk.VideoView;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

public class VideoCallActivity extends CallActivity implements View.OnClickListener {

    protected TextView tvCallState;
    protected MyChronometer chronometer;
    protected TextView tvIsP2p;
    protected TextView tvNick;
    protected LinearLayout llTopContainer;
    protected Button btnRecordVideo;
    protected Button btnSwitchCamera;
    protected Button btnCaptureImage;
    protected SeekBar seekbarYDetal;
    protected TextView tvCallMonitor;
    protected ImageView ivMute;
    protected ImageView ivHandsfree;
    protected LinearLayout llVoiceControl;
    protected Button btnHangupCall;
    protected Button btnRefuseCall;
    protected Button btnAnswerCall;
    protected LinearLayout llComingCall;
    protected LinearLayout llBottomContainer;
    protected LinearLayout llSurfaceBaseline;
    protected RelativeLayout llBtns;
    protected TextView tvNetworkStatus;
    protected RelativeLayout rootLayout;
    private boolean isMuteState;
    private boolean isHandsfreeState;
    private boolean isAnswered;
    private boolean endCallTriggerByMe = false;
    private boolean monitor = true;

    private Handler uiHandler;
    private boolean isInCalling;
    boolean isRecording = false;
    //    private Button recordBtn;
    private EMCallManager.EMVideoCallHelper callHelper;
    private Button toggleVideoBtn;
    private BrightnessDataProcess brightnessData = new BrightnessDataProcess();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            finish();
            return;
        }
        super.setContentView(R.layout.activity_video_call);
        CustomerHelper.getCustomerhelper().isVideoCalling = true;
        callType = 1;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        uiHandler = new Handler();
        initView();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_record_video) {
            // record video
        } else if (view.getId() == R.id.btn_switch_camera) {
            handler.sendEmptyMessage(MSG_CALL_SWITCH_CAMERA);
        } else if (view.getId() == R.id.btn_capture_image) {
            DateFormat df = DateFormat.getDateTimeInstance();
            Date date = new Date();
            final String fileName = Environment.getExternalStorageDirectory()+df.format(date)+".jpg";
            EMClient.getInstance().callManager().getVideoCallHelper().takePicture(fileName);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoCallActivity.this,"saved image to:" + fileName,Toast.LENGTH_SHORT).show();
                }
            });
        } else if (view.getId() == R.id.iv_mute) {
            if(isMuteState){
                ivMute.setImageResource(R.drawable.em_icon_mute_normal);
                try {
                    EMClient.getInstance().callManager().resumeVoiceTransfer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                isMuteState = false;
            }else{
                ivMute.setImageResource(R.drawable.em_icon_mute_on);
                try {
                    EMClient.getInstance().callManager().pauseVoiceTransfer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                isMuteState = true;
            }
        } else if (view.getId() == R.id.iv_handsfree) {
            if(isHandsfreeState){
                ivHandsfree.setImageResource(R.drawable.em_icon_speaker_normal);
                closeSpeakerOn();
                isHandsfreeState = false;
            }else {
                ivHandsfree.setImageResource(R.drawable.em_icon_speaker_on);
                openSpeakerOn();
                isHandsfreeState = true;
            }
        } else if (view.getId() == R.id.btn_hangup_call) {
            btnHangupCall.setEnabled(false);
            chronometer.stop();
            callDruationText = chronometer.getText().toString();
            endCallTriggerByMe = true;
            tvCallState.setText(getResources().getString(R.string.hanging_up));
            if(isRecording){
                callHelper.stopVideoRecord();
            }
            handler.sendEmptyMessage(MSG_CALL_END);
        } else if (view.getId() == R.id.btn_refuse_call) {
            isRefused = true;
            btnRefuseCall.setEnabled(false);
            handler.sendEmptyMessage(MSG_CALL_REJECT);
        } else if (view.getId() == R.id.btn_answer_call) {
            EMLog.d("videocallactivity","answercall");
            btnAnswerCall.setEnabled(false);
            openSpeakerOn();
            if(ringtone != null){
                ringtone.stop();
            }
            tvCallState.setText("answering...");
            handler.sendEmptyMessage(MSG_CALL_ANSWER);
            ivHandsfree.setImageResource(R.drawable.em_icon_speaker_on);
            isAnswered = true;
            isHandsfreeState = true;
            llComingCall.setVisibility(View.INVISIBLE);
            btnHangupCall.setVisibility(View.VISIBLE);
            llVoiceControl.setVisibility(View.VISIBLE);
            localSurface.setVisibility(View.VISIBLE);
        }else if(view.getId() == R.id.root_layout){
            if(callingState == CallingState.NORMAL){
                if(llBottomContainer.getVisibility() == View.VISIBLE){
                    llBottomContainer.setVisibility(View.GONE);
                    llTopContainer.setVisibility(View.GONE);
                    oppositeSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFill);
                }else{
                    llBottomContainer.setVisibility(View.VISIBLE);
                    llTopContainer.setVisibility(View.VISIBLE);
                    oppositeSurface.setScaleMode(VideoView.EMCallViewScaleMode.EMCallViewScaleModeAspectFit);
                }
            }
        }
    }

    private void initView() {
        oppositeSurface = (EMOppositeSurfaceView) findViewById(R.id.opposite_surface);
        tvCallState = (TextView) findViewById(R.id.tv_call_state);
        chronometer = (MyChronometer) findViewById(R.id.chronometer);
        tvIsP2p = (TextView) findViewById(R.id.tv_is_p2p);
        tvNick = (TextView) findViewById(R.id.tv_nick);
        llTopContainer = (LinearLayout) findViewById(R.id.ll_top_container);
        btnRecordVideo = (Button) findViewById(R.id.btn_record_video);
        btnRecordVideo.setOnClickListener(VideoCallActivity.this);
        btnSwitchCamera = (Button) findViewById(R.id.btn_switch_camera);
        btnSwitchCamera.setOnClickListener(VideoCallActivity.this);
        btnCaptureImage = (Button) findViewById(R.id.btn_capture_image);
        btnCaptureImage.setOnClickListener(VideoCallActivity.this);
        seekbarYDetal = (SeekBar) findViewById(R.id.seekbar_y_detal);
        tvCallMonitor = (TextView) findViewById(R.id.tv_call_monitor);
        localSurface = (EMLocalSurfaceView) findViewById(R.id.local_surface);
        ivMute = (ImageView) findViewById(R.id.iv_mute);
        ivMute.setOnClickListener(VideoCallActivity.this);
        ivHandsfree = (ImageView) findViewById(R.id.iv_handsfree);
        ivHandsfree.setOnClickListener(VideoCallActivity.this);
        llVoiceControl = (LinearLayout) findViewById(R.id.ll_voice_control);
        btnHangupCall = (Button) findViewById(R.id.btn_hangup_call);
        btnHangupCall.setOnClickListener(VideoCallActivity.this);
        btnRefuseCall = (Button) findViewById(R.id.btn_refuse_call);
        btnRefuseCall.setOnClickListener(VideoCallActivity.this);
        btnAnswerCall = (Button) findViewById(R.id.btn_answer_call);
        btnAnswerCall.setOnClickListener(VideoCallActivity.this);
        llComingCall = (LinearLayout) findViewById(R.id.ll_coming_call);
        llBottomContainer = (LinearLayout) findViewById(R.id.ll_bottom_container);
        llSurfaceBaseline = (LinearLayout) findViewById(R.id.ll_surface_baseline);
        llBtns = (RelativeLayout) findViewById(R.id.ll_btns);
        tvNetworkStatus = (TextView) findViewById(R.id.tv_network_status);
        rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        rootLayout.setOnClickListener(this);
        seekbarYDetal.setOnSeekBarChangeListener(new YDeltaSeekBarListener());

        msgid = UUID.randomUUID().toString();
        isInComingCall = getIntent().getBooleanExtra("isComingCall", false);
        username = getIntent().getStringExtra("username");

        tvNick.setText(username);

        localSurface.setZOrderMediaOverlay(true);
        localSurface.setZOrderOnTop(true);

        // set call state listener
        addCallStateListener();
        if(!isInComingCall){ //outgoing call
            soundPool = new SoundPool(1, AudioManager.STREAM_RING,0);
            outgoing = soundPool.load(this,R.raw.em_outgoing,1);

            llComingCall.setVisibility(View.INVISIBLE);
            btnHangupCall.setVisibility(View.VISIBLE);
            String str = getResources().getString(R.string.Are_connected_to_each_other);
            tvCallState.setText(str);
            EMClient.getInstance().callManager().setSurfaceView(localSurface,oppositeSurface);
            handler.sendEmptyMessage(MSG_CALL_MAKE_VIDEO);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    streamID = playMakeCallSounds();
                }
            },300);
        }else{ // incoming call
            if(EMClient.getInstance().callManager().getCallState() == EMCallStateChangeListener.CallState.IDLE
                    || EMClient.getInstance().callManager().getCallState() == EMCallStateChangeListener.CallState.DISCONNECTED){
                // the call has ended
                finish();
                return;
            }
            llVoiceControl.setVisibility(View.INVISIBLE);
            localSurface.setVisibility(View.INVISIBLE);
            Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone = RingtoneManager.getRingtone(this,ringUri);
            ringtone.play();
            EMClient.getInstance().callManager().setSurfaceView(localSurface,oppositeSurface);
        }

        final int MAKE_CALL_TIMEOUT = 50*1000;
        handler.removeCallbacks(timeoutHangup);
        handler.postDelayed(timeoutHangup,MAKE_CALL_TIMEOUT);

        // get instance of call helper, should be called after setSurfaceView was called
        callHelper = EMClient.getInstance().callManager().getVideoCallHelper();

        /**
         * This function is only meaningful when your app need recording
         * If not, remove it.
         * This function need be called before the video stream started, so we set it in onCreate function.
         * This method will set the preferred video record encoding codec.
         * Using default encoding format, recorded file may not be played by mobile player.
         */
//        callHelper.setPreferMovFormatEnable(true);

        EMClient.getInstance().callManager().setCameraDataProcessor(brightnessData);
    }

    private void addCallStateListener() {
        callStateListener = new EMCallStateChangeListener() {
            @Override
            public void onCallStateChanged(final CallState callState, final CallError callError) {
                switch (callState){
                    case CONNECTING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvCallState.setText(R.string.Are_connected_to_each_other);
                            }
                        });
                        break;
                    case CONNECTED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvCallState.setText(R.string.have_connected_with);
                            }
                        });
                        break;
                    case ACCEPTED:
                        handler.removeCallbacks(timeoutHangup);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if(soundPool != null){
                                        soundPool.stop(streamID);
                                    }
                                }catch (Exception e){
                                    Log.e("videocall","closesoundpool"+e.getMessage());
                                }
                                openSpeakerOn();
                                tvIsP2p.setText(EMClient.getInstance().callManager().isDirectCall()?R.string.direct_call:R.string.relay_call);
                                ivHandsfree.setImageResource(R.drawable.em_icon_speaker_on);
                                isHandsfreeState = true;
                                isInCalling = true;
                                callingState = CallingState.NORMAL;
                                chronometer.setVisibility(View.VISIBLE);
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                chronometer.start();
                                tvNick.setVisibility(View.VISIBLE);
                                tvCallState.setText(R.string.In_the_call);
                            }
                        });
                        break;
                    case NETWORK_DISCONNECTED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvNetworkStatus.setVisibility(View.VISIBLE);
                                tvNetworkStatus.setText(R.string.network_isnot_available);
                            }
                        });
                        break;
                    case NETWORK_UNSTABLE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvNetworkStatus.setVisibility(View.VISIBLE);
                                if(callError == CallError.ERROR_NO_DATA){
                                    tvNetworkStatus.setText(R.string.no_call_data);
                                }else{
                                    tvNetworkStatus.setText(R.string.network_unstable);
                                }
                            }
                        });
                        break;
                    case NETWORK_NORMAL:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tvNetworkStatus.setVisibility(View.INVISIBLE);
                            }
                        });
                        break;
                    case VIDEO_PAUSE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VIDEO_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VIDEO_RESUME:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VIDEO_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_PAUSE:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_RESUME:
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case DISCONNECTED:
                        handler.removeCallbacks(timeoutHangup);
                        final CallError error = callError;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chronometer.stop();
                                callDruationText = chronometer.getText().toString();
                                String s1 = getResources().getString(R.string.The_other_party_refused_to_accept);
                                String s2 = getResources().getString(R.string.Connection_failure);
                                String s3 = getResources().getString(R.string.The_other_party_is_not_online);
                                String s4 = getResources().getString(R.string.The_other_is_on_the_phone_please);
                                String s5 = getResources().getString(R.string.The_other_party_did_not_answer);

                                String s6 = getResources().getString(R.string.hang_up);
                                String s7 = getResources().getString(R.string.The_other_is_hang_up);
                                String s8 = getResources().getString(R.string.did_not_answer);
                                String s9 = getResources().getString(R.string.Has_been_cancelled);
                                String s10 = getResources().getString(R.string.Refused);

                                if (error == CallError.REJECTED) {
                                    callingState = CallActivity.CallingState.BEREFUSED;
                                    tvCallState.setText(s1);
                                } else if (error == CallError.ERROR_TRANSPORT) {
                                    tvCallState.setText(s2);
                                } else if (error == CallError.ERROR_UNAVAILABLE) {
                                    callingState = CallActivity.CallingState.OFFLINE;
                                    tvCallState.setText(s3);
                                } else if (error == CallError.ERROR_BUSY) {
                                    callingState = CallActivity.CallingState.BUSY;
                                    tvCallState.setText(s4);
                                } else if (error == CallError.ERROR_NORESPONSE) {
                                    callingState = CallActivity.CallingState.NO_RESPONSE;
                                    tvCallState.setText(s5);
                                }else if (error == CallError.ERROR_LOCAL_SDK_VERSION_OUTDATED || error == CallError.ERROR_REMOTE_SDK_VERSION_OUTDATED){
                                    callingState = CallActivity.CallingState.VERSION_NOT_SAME;
                                    tvCallState.setText(R.string.call_version_inconsistent);
                                } else {
                                    if (isRefused) {
                                        callingState = CallActivity.CallingState.REFUSED;
                                        tvCallState.setText(s10);
                                    }
                                    else if (isAnswered) {
                                        callingState = CallActivity.CallingState.NORMAL;
                                        if (endCallTriggerByMe) {
//                                        tvCallState.setText(s6);
                                        } else {
                                            tvCallState.setText(s7);
                                        }
                                    } else {
                                        if (isInComingCall) {
                                            callingState = CallActivity.CallingState.UNANSWERED;
                                            tvCallState.setText(s8);
                                        } else {
                                            if (callingState != CallActivity.CallingState.NORMAL) {
                                                callingState = CallActivity.CallingState.CANCELLED;
                                                tvCallState.setText(s9);
                                            } else {
                                                tvCallState.setText(s6);
                                            }
                                        }
                                    }
                                }
                                postDelayedCloseMsg();
                            }
                        });
                        break;
                    default:
                        break;
                }
            }
        };
        EMClient.getInstance().callManager().addCallStateChangeListener(callStateListener);
    }

    void postDelayedCloseMsg(){
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeCallStateListener();
                saveCallRecord();
                AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
                animation.setDuration(1200);
                rootLayout.startAnimation(animation);
                finish();
            }
        }, 200);
    }

    void removeCallStateListener(){
        EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
    }

    @Override
    protected void onDestroy() {
        CustomerHelper.getCustomerhelper().isVideoCalling = false;
        if(isRecording){
            callHelper.stopVideoRecord();
            isRecording = false;
        }
        localSurface.getRenderer().dispose();
        localSurface = null;
        oppositeSurface.getRenderer().dispose();
        oppositeSurface = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this).setMessage("确定要结束会话？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        callingState = CallingState.CANCELLED;
                        postDelayedCloseMsg();
                        callDruationText = chronometer.getText().toString();
                        //super.onBackPressed();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
        //super.onBackPressed();
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if(isInCalling){
            try {
                EMClient.getInstance().callManager().pauseVideoTransfer();
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isInCalling){
            try {
                EMClient.getInstance().callManager().resumeVideoTransfer();
            } catch (HyphenateException e) {
                e.printStackTrace();
            }
        }
    }

    // dynamic adjust brightness
    public static class BrightnessDataProcess implements EMCallManager.EMCameraDataProcessor {
        byte yDelta = 0;

        synchronized void setYDelta(byte yDelta) {
            Log.d("VideoCallActivity", "brigntness uDelta:" + yDelta);
            this.yDelta = yDelta;
        }

        // data size is width*height*2
        // the first width*height is Y, second part is UV
        // the storage layout detailed please refer 2.x demo CameraHelper.onPreviewFrame
        @Override
        public synchronized void onProcessData(byte[] data, Camera camera, final int width, final int height, final int rotateAngel) {
            int wh = width * height;
            for (int i = 0; i < wh; i++) {
                int d = (data[i] & 0xFF) + yDelta;
                d = d < 16 ? 16 : d;
                d = d > 235 ? 235 : d;
                data[i] = (byte) d;
            }
        }
    }

    class YDeltaSeekBarListener implements SeekBar.OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            brightnessData.setYDelta((byte)(20.0f * (progress - 50) / 50.0f));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
