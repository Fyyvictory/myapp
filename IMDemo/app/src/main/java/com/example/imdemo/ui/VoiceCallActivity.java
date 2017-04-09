package com.example.imdemo.ui;

import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.CustomerHelper;
import com.example.imdemo.R;
import com.example.imdemo.widget.MyChronometer;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.exceptions.HyphenateException;
import com.hyphenate.util.EMLog;

import java.util.UUID;


public class VoiceCallActivity extends CallActivity implements View.OnClickListener {

    protected TextView textCallstate;
    protected MyChronometer chronometer;
    protected TextView tvIsP2p;
    protected TextView tvCallingdur;
    protected LinearLayout toplayout;
    protected ImageView swingCard;
    protected TextView tvNick;
    protected TextView tvNetworkStatus;
    protected ImageView ivMute;
    protected ImageView ivHandsfree;
    protected LinearLayout llVoiceControl;
    protected Button btnHangupCall;
    protected Button btnRefuseCall;
    protected Button btnAnswerCall;
    protected LinearLayout llComingCall;
    protected LinearLayout activityVoiceCall;
    String str3;
    private boolean isHandsFreeState;
    protected CallingState callingState = CallingState.CANCELLED;
    private boolean endCallTriggerByMe = false;
    private boolean isMuteState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            finish();
            return;
        }
        super.setContentView(R.layout.activity_voice_call);
        CustomerHelper.getCustomerhelper().isVoiceCalling = true;
        callType = 0;
        initView();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        addCallStateListener();
        msgid = UUID.randomUUID().toString();

        username = getIntent().getStringExtra("username");
        isInComingCall = getIntent().getBooleanExtra("isComingCall", false);
        tvNick.setText(username);
        // 继承了callactivity，这些共有的如铃声，timeout的事件，handler的处理都写到父类
        if (!isInComingCall) {  // outgoingcall
            soundPool = new SoundPool(1, AudioManager.STREAM_RING, 0);
            outgoing = soundPool.load(this, R.raw.em_outgoing, 1);
            llComingCall.setVisibility(View.INVISIBLE);
            btnHangupCall.setVisibility(View.VISIBLE);
            str3 = getResources().getString(R.string.Are_connected_to_each_other);
            textCallstate.setText(str3);
            handler.sendEmptyMessage(MSG_CALL_MAKE_VOICE);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    streamID = playMakeCallSounds();
                }
            }, 300);
        } else {  // incoming call
            llVoiceControl.setVisibility(View.VISIBLE);
            Uri ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(true);
            ringtone = RingtoneManager.getRingtone(this, ringUri);
            ringtone.play();
        }
        final int MAKE_CALL_TIMEOUT = 50 * 1000;
        handler.removeCallbacks(timeoutHangup);
        handler.postDelayed(timeoutHangup, MAKE_CALL_TIMEOUT);
    }

    private void addCallStateListener() {
        callStateListener = new EMCallStateChangeListener() {
            @Override
            public void onCallStateChanged(CallState callState, final CallError callError) {
                EMLog.d("EMcallManager", "oncallstatechanged");
                switch (callState) {
                    case CONNECTED:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String str1 = getResources().getString(R.string.have_connected_with);
                                textCallstate.setText(str1);
                            }
                        });
                        break;
                    case CONNECTING:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textCallstate.setText(str3);
                            }
                        });
                        break;
                    case ACCEPTED:
                        handler.removeCallbacks(timeoutHangup);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (soundPool != null) {
                                        soundPool.stop(streamID);
                                    }
                                } catch (Exception e) {
                                    EMLog.e("stopsoundpool", e.getMessage());
                                }
                                if (!isHandsFreeState)
                                    closeSpeakerOn();
                                tvIsP2p.setText(getResources().getText(EMClient.getInstance().callManager().isDirectCall() ? R.string.direct_call : R.string.relay_call));
                                chronometer.setVisibility(View.VISIBLE);
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                chronometer.start();
                                String str2 = getResources().getString(R.string.In_the_call);
                                textCallstate.setText(str2);
                                callingState = CallingState.NORMAL;
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
                                if (callError == CallError.ERROR_NO_DATA) {
                                    tvNetworkStatus.setText(R.string.no_call_data);
                                } else {
                                    tvNetworkStatus.setText(R.string.network_unstable);
                                }
                            }
                        });
                        break;
                    case NETWORK_NORMAL:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvNetworkStatus.setVisibility(View.INVISIBLE);
                            }
                        });
                        break;
                    case VOICE_PAUSE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_PAUSE", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case VOICE_RESUME:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "VOICE_RESUME", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    case DISCONNECTED:  // call is disconnected
                        handler.removeCallbacks(timeoutHangup);
                        final CallError error = callError;
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chronometer.stop();
                                callDruationText = chronometer.getText().toString();

                                String st1 = getResources().getString(R.string.Refused);
                                String st2 = getResources().getString(R.string.The_other_party_refused_to_accept);
                                String st3 = getResources().getString(R.string.Connection_failure);
                                String st4 = getResources().getString(R.string.The_other_party_is_not_online);
                                String st5 = getResources().getString(R.string.The_other_is_on_the_phone_please);

                                String st6 = getResources().getString(R.string.The_other_party_did_not_answer_new);
                                String st7 = getResources().getString(R.string.hang_up);
                                String st8 = getResources().getString(R.string.The_other_is_hang_up);

                                String st9 = getResources().getString(R.string.did_not_answer);
                                String st10 = getResources().getString(R.string.Has_been_cancelled);
                                String st11 = getResources().getString(R.string.hang_up);

                                if (error == CallError.REJECTED) {
                                    callingState = CallActivity.CallingState.BEREFUSED;
                                    textCallstate.setText(st2);
                                } else if (error == CallError.ERROR_TRANSPORT) {
                                    textCallstate.setText(st3);
                                } else if (error == CallError.ERROR_UNAVAILABLE) {
                                    callingState = CallActivity.CallingState.OFFLINE;
                                    textCallstate.setText(st4);
                                } else if (error == CallError.ERROR_BUSY) {
                                    callingState = CallActivity.CallingState.BUSY;
                                    textCallstate.setText(st5);
                                } else if (error == CallError.ERROR_NORESPONSE) {
                                    callingState = CallActivity.CallingState.NO_RESPONSE;
                                    textCallstate.setText(st6);
                                } else if (error == CallError.ERROR_LOCAL_SDK_VERSION_OUTDATED || error == CallError.ERROR_REMOTE_SDK_VERSION_OUTDATED) {
                                    callingState = CallActivity.CallingState.VERSION_NOT_SAME;
                                    textCallstate.setText(R.string.call_version_inconsistent);
                                } else {
                                    if (isRefused) {
                                        callingState = CallActivity.CallingState.REFUSED;
                                        textCallstate.setText(st1);
                                    } else if (isAnswered) {
                                        callingState = CallActivity.CallingState.NORMAL;
                                        if (endCallTriggerByMe) {
//                                        callStateTextView.setText(st7);
                                        } else {
                                            textCallstate.setText(st8);
                                        }
                                    } else {
                                        if (isInComingCall) {
                                            callingState = CallActivity.CallingState.UNANSWERED;
                                            textCallstate.setText(st9);
                                        } else {
                                            if (callingState != CallActivity.CallingState.NORMAL) {
                                                callingState = CallActivity.CallingState.CANCELLED;
                                                textCallstate.setText(st10);
                                            } else {
                                                textCallstate.setText(st11);
                                            }
                                        }
                                    }
                                }
                                postDelayCloseMsg();
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

    private void postDelayCloseMsg() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        removeCallStateListener();
                        saveCallRecord();
                        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
                        animation.setDuration(800);
                        activityVoiceCall.startAnimation(animation);
                        finish();
                    }
                });
            }
        },200);
    }

    void removeCallStateListener() {
        EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_mute) {
            if (isMuteState) {
                ivMute.setImageResource(R.drawable.em_icon_mute_normal);
                try {
                    EMClient.getInstance().callManager().resumeVoiceTransfer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                isMuteState = false;
            } else {
                ivMute.setImageResource(R.drawable.em_icon_mute_on);
                try {
                    EMClient.getInstance().callManager().pauseVoiceTransfer();
                } catch (HyphenateException e) {
                    e.printStackTrace();
                }
                isMuteState = true;
            }
        } else if (view.getId() == R.id.iv_handsfree) {
            if (isHandsFreeState) {
                ivHandsfree.setImageResource(R.drawable.em_icon_speaker_normal);
                closeSpeakerOn();
                isHandsFreeState = false;
            } else {
                ivHandsfree.setImageResource(R.drawable.em_icon_speaker_on);
                openSpeakerOn();
                isHandsFreeState = true;
            }
        } else if (view.getId() == R.id.btn_hangup_call) {
            btnHangupCall.setEnabled(false);
            chronometer.stop();
            callDruationText = chronometer.getText().toString();
            endCallTriggerByMe = true;
            textCallstate.setText(R.string.hanging_up);
            handler.sendEmptyMessage(MSG_CALL_END);
        } else if (view.getId() == R.id.btn_refuse_call) {
            isRefused = true;
            btnRefuseCall.setEnabled(false);
            handler.sendEmptyMessage(MSG_CALL_REJECT);
        } else if (view.getId() == R.id.btn_answer_call) {
            btnAnswerCall.setEnabled(false);
            closeSpeakerOn();
            textCallstate.setText("正在接听");
            llComingCall.setVisibility(View.INVISIBLE);
            btnHangupCall.setVisibility(View.VISIBLE);
            llVoiceControl.setVisibility(View.VISIBLE);
            handler.sendEmptyMessage(MSG_CALL_ANSWER);
        }
    }

    private void initView() {
        textCallstate = (TextView) findViewById(R.id.text_callstate);
        chronometer = (MyChronometer) findViewById(R.id.chronometer);
        tvIsP2p = (TextView) findViewById(R.id.tv_is_p2p);
        tvCallingdur = (TextView) findViewById(R.id.tv_callingdur);
        toplayout = (LinearLayout) findViewById(R.id.toplayout);
        swingCard = (ImageView) findViewById(R.id.swing_card);
        tvNick = (TextView) findViewById(R.id.tv_nick);
        tvNetworkStatus = (TextView) findViewById(R.id.tv_network_status);
        ivMute = (ImageView) findViewById(R.id.iv_mute);
        ivMute.setOnClickListener(VoiceCallActivity.this);
        ivHandsfree = (ImageView) findViewById(R.id.iv_handsfree);
        ivHandsfree.setOnClickListener(VoiceCallActivity.this);
        llVoiceControl = (LinearLayout) findViewById(R.id.ll_voice_control);
        btnHangupCall = (Button) findViewById(R.id.btn_hangup_call);
        btnHangupCall.setOnClickListener(VoiceCallActivity.this);
        btnRefuseCall = (Button) findViewById(R.id.btn_refuse_call);
        btnRefuseCall.setOnClickListener(VoiceCallActivity.this);
        btnAnswerCall = (Button) findViewById(R.id.btn_answer_call);
        btnAnswerCall.setOnClickListener(VoiceCallActivity.this);
        llComingCall = (LinearLayout) findViewById(R.id.ll_coming_call);
        activityVoiceCall = (LinearLayout) findViewById(R.id.activity_voice_call);
    }

    @Override
    protected void onDestroy() {
        CustomerHelper.getCustomerhelper().isVoiceCalling = false;
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
                                callDruationText = chronometer.getText().toString();
                                postDelayCloseMsg();
                                //super.onBackPressed();
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
    }
}
