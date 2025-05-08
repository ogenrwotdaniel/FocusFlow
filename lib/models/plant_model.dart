class Plant {
  final String id;
  final String name;
  final String description;
  final String imageUrl;
  final String? requiredAchievementId;

  Plant({
    required this.id,
    required this.name,
    required this.description,
    required this.imageUrl,
    this.requiredAchievementId,
  });

  Plant copyWith({
    String? id,
    String? name,
    String? description,
    String? imageUrl,
    String? requiredAchievementId,
  }) {
    return Plant(
      id: id ?? this.id,
      name: name ?? this.name,
      description: description ?? this.description,
      imageUrl: imageUrl ?? this.imageUrl,
      requiredAchievementId: requiredAchievementId ?? this.requiredAchievementId,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'name': name,
      'description': description,
      'imageUrl': imageUrl,
      'requiredAchievementId': requiredAchievementId,
    };
  }

  factory Plant.fromMap(Map<String, dynamic> map) {
    return Plant(
      id: map['id'] ?? '',
      name: map['name'] ?? '',
      description: map['description'] ?? '',
      imageUrl: map['imageUrl'] ?? '',
      requiredAchievementId: map['requiredAchievementId'],
    );
  }
}
