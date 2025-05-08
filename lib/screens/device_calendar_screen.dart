// lib/screens/device_calendar_screen.dart
import 'package:flutter/material.dart';
import 'package:focus_flow/providers/calendar_provider.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import 'package:device_calendar/device_calendar.dart' as dc; // Alias to avoid name clash

class DeviceCalendarScreen extends StatefulWidget {
  const DeviceCalendarScreen({super.key});

  static const String routeName = '/device_calendar'; // Route name for navigation

  @override
  State<DeviceCalendarScreen> createState() => _DeviceCalendarScreenState();
}

class _DeviceCalendarScreenState extends State<DeviceCalendarScreen> {
  DateTime _startDate = DateTime.now();
  // Show events for the current day by default initially, can be expanded by user
  DateTime _endDate = DateTime(
    DateTime.now().year,
    DateTime.now().month,
    DateTime.now().day,
    23, 59, 59
  );

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initCalendar();
    });
  }

  Future<void> _initCalendar() async {
    final calendarProvider = Provider.of<CalendarProvider>(context, listen: false);
    if (!calendarProvider.permissionGranted) {
      await calendarProvider.requestPermission();
    }
    if (calendarProvider.permissionGranted) {
      if (calendarProvider.calendars.isEmpty) {
        // If permission granted but calendars are not fetched yet (e.g. first time)
        await calendarProvider.fetchCalendars(); 
      }
      // If a calendar is auto-selected or becomes selected after fetching, fetch its events.
      if (calendarProvider.selectedCalendarId != null) {
        await calendarProvider.fetchEventsForSelectedCalendar(_startDate, _endDate);
      } else if (calendarProvider.calendars.isNotEmpty) {
        // If no calendar was auto-selected but calendars are available, select the first one.
        // This will also trigger event fetching within selectCalendar.
        calendarProvider.selectCalendar(calendarProvider.calendars.first.id);
      }
    }
  }

  Future<void> _selectDateRange() async {
    final DateTimeRange? picked = await showDateRangePicker(
      context: context,
      firstDate: DateTime(DateTime.now().year - 1),
      lastDate: DateTime(DateTime.now().year + 1),
      initialDateRange: DateTimeRange(start: _startDate, end: _endDate),
       helpText: 'Select Event Date Range',
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: Theme.of(context).colorScheme.copyWith(
              primary: Theme.of(context).colorScheme.primary, // header background
              onPrimary: Theme.of(context).colorScheme.onPrimary, // header text
              onSurface: Theme.of(context).textTheme.bodyLarge?.color, // body text
            ),
            dialogBackgroundColor: Theme.of(context).cardColor,
          ),
          child: child!,
        );
      }
    );
    if (picked != null) {
      setState(() {
        _startDate = picked.start;
        _endDate = picked.end;
      });
      final calendarProvider = Provider.of<CalendarProvider>(context, listen: false);
      if (calendarProvider.selectedCalendarId != null) {
        await calendarProvider.fetchEventsForSelectedCalendar(_startDate, _endDate);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Device Calendar Events'),
        actions: [
          IconButton(
            icon: const Icon(Icons.date_range_outlined),
            tooltip: 'Select Date Range',
            onPressed: _selectDateRange,
          ),
          IconButton(
            icon: const Icon(Icons.refresh_outlined),
            tooltip: 'Refresh',
            onPressed: () {
               final calendarProvider = Provider.of<CalendarProvider>(context, listen: false);
               if (calendarProvider.permissionGranted && calendarProvider.selectedCalendarId != null) {
                  calendarProvider.fetchEventsForSelectedCalendar(_startDate, _endDate);
               } else if (calendarProvider.permissionGranted && calendarProvider.calendars.isEmpty) {
                  calendarProvider.fetchCalendars(); // In case calendars were not fetched
               } else if (!calendarProvider.permissionGranted) {
                  calendarProvider.requestPermission();
               }
            },
          ),
        ],
      ),
      body: Consumer<CalendarProvider>(
        builder: (context, calendarProvider, child) {
          if (!calendarProvider.permissionGranted) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.calendar_month_outlined, size: 60, color: Theme.of(context).disabledColor),
                    const SizedBox(height: 20),
                    const Text('Calendar permission is required to view your device events.', textAlign: TextAlign.center, style: TextStyle(fontSize: 16)),
                    const SizedBox(height: 20),
                    ElevatedButton.icon(
                      icon: const Icon(Icons.lock_open_outlined),
                      onPressed: _initCalendar, // Re-run init to request permission and fetch
                      label: const Text('Grant Permission'),
                      style: ElevatedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                      )
                    ),
                  ],
                ),
              ),
            );
          }

          if (calendarProvider.calendars.isEmpty && calendarProvider.permissionGranted) {
            return const Center(child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.search_off_outlined, size: 60, color: Colors.grey),
                SizedBox(height:16),
                Text('No calendars found on this device.', style: TextStyle(fontSize: 16)),
                SizedBox(height:8),
                Text('Please ensure you have calendars set up in your device account.', style: TextStyle(color: Colors.grey, fontSize: 13), textAlign: TextAlign.center,)
              ]
            ));
          }

          return Column(
            children: [
              _buildCalendarSelector(calendarProvider),
              Padding(
                padding: const EdgeInsets.fromLTRB(16.0, 8.0, 16.0, 12.0),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: Text(
                        'Events from ${DateFormat.yMMMd().format(_startDate)} to ${DateFormat.yMMMd().format(_endDate)}',
                        style: Theme.of(context).textTheme.titleSmall,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    TextButton.icon(
                      icon: const Icon(Icons.today, size: 18),
                      label: const Text('Today'),
                      onPressed: () {
                        setState(() {
                          _startDate = DateTime.now();
                          _endDate = DateTime(
                            DateTime.now().year,
                            DateTime.now().month,
                            DateTime.now().day,
                            23, 59, 59
                          );
                        });
                        if (calendarProvider.selectedCalendarId != null) {
                          calendarProvider.fetchEventsForSelectedCalendar(_startDate, _endDate);
                        }
                      }
                    )
                  ],
                ),
              ),
              Expanded(child: _buildEventsList(calendarProvider.events, calendarProvider)),
            ],
          );
        },
      ),
    );
  }

  Widget _buildCalendarSelector(CalendarProvider calendarProvider) {
    if (calendarProvider.calendars.isEmpty) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.fromLTRB(16.0, 16.0, 16.0, 8.0),
      child: DropdownButtonFormField<String>(
        decoration: InputDecoration(
          labelText: 'Select Device Calendar',
          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          prefixIcon: const Icon(Icons.calendar_today_outlined),
        ),
        value: calendarProvider.selectedCalendarId,
        isExpanded: true,
        hint: const Text('Choose a calendar to display'),
        items: calendarProvider.calendars.map((dc.Calendar calendar) {
          return DropdownMenuItem<String>(
            value: calendar.id,
            child: Row(
              children: [
                if (calendar.color != null) 
                  Padding(
                    padding: const EdgeInsets.only(right: 8.0),
                    child: CircleAvatar(radius: 6, backgroundColor: Color(calendar.color! | 0xFF000000)),
                  ),
                Expanded(child: Text(calendar.name ?? 'Unnamed Calendar', overflow: TextOverflow.ellipsis)),
                if (calendar.isDefault ?? false) const Icon(Icons.star, size:16, color: Colors.amber),
              ],
            ),
          );
        }).toList(),
        onChanged: (String? newValue) {
          if (newValue != null) {
            calendarProvider.selectCalendar(newValue);
            // Events will be fetched by the selectCalendar method within the provider
            // for the default date range (_startDate, _endDate)
            calendarProvider.fetchEventsForSelectedCalendar(_startDate, _endDate);
          }
        },
        selectedItemBuilder: (BuildContext context) { // Custom builder for selected item
          return calendarProvider.calendars.map<Widget>((dc.Calendar item) {
            return Row(
                children: [
                  if (item.color != null) 
                    Padding(
                      padding: const EdgeInsets.only(right: 8.0),
                      child: CircleAvatar(radius: 6, backgroundColor: Color(item.color! | 0xFF000000)),
                    ),
                  Expanded(child: Text(item.name ?? 'Unnamed Calendar', overflow: TextOverflow.ellipsis)),
                ],
              );
          }).toList();
        },
      ),
    );
  }

  Widget _buildEventsList(List<dc.Event> events, CalendarProvider calendarProvider) {
    if (calendarProvider.selectedCalendarId == null && calendarProvider.calendars.isNotEmpty) {
      return const Center(child: Text('Please select a calendar to view events.', style: TextStyle(fontSize: 16)));
    }
    if (events.isEmpty) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(20.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.event_busy_outlined, size: 60, color: Colors.grey),
              SizedBox(height: 16),
              Text('No events found in this calendar for the selected date range.', textAlign: TextAlign.center, style: TextStyle(fontSize: 16)),
            ],
          ),
        ),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.only(bottom: 16.0), // Added padding at the bottom
      itemCount: events.length,
      itemBuilder: (context, index) {
        final event = events[index];
        final String startTime = event.start != null ? DateFormat.jm().format(event.start!.toLocal()) : 'N/A';
        final String endTime = event.end != null ? DateFormat.jm().format(event.end!.toLocal()) : 'N/A';
        final String startDateStr = event.start != null ? DateFormat.MMMEd().format(event.start!.toLocal()) : '';
        
        Color eventColor = Colors.blue.shade300; 
        if (event.color != null) {
          try {
            eventColor = Color(event.color! | 0xFF000000); 
          } catch (e) { /* Keep default */ }
        } else if (calendarProvider.selectedCalendar?.color != null) {
           try {
            eventColor = Color(calendarProvider.selectedCalendar!.color! | 0xFF000000); 
          } catch (e) { /* Keep default */ }
        }

        bool isMultiDay = false;
        if(event.start != null && event.end != null) {
          final startDay = DateTime(event.start!.year, event.start!.month, event.start!.day);
          final endDay = DateTime(event.end!.year, event.end!.month, event.end!.day);
          isMultiDay = endDay.difference(startDay).inDays > 0 && !event.allDay!;
        }

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          elevation: 1.5,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(10),
             side: BorderSide(color: eventColor.withOpacity(0.6), width: 0.5)
          ),
          child: ListTile(
            leading: Container(
              width: 5,
              decoration: BoxDecoration(
                color: eventColor,
                borderRadius: const BorderRadius.only(topLeft: Radius.circular(10), bottomLeft: Radius.circular(10))
              ),
            ),
            minLeadingWidth: 5, // To make the leading container take minimal width
            title: Text(event.title ?? 'No Title', style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const SizedBox(height: 4),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: Theme.of(context).textTheme.bodySmall?.color?.withOpacity(0.7)),
                    const SizedBox(width: 6),
                    if(event.allDay == true)
                      Text('All Day on $startDateStr', style: const TextStyle(fontSize: 13)),
                    else if (isMultiDay)
                      Text('Multi-day event', style: const TextStyle(fontSize: 13)), // Placeholder for multi-day
                    else
                      Text('$startDateStr, $startTime - $endTime', style: const TextStyle(fontSize: 13)),
                  ],
                ),
                if (event.description != null && event.description!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 5.0),
                    child: Text(event.description!, maxLines: 2, overflow: TextOverflow.ellipsis, style: TextStyle(fontSize: 12.5, color: Theme.of(context).textTheme.bodySmall?.color?.withOpacity(0.9))),
                  ),
                if (event.location != null && event.location!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 5.0),
                    child: Row(
                      children: [
                        Icon(Icons.location_on_outlined, size: 14, color: Theme.of(context).textTheme.bodySmall?.color?.withOpacity(0.7)),
                        const SizedBox(width: 6),
                        Expanded(child: Text(event.location!, style: const TextStyle(fontSize: 12.5), overflow: TextOverflow.ellipsis)),
                      ],
                    ),
                  ),
              ],
            ),
            isThreeLine: (event.description != null && event.description!.isNotEmpty) || (event.location != null && event.location!.isNotEmpty),
            contentPadding: const EdgeInsets.only(top: 8, bottom: 8, right: 16), // Adjusted padding
          ),
        );
      },
    );
  }
}
