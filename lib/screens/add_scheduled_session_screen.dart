import 'package:flutter/material.dart';
import 'package:focus_flow/models/scheduled_session_model.dart';
import 'package:focus_flow/models/session_model.dart';
import 'package:focus_flow/providers/schedule_provider.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';

class AddScheduledSessionScreen extends StatefulWidget {
  final ScheduledSession? sessionToEdit; // For editing existing sessions

  const AddScheduledSessionScreen({super.key, this.sessionToEdit});

  @override
  State<AddScheduledSessionScreen> createState() =>
      _AddScheduledSessionScreenState();
}

class _AddScheduledSessionScreenState extends State<AddScheduledSessionScreen> {
  final _formKey = GlobalKey<FormState>();
  late TextEditingController _nameController;
  late DateTime _selectedDate;
  late TimeOfDay _selectedTime;
  late int _durationMinutes;
  late SessionType _sessionType;
  bool _isEditing = false;

  @override
  void initState() {
    super.initState();
    _isEditing = widget.sessionToEdit != null;

    if (_isEditing) {
      final session = widget.sessionToEdit!;
      _nameController = TextEditingController(text: session.name);
      _selectedDate = session.plannedStartTime;
      _selectedTime = TimeOfDay.fromDateTime(session.plannedStartTime);
      _durationMinutes = session.plannedDurationMinutes;
      _sessionType = session.sessionType;
    } else {
      _nameController = TextEditingController();
      _selectedDate = DateTime.now().add(const Duration(hours: 1)); // Default to 1 hour from now
      _selectedTime = TimeOfDay.fromDateTime(_selectedDate);
      _durationMinutes = 25; // Default duration
      _sessionType = SessionType.focus; // Default type
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _pickDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
      });
    }
  }

  Future<void> _pickTime(BuildContext context) async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: _selectedTime,
    );
    if (picked != null && picked != _selectedTime) {
      setState(() {
        _selectedTime = picked;
      });
    }
  }

  void _saveForm() {
    if (_formKey.currentState!.validate()) {
      _formKey.currentState!.save();

      final scheduleProvider = Provider.of<ScheduleProvider>(context, listen: false);
      final finalDateTime = DateTime(
        _selectedDate.year,
        _selectedDate.month,
        _selectedDate.day,
        _selectedTime.hour,
        _selectedTime.minute,
      );

      // Basic validation for past time
      if (!_isEditing && finalDateTime.isBefore(DateTime.now())) {
         ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Cannot schedule a session in the past.'), backgroundColor: Colors.red),
        );
        return;
      }


      final newScheduledSession = ScheduledSession(
        id: _isEditing ? widget.sessionToEdit!.id : null, // Keep ID if editing
        name: _nameController.text.trim().isEmpty ? null : _nameController.text.trim(),
        plannedStartTime: finalDateTime,
        plannedDurationMinutes: _durationMinutes,
        sessionType: _sessionType,
        // notificationId: _isEditing ? widget.sessionToEdit!.notificationId : null, // Preserve or re-schedule
      );

      if (_isEditing) {
        scheduleProvider.updateScheduledSession(newScheduledSession);
      } else {
        scheduleProvider.addScheduledSession(newScheduledSession);
      }
      
      Navigator.of(context).pop();
       ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Session ${_isEditing ? 'updated' : 'scheduled'}!'), duration: const Duration(seconds: 2)),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_isEditing ? 'Edit Session Plan' : 'Plan New Session'),
        actions: [
          IconButton(
            icon: const Icon(Icons.save),
            onPressed: _saveForm,
            tooltip: 'Save Session',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Form(
          key: _formKey,
          child: ListView(
            children: <Widget>[
              TextFormField(
                controller: _nameController,
                decoration: const InputDecoration(
                  labelText: 'Session Name (Optional)',
                  hintText: 'e.g., Morning Study, Project Work',
                  icon: Icon(Icons.label_outline),
                ),
              ),
              const SizedBox(height: 20),
              ListTile(
                leading: const Icon(Icons.calendar_today_outlined),
                title: const Text('Date'),
                subtitle: Text(DateFormat.yMMMd().format(_selectedDate)),
                trailing: const Icon(Icons.arrow_drop_down),
                onTap: () => _pickDate(context),
              ),
              ListTile(
                leading: const Icon(Icons.access_time_outlined),
                title: const Text('Time'),
                subtitle: Text(_selectedTime.format(context)),
                trailing: const Icon(Icons.arrow_drop_down),
                onTap: () => _pickTime(context),
              ),
              const SizedBox(height: 20),
              TextFormField(
                initialValue: _durationMinutes.toString(),
                decoration: const InputDecoration(
                  labelText: 'Duration (minutes)',
                  icon: Icon(Icons.hourglass_bottom_outlined),
                ),
                keyboardType: TextInputType.number,
                validator: (value) {
                  if (value == null || value.isEmpty) {
                    return 'Please enter a duration.';
                  }
                  final n = int.tryParse(value);
                  if (n == null || n <= 0) {
                    return 'Please enter a valid positive duration.';
                  }
                  return null;
                },
                onSaved: (value) {
                  _durationMinutes = int.parse(value!);
                },
              ),
              const SizedBox(height: 20),
              DropdownButtonFormField<SessionType>(
                value: _sessionType,
                decoration: const InputDecoration(
                  labelText: 'Session Type',
                  icon: Icon(Icons.category_outlined),
                ),
                items: SessionType.values.map((SessionType type) {
                  return DropdownMenuItem<SessionType>(
                    value: type,
                    child: Text(type.name[0].toUpperCase() + type.name.substring(1)), // Capitalize first letter
                  );
                }).toList(),
                onChanged: (SessionType? newValue) {
                  if (newValue != null) {
                    setState(() {
                      _sessionType = newValue;
                    });
                  }
                },
              ),
              const SizedBox(height: 30),
              ElevatedButton.icon(
                icon: const Icon(Icons.save),
                label: Text(_isEditing ? 'Update Session' : 'Schedule Session'),
                onPressed: _saveForm,
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
