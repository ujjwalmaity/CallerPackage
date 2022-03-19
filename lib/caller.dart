library caller;

import 'package:caller/src/caller_event.dart';
import 'package:caller/src/failures.dart';
import 'package:flutter/services.dart';

export 'src/caller_event.dart';
export 'src/failures.dart';

class Caller {
  static const _method = MethodChannel("me.leoletto.caller");
  static const _event = EventChannel('MyEvent');

  static Stream<CallEvent> get onEvent => _event.receiveBroadcastStream().map(_toCallEvent);

  static Future<void> initialize() async {
    final hasPermissions = await Caller.checkPermission();

    if (!hasPermissions) throw MissingAuthorizationFailure();

    try {
      await _method.invokeMethod('initialize');
    } on PlatformException catch (_) {
      throw UnableToInitializeFailure('Unable to initialize Caller plugin');
    }
  }

  static Future<void> requestPermissions() async {
    await _method.invokeMethod('requestPermissions');
  }

  static Future<bool> checkPermission() async {
    try {
      final res = await _method.invokeMethod('checkPermissions');
      return res == true;
    } catch (_) {
      return false;
    }
  }

  static Future<void> stopCaller() async {
    await _method.invokeMethod('stopCaller');
  }

  static CallEvent _toCallEvent(dynamic data) {
    if (data is Map) {
      final event = data['event'];
      final args = List<dynamic>.from(data['arguments']);
      print(event);

      switch (args.elementAt(1)) {
        case 'callEnded':
          return CallEvent(action: CallAction.callEnded, number: args.elementAt(0), duration: args.elementAt(2));
        case 'onMissedCall':
          return CallEvent(action: CallAction.onMissedCall, number: args.elementAt(0), duration: args.elementAt(2));
        case 'onIncomingCallAnswered':
          return CallEvent(action: CallAction.onIncomingCallAnswered, number: args.elementAt(0), duration: args.elementAt(2));
        case 'onIncomingCallReceived':
          return CallEvent(action: CallAction.onIncomingCallReceived, number: args.elementAt(0), duration: args.elementAt(2));
      }
    }
    throw Exception('Undefined event!');
  }
}
