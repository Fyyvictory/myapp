package com.example.imdemo.ui;

import android.media.AudioManager;
import android.media.Ringtone;
import android.media.SoundPool;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.imdemo.R;
import com.example.imdemo.entities.Constant;
import com.hyphenate.EMCallBack;
import com.hyphenate.EMError;
import com.hyphenate.chat.EMCallManager;
import com.hyphenate.chat.EMCallStateChangeListener;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMConversation;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chat.EMTextMessageBody;
import com.hyphenate.exceptions.EMNoActiveCallException;
import com.hyphenate.exceptions.EMServiceNotReadyException;
import com.hyphenate.media.EMLocalSurfaceView;
import com.hyphenate.media.EMOppositeSurfaceView;
import com.hyphenate.util.EMLog;

public class CallActivity extends BaseActivity {

    public final static String TAG = "CallActivity";
    protected final int MSG_CALL_MAKE_VIDEO = 0;
    protected final int MSG_CALL_MAKE_VOICE = 1;
    protected final int MSG_CALL_ANSWER = 2;
    protected final int MSG_CALL_REJECT = 3;
    protected final int MSG_CALL_END = 4;
    protected final int MSG_CALL_RLEASE_HANDLER = 5;
    protected final int MSG_CALL_SWITCH_CAMERA = 6;

    protected boolean isInComingCall;
    protected boolean isRefused = false;
    protected String username;
    protected CallActivity.CallingState callingState = CallingState.CANCELLED;
    protected String callDruationText;
    protected String msgid;
    protected AudioManager audioManager;
    protected SoundPool soundPool;
    protected Ringtone ringtone;
    protected int outgoing;
    protected EMCallStateChangeListener callStateListener;
    protected EMLocalSurfaceView localSurface;
    protected EMOppositeSurfaceView oppositeSurface;
    protected boolean isAnswered = false;
    protected int streamID = -1;

    EMCallManager.EMCallPushProvider pushProvider;

    protected int callType = 0; // 0:voice call 1: video call

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_call);
        audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
        pushProvider = new EMCallManager.EMCallPushProvider() {

            void updateMessageText(EMMessage oldMessage, String to) {
                EMConversation conversation = EMClient.getInstance().chatManager().getConversation(oldMessage.getTo());
                conversation.removeMessage(oldMessage.getMsgId());
            }

            @Override
            public void onRemoteOffline(final String s) {
                EMLog.d(TAG, "onremoteoffline,to" + s);
                final EMMessage emMessage = EMMessage.createTxtSendMessage("you have an income message", s);
                emMessage.setAttribute("em_apns_ext", true);
                emMessage.setAttribute("is_voice_call", callType == 0 ? true : false);
                emMessage.setMessageStatusCallback(new EMCallBack() {
                    @Override
                    public void onSuccess() {
                        EMLog.d(TAG, "onremoteoffline sucess");
                        updateMessageText(emMessage, s);
                    }

                    @Override
                    public void onError(int i, String s) {
                        EMLog.d(TAG, "onRemoteOffline Error");
                        updateMessageText(emMessage, s);
                    }

                    @Override
                    public void onProgress(int i, String s) {

                    }
                });
                // send message
                EMClient.getInstance().chatManager().sendMessage(emMessage);
            }
        };
        EMClient.getInstance().callManager().setPushProvider(pushProvider);
    }

    @Override
    protected void onDestroy() {
        if (soundPool != null) {
            soundPool.release();
        }
        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setMicrophoneMute(false);
        if (callStateListener != null) {
            EMClient.getInstance().callManager().removeCallStateChangeListener(callStateListener);
        }
        if (pushProvider != null) {
            EMClient.getInstance().callManager().setPushProvider(null);
            pushProvider = null;
        }
        releaseHandler();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        handler.sendEmptyMessage(MSG_CALL_END);
        saveCallRecord();
        finish();
        super.onBackPressed();
    }

    public void saveCallRecord() {
        @SuppressWarnings("UnusedAssignment") EMMessage message = null;
        @SuppressWarnings("UnusedAssignment") EMTextMessageBody txtBody = null;
        if(!isInComingCall){
            message = EMMessage.createSendMessage(EMMessage.Type.TXT);
            message.setReceipt(username);
        }else{
            message = EMMessage.createReceiveMessage(EMMessage.Type.TXT);
            message.setFrom(username);
        }

        String st1 = getResources().getString(R.string.call_duration);
        String st2 = getResources().getString(R.string.Refused);
        String st3 = getResources().getString(R.string.The_other_party_has_refused_to);
        String st4 = getResources().getString(R.string.The_other_is_not_online);
        String st5 = getResources().getString(R.string.The_other_is_on_the_phone);
        String st6 = getResources().getString(R.string.The_other_party_did_not_answer);
        String st7 = getResources().getString(R.string.did_not_answer);
        String st8 = getResources().getString(R.string.Has_been_cancelled);

        switch (callingState) {
            case NORMAL:
                txtBody = new EMTextMessageBody(st1 + callDruationText);
                break;
            case REFUSED:
                txtBody = new EMTextMessageBody(st2);
                break;
            case BEREFUSED:
                txtBody = new EMTextMessageBody(st3);
                break;
            case OFFLINE:
                txtBody = new EMTextMessageBody(st4);
                break;
            case BUSY:
                txtBody = new EMTextMessageBody(st5);
                break;
            case NO_RESPONSE:
                txtBody = new EMTextMessageBody(st6);
                break;
            case UNANSWERED:
                txtBody = new EMTextMessageBody(st7);
                break;
            case VERSION_NOT_SAME:
                txtBody = new EMTextMessageBody(getString(R.string.call_version_inconsistent));
                break;
            default:
                txtBody = new EMTextMessageBody(st8);
                break;
        }
        // set message extension
        if(callType == 0){
            message.setAttribute(Constant.MESSAGE_ATTR_IS_VOICE_CALL,true);
        }else{
            message.setAttribute(Constant.MESSAGE_ATTR_IS_VIDEO_CALL,true);
        }
        //set message body
        message.addBody(txtBody);
        message.setMsgId(msgid);
        message.setStatus(EMMessage.Status.SUCCESS);
        EMClient.getInstance().chatManager().saveMessage(message);
    }

    Runnable timeoutHangup = new Runnable() {
        @Override
        public void run() {
            handler.sendEmptyMessage(MSG_CALL_END);
        }
    };

    HandlerThread callHandlerThread = new HandlerThread("callHandlerThread");

    {
        callHandlerThread.start();
    }

    protected Handler handler = new Handler(callHandlerThread.getLooper()) {
        @Override
        public void handleMessage(Message msg) {
            //super.handleMessage(msg);
            EMLog.d("EMCallManager CallActivity", "handleMessage ---enter block--- msg.what:" + msg.what);
            switch (msg.what) {
                case MSG_CALL_MAKE_VIDEO:
                case MSG_CALL_MAKE_VOICE:
                    try {
                        if (msg.what == MSG_CALL_MAKE_VIDEO) {
                            EMClient.getInstance().callManager().makeVideoCall(username);
                        } else {
                            EMClient.getInstance().callManager().makeVoiceCall(username);
                        }
                    } catch (final EMServiceNotReadyException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String message = e.getMessage();
                                if (e.getErrorCode() == EMError.CALL_REMOTE_OFFLINE) {
                                    message = getResources().getString(R.string.The_other_is_not_online);
                                } else if (e.getErrorCode() == EMError.USER_NOT_LOGIN) {
                                    message = getResources().getString(R.string.Is_not_yet_connected_to_the_server);
                                } else if (e.getErrorCode() == EMError.INVALID_USER_NAME) {
                                    message = getResources().getString(R.string.illegal_user_name);
                                } else if (e.getErrorCode() == EMError.CALL_BUSY) {
                                    message = getResources().getString(R.string.The_other_is_on_the_phone);
                                } else if (e.getErrorCode() == EMError.NETWORK_ERROR) {
                                    message = getResources().getString(R.string.can_not_connect_chat_server_connection);
                                }
                                Toast.makeText(CallActivity.this, message, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                    break;
                case MSG_CALL_ANSWER:
                    EMLog.d(TAG,"MSG_CALL_ANSWER");
                    if(ringtone != null){
                        ringtone.stop();
                    }
                    if(isInComingCall){
                        try {
                            EMClient.getInstance().callManager().answerCall();
                            isAnswered = true;
                        } catch (EMNoActiveCallException e) {
                            e.printStackTrace();
                            saveCallRecord();
                            finish();
                        }
                    }else{
                        EMLog.d(TAG,"answer call isIncomingCall:false");
                    }
                    break;
                case MSG_CALL_REJECT:
                    if(ringtone != null){
                        ringtone.stop();
                    }
                    try {
                        EMClient.getInstance().callManager().rejectCall();
                    } catch (EMNoActiveCallException e) {
                        e.printStackTrace();
                        saveCallRecord();
                        finish();
                    }
                    break;
                case MSG_CALL_END:
                    if (soundPool != null)
                        soundPool.stop(streamID);
                    try {
                        EMClient.getInstance().callManager().endCall();
                    } catch (Exception e) {
                        e.printStackTrace();
                        saveCallRecord();
                        finish();
                    }
                    break;
                case MSG_CALL_RLEASE_HANDLER:
                    try {
                        EMClient.getInstance().callManager().endCall();
                    } catch (EMNoActiveCallException e) {
                        e.printStackTrace();
                    }
                    handler.removeCallbacks(timeoutHangup);
                    handler.removeMessages(MSG_CALL_MAKE_VIDEO);
                    handler.removeMessages(MSG_CALL_MAKE_VOICE);
                    handler.removeMessages(MSG_CALL_ANSWER);
                    handler.removeMessages(MSG_CALL_REJECT);
                    handler.removeMessages(MSG_CALL_END);
                    callHandlerThread.quit();
                    break;
                case MSG_CALL_SWITCH_CAMERA:
                    EMClient.getInstance().callManager().switchCamera();
                    break;
                default:
                    break;
            }
            EMLog.d("EMCallManager CallActivity","handleMessage ---exit block--- msg.what:" + msg.what);
        }
    };

    void releaseHandler() {
        handler.sendEmptyMessage(MSG_CALL_RLEASE_HANDLER);
    }

    protected int playMakeCallSounds(){
        try {
            audioManager.setMode(AudioManager.MODE_RINGTONE);
            audioManager.setSpeakerphoneOn(false);
            int id = soundPool.play(outgoing,0.3f,
                    0.3f,
                    1,
                    -1, // loop，0 is no loop，-1 is loop forever
                    1);
            return id;
        }catch (Exception e){
         return -1;
        }
    }

    protected void openSpeakerOn(){
        try {
            if(!audioManager.isSpeakerphoneOn()){
                audioManager.setSpeakerphoneOn(true);
            }
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }catch (Exception e){
        }
    }

    protected void closeSpeakerOn(){
        try {
            if (audioManager != null){
                if (audioManager.isSpeakerphoneOn()){
                    audioManager.setSpeakerphoneOn(false);
                }
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            }
        }catch (Exception e){
        }
    }
    enum CallingState {
        CANCELLED, NORMAL, REFUSED, BEREFUSED, UNANSWERED, OFFLINE, NO_RESPONSE, BUSY, VERSION_NOT_SAME
    }
}
