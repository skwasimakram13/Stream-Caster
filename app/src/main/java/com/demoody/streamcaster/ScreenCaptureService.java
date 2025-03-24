package com.demoody.streamcaster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.webrtc.*;

import java.util.Collections;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "ScreenShareService";

    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoCapturer videoCapturer;
    private EglBase eglBase;
    private VideoTrack videoTrack;

    private Intent screenCaptureIntent;
    private int screenCaptureResultCode;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, getNotification());
        Log.d(TAG, "ScreenCaptureService Created.");

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaProjection != null) {
            Log.w(TAG, "Service already running! Ignoring duplicate start request.");
            return START_NOT_STICKY;
        }

        if (intent == null) {
            Log.e(TAG, "Received null intent, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        screenCaptureResultCode = intent.getIntExtra("resultCode", -1);
        screenCaptureIntent = intent.getParcelableExtra("data");

        if (screenCaptureResultCode == -1 || screenCaptureIntent == null) {
            Log.e(TAG, "Invalid MediaProjection data.");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mediaProjectionManager == null) {
            Log.e(TAG, "MediaProjectionManager is null, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        mediaProjection = mediaProjectionManager.getMediaProjection(screenCaptureResultCode, screenCaptureIntent);

        if (mediaProjection == null) {
            Log.e(TAG, "Failed to obtain MediaProjection.");
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.d(TAG, "MediaProjection obtained successfully, starting screen capture.");
        startScreenCapture();

        return START_STICKY;
    }

    private void startScreenCapture() {
        if (mediaProjection == null) {
            Log.e(TAG, "Failed to get MediaProjection.");
            stopSelf();
            return;
        }
        startWebRTCStreaming();
    }

    private void startWebRTCStreaming() {
        Log.d(TAG, "Starting WebRTC Streaming...");

        eglBase = EglBase.create();
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .createInitializationOptions());

        peerConnectionFactory = PeerConnectionFactory.builder()
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .createPeerConnectionFactory();

        videoCapturer = new ScreenCapturerAndroid(screenCaptureIntent, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "MediaProjection stopped.");
                stopSelf();
            }
        });

        SurfaceTextureHelper textureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.getEglBaseContext());
        VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
        videoCapturer.initialize(textureHelper, this, videoSource.getCapturerObserver());
        videoCapturer.startCapture(1280, 720, 30);

        videoTrack = peerConnectionFactory.createVideoTrack("screenTrack", videoSource);

        setupPeerConnection();
    }

    private void setupPeerConnection() {
        Log.d(TAG, "Setting up WebRTC PeerConnection...");

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(Collections.emptyList());
        rtcConfig.iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "ICE Candidate: " + iceCandidate.sdp);
            }

            @Override public void onSignalingChange(PeerConnection.SignalingState signalingState) { }
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) { }
            @Override public void onIceConnectionReceivingChange(boolean b) { }
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) { }
            @Override public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) { }
            @Override public void onAddStream(MediaStream mediaStream) { }
            @Override public void onRemoveStream(MediaStream mediaStream) { }
            @Override public void onDataChannel(DataChannel dataChannel) { }
            @Override public void onRenegotiationNeeded() { }
        });
    }

    private Notification getNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Sharing Active")
                .setContentText("Your screen is being shared.")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "Screen Sharing Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping video capturer", e);
            }
            videoCapturer.dispose();
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory.dispose();
        }
        if (eglBase != null) {
            eglBase.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.d(TAG, "ScreenCaptureService Destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
