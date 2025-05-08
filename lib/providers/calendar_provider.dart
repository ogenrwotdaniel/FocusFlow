// lib/providers/calendar_provider.dart
import 'package:flutter/foundation.dart';
import 'package:device_calendar/device_calendar.dart';
import 'package:collection/collection.dart'; // For firstWhereOrNull
import 'package:timezone/timezone.dart' as tz; // Add this import

class CalendarProvider with ChangeNotifier {
  final DeviceCalendarPlugin _deviceCalendarPlugin = DeviceCalendarPlugin();
  List<Calendar> _calendars = [];
  List<Event> _events = [];
  bool _permissionGranted = false;
  String? _selectedCalendarId; // For simplicity, start with one selectable calendar or default

  List<Calendar> get calendars => _calendars;
  List<Event> get events => _events;
  bool get permissionGranted => _permissionGranted;
  Calendar? get selectedCalendar => _calendars.firstWhereOrNull((cal) => cal.id == _selectedCalendarId);

  CalendarProvider() {
    // Optionally, auto-request permission or load saved calendar preference on init
    // For now, we'll require explicit calls
  }

  Future<void> requestPermission() async {
    var permissionsGrantedResult = await _deviceCalendarPlugin.hasPermissions();
    if (permissionsGrantedResult.isSuccess && !(permissionsGrantedResult.data ?? false)) {
      permissionsGrantedResult = await _deviceCalendarPlugin.requestPermissions();
      if (!permissionsGrantedResult.isSuccess || !(permissionsGrantedResult.data ?? false)) {
        _permissionGranted = false;
        notifyListeners();
        // Consider throwing an error or returning a status to UI
        debugPrint("Calendar permission not granted.");
        return;
      }
    }
    _permissionGranted = true;
    debugPrint("Calendar permission granted.");
    await fetchCalendars(); // Fetch calendars once permission is confirmed
    notifyListeners();
  }

  Future<void> fetchCalendars() async {
    if (!_permissionGranted) {
      debugPrint("Cannot fetch calendars, permission not granted.");
      // Optionally attempt to request permission again or prompt user
      // await requestPermission();
      // if (!_permissionGranted) return;
      return;
    }

    final calendarsResult = await _deviceCalendarPlugin.retrieveCalendars();
    if (calendarsResult.isSuccess && calendarsResult.data != null) {
      _calendars = calendarsResult.data!.where((cal) => !(cal.isReadOnly ?? true)).toList(); // Filter for writable calendars if needed, or show all
      // _calendars = calendarsResult.data!; // Or show all
      debugPrint("Fetched ${_calendars.length} calendars.");
      // Auto-select the first calendar or a default one if none is selected
      if (_selectedCalendarId == null && _calendars.isNotEmpty) {
        // Try to find a commonly named default calendar or just pick the first
        var defaultCalendar = _calendars.firstWhereOrNull((c) => c.name?.toLowerCase() == 'calendar' || c.name?.toLowerCase() == 'events');
        _selectedCalendarId = defaultCalendar?.id ?? _calendars.first.id;
        debugPrint("Auto-selected calendar: ${_selectedCalendarId}");
      }
    } else {
      _calendars = [];
      debugPrint("Error fetching calendars: ${calendarsResult.errors}");
    }
    notifyListeners();
  }

  // Method to allow UI to change the selected calendar
  void selectCalendar(String? calendarId) {
    if (_calendars.any((cal) => cal.id == calendarId)) {
      _selectedCalendarId = calendarId;
      debugPrint("User selected calendar: $_selectedCalendarId");
      // Fetch events for the newly selected calendar
      if (_selectedCalendarId != null) {
        // Example: fetch for the next 7 days
        fetchEventsForSelectedCalendar(DateTime.now(), DateTime.now().add(const Duration(days: 7)));
      }
       else {
        _events = []; // Clear events if no calendar is selected
      }
      notifyListeners();
    } else if (calendarId == null) {
      _selectedCalendarId = null;
      _events = [];
      notifyListeners();
      debugPrint("Calendar selection cleared.");
    }
  }


  Future<void> fetchEventsForSelectedCalendar(DateTime startDate, DateTime endDate) async {
    if (!_permissionGranted || _selectedCalendarId == null) {
      _events = [];
      notifyListeners();
      debugPrint("Cannot fetch events, permission not granted or no calendar selected.");
      return;
    }

    final retrieveEventsParams = RetrieveEventsParams(
      startDate: startDate,
      endDate: endDate,
    );

    final eventsResult = await _deviceCalendarPlugin.retrieveEvents(
      _selectedCalendarId!,
      retrieveEventsParams,
    );

    if (eventsResult.isSuccess && eventsResult.data != null) {
      _events = eventsResult.data!;
      // Sort events by start time
      _events.sort((a, b) => (a.start ?? DateTime(0)).compareTo(b.start ?? DateTime(0)));
      debugPrint("Fetched ${_events.length} events for calendar $_selectedCalendarId between $startDate and $endDate.");
    } else {
      _events = [];
      debugPrint("Error fetching events for calendar $_selectedCalendarId: ${eventsResult.errors}");
    }
    notifyListeners();
  }

  // Optional: Add event to calendar (for Phase 2)
  Future<bool> addEventToSelectedCalendar({
    required String title,
    required DateTime startTime,
    required DateTime endTime,
    String? description,
    String? location,
    bool allDay = false,
    RecurrenceRule? recurrenceRule,
  }) async {
    if (!_permissionGranted || _selectedCalendarId == null) {
      debugPrint("Cannot add event, permission not granted or no calendar selected.");
      return false;
    }
    if (selectedCalendar == null || (selectedCalendar!.isReadOnly ?? true)) {
        debugPrint("Cannot add event, selected calendar '(${selectedCalendar?.name})' is read-only or not found.");
        return false;
    }


    final eventToAdd = Event(
      _selectedCalendarId,
      title: title,
      start: tz.TZDateTime.from(startTime, tz.local), // Convert here
      end: tz.TZDateTime.from(endTime, tz.local),     // Convert here
      description: description,
      location: location,
      allDay: allDay,
      recurrenceRule: recurrenceRule,
    );

    final createEventResult = await _deviceCalendarPlugin.createOrUpdateEvent(eventToAdd);
    if (createEventResult?.isSuccess == true && (createEventResult?.data?.isNotEmpty ?? false)) {
      debugPrint("Event '$title' added successfully to calendar $_selectedCalendarId. Event ID: ${createEventResult!.data}");
      // Optionally, re-fetch events to update the UI
      await fetchEventsForSelectedCalendar(DateTime.now(), DateTime.now().add(const Duration(days: 7)));
      return true;
    } else {
      debugPrint("Error adding event '$title' to calendar $_selectedCalendarId: ${createEventResult?.errors}");
      return false;
    }
  }
}
