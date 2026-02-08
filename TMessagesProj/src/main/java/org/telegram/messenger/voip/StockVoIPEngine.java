package org.telegram.messenger.voip;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.webrtc.VideoSink;

public class StockVoIPEngine implements VoIPEngine {

    private NativeInstance nativeInstance;
    private final VoIPService service;

    public StockVoIPEngine(VoIPService service) {
        this.service = service;
    }

    @Override
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private Callback callback;

    @Override
    public void start(String version, Instance.Config config, String persistentStateFilePath, Instance.Endpoint[] endpoints, Instance.Proxy proxy, int networkType, Instance.EncryptionKey encryptionKey, VideoSink remoteSink, long videoCapturer) {
        nativeInstance = Instance.makeInstance(version, config, persistentStateFilePath, endpoints, proxy, networkType, encryptionKey, remoteSink, videoCapturer, (uids, levels, voice) -> {
            if (callback != null) callback.onAudioLevelsUpdated(uids, levels, voice);
        });

        if (nativeInstance != null) {
            nativeInstance.setOnStateUpdatedListener((state, inTransition) -> {
                if (callback != null) callback.onStateUpdated(state, inTransition);
            });
            nativeInstance.setOnSignalBarsUpdatedListener(signalBars -> {
                if (callback != null) callback.onSignalBarsUpdated(signalBars);
            });
            nativeInstance.setOnSignalDataListener(data -> {
                if (callback != null) callback.onSignalData(data);
            });
            nativeInstance.setOnRemoteMediaStateUpdatedListener((audioState, videoState) -> {
                if (callback != null) callback.onRemoteMediaStateUpdated(audioState, videoState);
            });
        }
    }

    @Override
    public void startGroup(String logPath, long videoCapturer, boolean screencast, boolean noiseSupression, NativeInstance.PayloadCallback payloadCallback, NativeInstance.AudioLevelsCallback audioLevelsCallback, NativeInstance.VideoSourcesCallback unknownParticipantsCallback, NativeInstance.RequestBroadcastPartCallback requestBroadcastPartCallback, NativeInstance.RequestBroadcastPartCallback cancelRequestBroadcastPartCallback, NativeInstance.RequestCurrentTimeCallback requestCurrentTimeCallback, boolean isConference) {
        nativeInstance = NativeInstance.makeGroup(logPath, videoCapturer, screencast, noiseSupression, payloadCallback,
                audioLevelsCallback != null ? audioLevelsCallback : (uids, levels, voice) -> {
                    if (callback != null) callback.onAudioLevelsUpdated(uids, levels, voice);
                },
                unknownParticipantsCallback, requestBroadcastPartCallback, cancelRequestBroadcastPartCallback, requestCurrentTimeCallback, isConference);
        if (nativeInstance != null) {
            nativeInstance.setOnStateUpdatedListener((state, inTransition) -> {
                if (callback != null) callback.onStateUpdated(state, inTransition);
            });
        }
    }

    @Override
    public Instance.FinalState stop() {
        if (nativeInstance != null) {
            Instance.FinalState state = null;
            if (isGroup()) {
                nativeInstance.stopGroup();
            } else {
                state = nativeInstance.stop();
            }
            nativeInstance = null;
            return state;
        }
        return null;
    }

    @Override
    public void setMuteMicrophone(boolean mute) {
        if (nativeInstance != null) nativeInstance.setMuteMicrophone(mute);
    }

    @Override
    public void setAudioOutputGainControlEnabled(boolean enabled) {
        if (nativeInstance != null) nativeInstance.setAudioOutputGainControlEnabled(enabled);
    }

    @Override
    public void setEchoCancellationStrength(int strength) {
        if (nativeInstance != null) nativeInstance.setEchoCancellationStrength(strength);
    }

    @Override
    public void setVolume(int ssrc, double volume) {
        if (nativeInstance != null) nativeInstance.setVolume(ssrc, volume);
    }

    @Override
    public void setVideoState(int videoState) {
        if (nativeInstance != null) nativeInstance.setVideoState(videoState);
    }

    @Override
    public void switchCamera(boolean front) {
        if (nativeInstance != null) nativeInstance.switchCamera(front);
    }

    @Override
    public void setupOutgoingVideo(VideoSink localSink, int type) {
        if (nativeInstance != null) nativeInstance.setupOutgoingVideo(localSink, type);
    }

    @Override
    public void setVideoSink(VideoSink remoteSink) {
    }

    @Override
    public void onSignalingDataReceive(byte[] data) {
        if (nativeInstance != null) nativeInstance.onSignalingDataReceive(data);
    }

    @Override
    public void setNetworkType(int networkType) {
        if (nativeInstance != null) nativeInstance.setNetworkType(networkType);
    }

    @Override
    public long getPreferredRelayId() {
        return nativeInstance != null ? nativeInstance.getPreferredRelayId() : 0;
    }

    @Override
    public String getLastError() {
        return nativeInstance != null ? nativeInstance.getLastError() : Instance.ERROR_UNKNOWN;
    }

    @Override
    public String getDebugInfo() {
        return nativeInstance != null ? nativeInstance.getDebugInfo() : "";
    }

    @Override
    public Instance.TrafficStats getTrafficStats() {
        return nativeInstance != null ? nativeInstance.getTrafficStats() : null;
    }

    @Override
    public boolean isGroup() {
        return nativeInstance != null && nativeInstance.isGroup();
    }

    public NativeInstance getNativeInstance() {
        return nativeInstance;
    }

    @Override
    public void onMediaDescriptionAvailable(long taskPtr, VoIPService.RequestedParticipant[] participants) {
        if (nativeInstance != null) nativeInstance.onMediaDescriptionAvailable(taskPtr, participants);
    }

    @Override
    public void setNoiseSuppressionEnabled(boolean value) {
        if (nativeInstance != null) nativeInstance.setNoiseSuppressionEnabled(value);
    }

    @Override
    public void activateVideoCapturer(long videoCapturer) {
        if (nativeInstance != null) nativeInstance.activateVideoCapturer(videoCapturer);
    }

    @Override
    public void clearVideoCapturer() {
        if (nativeInstance != null) nativeInstance.clearVideoCapturer();
    }

    @Override
    public long addIncomingVideoOutput(int quality, String endpointId, NativeInstance.SsrcGroup[] ssrcGroups, VideoSink remoteSink, long userId) {
        return nativeInstance != null ? nativeInstance.addIncomingVideoOutput(quality, endpointId, ssrcGroups, remoteSink, userId) : 0;
    }

    @Override
    public void removeIncomingVideoOutput(long nativeRemoteSink) {
        if (nativeInstance != null) nativeInstance.removeIncomingVideoOutput(nativeRemoteSink);
    }

    @Override
    public void setVideoEndpointQuality(String endpointId, int quality) {
        if (nativeInstance != null) nativeInstance.setVideoEndpointQuality(endpointId, quality);
    }

    @Override
    public void setGlobalServerConfig(String serverConfigJson) {
        if (nativeInstance != null) nativeInstance.setGlobalServerConfig(serverConfigJson);
    }

    @Override
    public void setBufferSize(int size) {
        if (nativeInstance != null) nativeInstance.setBufferSize(size);
    }

    @Override
    public String getVersion() {
        return nativeInstance != null ? nativeInstance.getVersion() : "";
    }

    @Override
    public void setJoinResponsePayload(String payload) {
        if (nativeInstance != null) nativeInstance.setJoinResponsePayload(payload);
    }

    @Override
    public void prepareForStream(boolean isRtpStream) {
        if (nativeInstance != null) nativeInstance.prepareForStream(isRtpStream);
    }

    @Override
    public void resetGroupInstance(boolean set, boolean disconnect) {
        if (nativeInstance != null) nativeInstance.resetGroupInstance(set, disconnect);
    }

    @Override
    public void onStreamPartAvailable(long ts, java.nio.ByteBuffer buffer, int size, long timestamp, int videoChannel, int quality) {
        if (nativeInstance != null) nativeInstance.onStreamPartAvailable(ts, buffer, size, timestamp, videoChannel, quality);
    }

    @Override
    public boolean hasVideoCapturer() {
        return nativeInstance != null && nativeInstance.hasVideoCapturer();
    }

    @Override
    public void onRequestTimeComplete(long taskPtr, long time) {
        if (nativeInstance != null) nativeInstance.onRequestTimeComplete(taskPtr, time);
    }

    @Override
    public void setConferenceCallId(long call_id) {
        if (nativeInstance != null) nativeInstance.setConferenceCallId(call_id);
    }

    @Override
    public void setupOutgoingVideoCreated(long videoCapturer) {
        if (nativeInstance != null) nativeInstance.setupOutgoingVideoCreated(videoCapturer);
    }
}
