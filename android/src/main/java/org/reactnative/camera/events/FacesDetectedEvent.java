package org.reactnative.camera.events;

import androidx.core.util.Pools;

import org.reactnative.camera.CameraViewManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class FacesDetectedEvent extends Event<FacesDetectedEvent> {
  private static final Pools.SynchronizedPool<FacesDetectedEvent> EVENTS_POOL =
      new Pools.SynchronizedPool<>(3);

  private WritableArray mData;
  private String mBase64Image;

  private FacesDetectedEvent() {}

  public static FacesDetectedEvent obtain(int viewTag, WritableArray data, String base64Image) {
    FacesDetectedEvent event = EVENTS_POOL.acquire();
    if (event == null) {
      event = new FacesDetectedEvent();
    }
    event.init(viewTag, data, base64Image);
    return event;
  }

  private void init(int viewTag, WritableArray data, String base64Image) {
    super.init(viewTag);
    mData = data;
    mBase64Image = base64Image;
  }

  /**
   * note(@sjchmiela)
   * Should the events about detected faces coalesce, the best strategy will be
   * to ensure that events with different faces count are always being transmitted.
   */
  @Override
  public short getCoalescingKey() {
    if (mData.size() > Short.MAX_VALUE) {
      return Short.MAX_VALUE;
    }

    return (short) mData.size();
  }

  @Override
  public String getEventName() {
    return CameraViewManager.Events.EVENT_ON_FACES_DETECTED.toString();
  }

  @Override
  public void dispatch(RCTEventEmitter rctEventEmitter) {
    rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
  }

  private WritableMap serializeEventData() {
    WritableMap event = Arguments.createMap();
    event.putString("type", "face");
    event.putArray("faces", mData);
    event.putString("base64", mBase64Image);
    event.putInt("target", getViewTag());
    return event;
  }
}
