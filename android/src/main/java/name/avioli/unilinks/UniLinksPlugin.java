package name.avioli.unilinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** UniLinksPlugin */
public class UniLinksPlugin
    implements MethodCallHandler, StreamHandler, PluginRegistry.NewIntentListener {
  private static final String MESSAGES_CHANNEL = "uni_links/messages";
  private static final String EVENTS_CHANNEL = "uni_links/events";

  private BroadcastReceiver changeReceiver;
  private Registrar registrar;

  private String initialLink;
  private String latestLink;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    // Detect if we've been launched in background
    if (registrar.activity() == null) {
      return;
    }

    UniLinksPlugin instance = new UniLinksPlugin(registrar);

    final MethodChannel mChannel = new MethodChannel(registrar.messenger(), MESSAGES_CHANNEL);
    mChannel.setMethodCallHandler(instance);

    final EventChannel eChannel = new EventChannel(registrar.messenger(), EVENTS_CHANNEL);
    eChannel.setStreamHandler(instance);

    registrar.addNewIntentListener(instance);
  }

  private UniLinksPlugin(Registrar registrar) {
    this.registrar = registrar;
    handleIntent(registrar.context(), registrar.activity().getIntent(), true);
  }

  private void handleIntent(Context context, Intent intent, Boolean initial) {
    String action = intent.getAction();
    String dataString = intent.getDataString();

    if (Intent.ACTION_VIEW.equals(action)) {
      if (initial) initialLink = dataString;
      latestLink = dataString;
      if (changeReceiver != null) changeReceiver.onReceive(context, intent);
    }
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("getInitialLink")) {
      result.success(initialLink);
    } else if (call.method.equals("getLatestLink")) {
      result.success(latestLink);
    } else if (call.method.equals("clear")) {
      initialLink = null;
      latestLink = null;
      if (changeReceiver != null) changeReceiver.onReceive(registrar.context(), null);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onListen(Object arguments, EventSink events) {
    changeReceiver = createChangeReceiver(events);
    
    //Go ahead and send the link on listen if we have one
    if (latestLink != null && !latestLink.isEmpty()) {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latestLink));
      if (changeReceiver != null) changeReceiver.onReceive(registrar.context(), intent);
    }
  }

  @Override
  public void onCancel(Object arguments) {
    changeReceiver = null;
  }

  @Override
  public boolean onNewIntent(Intent intent) {
    handleIntent(registrar.context(), intent, false);
    return false;
  }

  private BroadcastReceiver createChangeReceiver(final EventSink events) {
    return new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        // NOTE: assuming intent.getAction() is Intent.ACTION_VIEW

        // Log.v("uni_links", String.format("received action: %s", intent.getAction()));
        String dataString = "";
        if (intent != null) {
          dataString = intent.getDataString();
        }

        if (dataString == null) {
          events.error("UNAVAILABLE", "Link unavailable", null);
        } else {
          events.success(dataString);
        }
      }
    };
  }
}
