enum CallAction {
  callEnded,
  onMissedCall,
  onIncomingCallAnswered,
  onIncomingCallReceived,
}

abstract class BaseCallEvent {}

class CallEvent extends BaseCallEvent {
  final CallAction action;
  final String number;
  final int? duration;

  CallEvent({required this.action, required this.number, this.duration});

  @override
  String toString() => 'CallEvent { number: $number, duration: $duration }';
}
