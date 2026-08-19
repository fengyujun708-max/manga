/// 漫画项目模型
class ComicItem {
  final String id;
  final String title;
  final String author;
  final String coverUrl;
  final String chapter;
  final double rating;

  ComicItem({
    required this.id,
    required this.title,
    required this.author,
    this.coverUrl = '',
    this.chapter = '',
    this.rating = 0.0,
  });

  factory ComicItem.fromJson(Map<String, dynamic> json) {
    return ComicItem(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '',
      author: json['author']?.toString() ?? '',
      coverUrl: json['coverUrl']?.toString() ?? '',
      chapter: json['chapter']?.toString() ?? '',
      rating: (json['rating'] as num?)?.toDouble() ?? 0.0,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'author': author,
    'coverUrl': coverUrl,
    'chapter': chapter,
    'rating': rating,
  };
}