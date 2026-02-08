package org.telegram.messenger.voip;

import io.github.pytgcalls.NTgCalls;
import io.github.pytgcalls.NetworkInfo;
import io.github.pytgcalls.media.AudioDescription;
import io.github.pytgcalls.media.MediaDescription;
import io.github.pytgcalls.media.MediaSource;
import io.github.pytgcalls.media.StreamMode;
import io.github.pytgcalls.media.VideoDescription;
import io.github.pytgcalls.p2p.RTCServer;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.webrtc.VideoSink;

public class NTgCallsEngine implements VoIPEngine {

  private final VoIPService service;
  private Callback callback;
  private NTgCalls ntgCalls;
  private static final long CALL_ID = 0;

  private boolean isMicMuted = false;
  private boolean isVideoEnabled = false;
  private boolean isFrontCamera = true;
  private VideoSink remoteSink;

  public NTgCallsEngine(VoIPService service) {
    this.service = service;
  }

  @Override
  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  @Override
  public void start(
      String version,
      Instance.Config config,
      String persistentStateFilePath,
      Instance.Endpoint[] endpoints,
      Instance.Proxy proxy,
      int networkType,
      Instance.EncryptionKey encryptionKey,
      VideoSink remoteSink,
      long videoCapturer) {
    if (BuildVars.LOGS_ENABLED) {
      FileLog.d("NTgCallsEngine: start()");
    }

    try {
      ntgCalls = new NTgCalls();

      ntgCalls.setSignalingDataCallback(
          (callId, data) -> {
            if (callback != null) {
              callback.onSignalData(data);
            }
          });

      ntgCalls.setConnectionChangeCallback(
          (chatId, callNetworkState) -> {
            if (callback != null) {
              int state = mapNetworkState(callNetworkState.state);
              callback.onStateUpdated(state, false);
            }
          });

      if (remoteSink != null) {}

      ntgCalls.createP2PCall(CALL_ID);

      List<RTCServer> rtcServers = new ArrayList<>();
      if (endpoints != null) {
        for (Instance.Endpoint ep : endpoints) {
          rtcServers.add(
              new RTCServer(
                  ep.id,
                  ep.ipv4,
                  ep.ipv6,
                  ep.port,
                  ep.username,
                  ep.password,
                  ep.turn,
                  ep.stun,
                  ep.tcp,
                  ep.peerTag != null ? new String(ep.peerTag) : null));
        }
      }

      ntgCalls.skipExchange(CALL_ID, encryptionKey.value, encryptionKey.isOutgoing);

      List<String> libVersions = new ArrayList<>();
      if (version != null) libVersions.add(version);

      ntgCalls.connectP2P(CALL_ID, rtcServers, libVersions, config.enableP2p);

      updateStreamSources();

    } catch (Exception e) {
      FileLog.e("NTgCallsEngine: Error starting call", e);
    }
  }

  private void updateStreamSources() {
    if (ntgCalls == null) return;

    try {
      AudioDescription audioDesc =
          new AudioDescription(
              MediaSource.DEVICE,
              NTgCalls.getMediaDevices().microphone.get(0).metadata,
              !isMicMuted,
              48000,
              2);

      VideoDescription videoDesc = null;
      if (isVideoEnabled) {
        String cameraId = "";
        if (!NTgCalls.getMediaDevices().camera.isEmpty()) {
          cameraId = NTgCalls.getMediaDevices().camera.get(isFrontCamera ? 1 : 0).metadata;
        }

        videoDesc = new VideoDescription(MediaSource.DEVICE, cameraId, true, 1280, 720, 30);
      }

      MediaDescription mediaDesc = new MediaDescription(audioDesc, null, videoDesc, null);

      ntgCalls.setStreamSources(CALL_ID, StreamMode.CAPTURE, mediaDesc);
    } catch (Exception e) {
      FileLog.e("NTgCallsEngine: Error updating streams", e);
    }
  }

  private int mapNetworkState(NetworkInfo.State state) {
    if (state == NetworkInfo.State.CONNECTED) return Instance.STATE_ESTABLISHED;
    if (state == NetworkInfo.State.CONNECTING) return Instance.STATE_WAIT_INIT;
    if (state == NetworkInfo.State.FAILED) return Instance.STATE_FAILED;
    if (state == NetworkInfo.State.RECONNECTING) return Instance.STATE_RECONNECTING;
    return Instance.STATE_WAIT_INIT;
  }

  @Override
  public Instance.FinalState stop() {
    if (BuildVars.LOGS_ENABLED) {
      FileLog.d("NTgCallsEngine: stop()");
    }
    if (ntgCalls != null) {
      try {
        ntgCalls.stop(CALL_ID);
      } catch (Exception e) {
        FileLog.e(e);
      }
      ntgCalls = null;
    }
    return new Instance.FinalState(new byte[0], "", new Instance.TrafficStats(0, 0, 0, 0), false);
  }

  @Override
  public void onSignalingDataReceive(byte[] data) {
    if (ntgCalls != null) {
      ntgCalls.sendSignalingData(CALL_ID, data);
    }
  }

  @Override
  public void setMuteMicrophone(boolean mute) {
    isMicMuted = mute;
    if (ntgCalls != null) {
      if (mute) ntgCalls.mute(CALL_ID);
      else ntgCalls.unmute(CALL_ID);
    }
  }

  @Override
  public void setVideoState(int videoState) {
    isVideoEnabled = (videoState == Instance.VIDEO_STATE_ACTIVE);
    updateStreamSources();
  }

  @Override
  public void switchCamera(boolean front) {
    isFrontCamera = front;
    updateStreamSources();
  }

  @Override
  public long createVideoCapturer(VideoSink localSink, int type) {
    return 1;
  }

  @Override
  public void destroyVideoCapturer(long capturerPtr) {}

  @Override
  public void setVideoStateCapturer(long videoCapturer, int videoState) {}

  @Override
  public void switchCameraCapturer(long videoCapturer, boolean front) {}

  @Override
  public void setupOutgoingVideo(VideoSink localSink, int type) {}

  @Override
  public void setupOutgoingVideoCreated(long videoCapturer) {}

  @Override
  public void setVideoSink(VideoSink remoteSink) {
    this.remoteSink = remoteSink;
  }

  @Override
  public long addIncomingVideoOutput(
      int quality,
      String endpointId,
      NativeInstance.SsrcGroup[] ssrcGroups,
      VideoSink remoteSink,
      long userId) {
    return endpointId.hashCode();
  }

  @Override
  public void removeIncomingVideoOutput(long nativeRemoteSink) {}

  @Override
  public void startGroup(
      String logPath,
      long videoCapturer,
      boolean screencast,
      boolean noiseSupression,
      NativeInstance.PayloadCallback payloadCallback,
      NativeInstance.AudioLevelsCallback audioLevelsCallback,
      NativeInstance.VideoSourcesCallback unknownParticipantsCallback,
      NativeInstance.RequestBroadcastPartCallback requestBroadcastPartCallback,
      NativeInstance.RequestBroadcastPartCallback cancelRequestBroadcastPartCallback,
      NativeInstance.RequestCurrentTimeCallback requestCurrentTimeCallback,
      boolean isConference) {
    if (BuildVars.LOGS_ENABLED) {
      FileLog.d(
          "NTgCallsEngine: startGroup() - Group calls via NTgCalls not fully implemented yet");
    }
  }

  @Override
  public void setAudioOutputGainControlEnabled(boolean enabled) {}

  @Override
  public void setEchoCancellationStrength(int strength) {}

  @Override
  public void setVolume(int ssrc, double volume) {}

  @Override
  public void setNetworkType(int networkType) {}

  @Override
  public long getPreferredRelayId() {
    return 0;
  }

  @Override
  public String getLastError() {
    return null;
  }

  @Override
  public String getDebugInfo() {
    return "NTgCalls";
  }

  @Override
  public Instance.TrafficStats getTrafficStats() {
    return null;
  }

  @Override
  public boolean isGroup() {
    return false;
  }

  @Override
  public void onMediaDescriptionAvailable(
      long taskPtr, VoIPService.RequestedParticipant[] participants) {}

  @Override
  public void setNoiseSuppressionEnabled(boolean value) {}

  @Override
  public void activateVideoCapturer(long videoCapturer) {}

  @Override
  public void clearVideoCapturer() {}

  @Override
  public void setVideoEndpointQuality(String endpointId, int quality) {}

  @Override
  public void setGlobalServerConfig(String serverConfigJson) {}

  @Override
  public void setBufferSize(int size) {}

  @Override
  public String getVersion() {
    return "1.0-ntg";
  }

  @Override
  public void setJoinResponsePayload(String payload) {}

  @Override
  public void prepareForStream(boolean isRtpStream) {}

  @Override
  public void resetGroupInstance(boolean set, boolean disconnect) {}

  @Override
  public void onStreamPartAvailable(
      long ts,
      java.nio.ByteBuffer buffer,
      int size,
      long timestamp,
      int videoChannel,
      int quality) {}

  @Override
  public boolean hasVideoCapturer() {
    return isVideoEnabled;
  }

  @Override
  public void onRequestTimeComplete(long taskPtr, long time) {}

  @Override
  public void setConferenceCallId(long call_id) {}
}
