import 'package:flutter_test/flutter_test.dart';
import 'package:manjie/app/app.dart';

void main() {
  testWidgets('App smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const ManjieApp());
    expect(find.text('漫界'), findsOneWidget);
  });
}