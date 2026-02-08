package org.telegram.messenger.voip;

import java.nio.ByteBuffer;

import org.webrtc.VideoSink;

public interface VoIPEngine {

  interface Callback {
    void onStateUpdated(int state, boolean inTransition);

    void onSignalBarsUpdated(int signalBars);

    void onSignalData(byte[] data);

    void onRemoteMediaStateUpdated(int audioState, int videoState);

    void onAudioLevelsUpdated(int[] uids, float[] levels, boolean[] voice);
  }

  void setCallback(Callback callback);

  void start(
      String version,
      Instance.Config config,
      String persistentStateFilePath,
      Instance.Endpoint[] endpoints,
      Instance.Proxy proxy,
      int networkType,
      Instance.EncryptionKey encryptionKey,
      VideoSink remoteSink,
      long videoCapturer);

  void startGroup(
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
      boolean isConference);

  Instance.FinalState stop();

  void setMuteMicrophone(boolean mute);

  void setAudioOutputGainControlEnabled(boolean enabled);

  void setEchoCancellationStrength(int strength);

  void setVolume(int ssrc, double volume);

  void setVideoState(int videoState);

  void switchCamera(boolean front);

  void setupOutgoingVideo(VideoSink localSink, int type);

  void setVideoSink(VideoSink remoteSink);

  void onSignalingDataReceive(byte[] data);

  void setNetworkType(int networkType);

  long getPreferredRelayId();

  String getLastError();

  String getDebugInfo();

  Instance.TrafficStats getTrafficStats();

  boolean isGroup();

  void onMediaDescriptionAvailable(long taskPtr, VoIPService.RequestedParticipant[] participants);

  void setNoiseSuppressionEnabled(boolean value);

  void activateVideoCapturer(long videoCapturer);

  void clearVideoCapturer();

  long addIncomingVideoOutput(
      int quality,
      String endpointId,
      NativeInstance.SsrcGroup[] ssrcGroups,
      VideoSink remoteSink,
      long userId);

  void removeIncomingVideoOutput(long nativeRemoteSink);

  void setVideoEndpointQuality(String endpointId, int quality);

  void setGlobalServerConfig(String serverConfigJson);

  void setBufferSize(int size);

  String getVersion();

  void setJoinResponsePayload(String payload);

  void prepareForStream(boolean isRtpStream);

  void resetGroupInstance(boolean set, boolean disconnect);

  void onStreamPartAvailable(
      long ts, ByteBuffer buffer, int size, long timestamp, int videoChannel, int quality);

  boolean hasVideoCapturer();

  void onRequestTimeComplete(long taskPtr, long time);

  void setConferenceCallId(long call_id);

  void setupOutgoingVideoCreated(long videoCapturer);
}
