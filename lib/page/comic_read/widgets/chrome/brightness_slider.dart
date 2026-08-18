import 'package:flutter/material.dart';

/// Brightness slider overlay for reader
class ReaderBrightnessSlider extends StatefulWidget {
  final double initialBrightness;
  final ValueChanged<double> onChanged;
  final VoidCallback? onDismiss;

  const ReaderBrightnessSlider({
    super.key,
    required this.initialBrightness,
    required this.onChanged,
    this.onDismiss,
  });

  @override
  State<ReaderBrightnessSlider> createState() => _ReaderBrightnessSliderState();
}

class _ReaderBrightnessSliderState extends State<ReaderBrightnessSlider>
    with SingleTickerProviderStateMixin {
  late double _brightness;
  late AnimationController _controller;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _brightness = widget.initialBrightness;
    _controller = AnimationController(
      duration: Duration(milliseconds: 300),
      vsync: this,
    );
    _fadeAnimation = CurvedAnimation(
      parent: _controller,
      curve: Curves.easeOut,
    );
    _controller.forward();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _dismiss() {
    _controller.reverse().then((_) => widget.onDismiss?.call());
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _dismiss,
      behavior: HitTestBehavior.opaque,
      child: FadeTransition(
        opacity: _fadeAnimation,
        child: Container(
          color: Colors.black.withOpacity(0.4),
          child: Center(
            child: GestureDetector(
              onTap: () {},
              child: Container(
                margin: EdgeInsets.symmetric(horizontal: 32),
                padding: EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: Color(0xFF1A1A2E),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: Colors.white.withOpacity(0.1),
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.5),
                      blurRadius: 30,
                      offset: Offset(0, 10),
                    ),
                  ],
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      _brightness > 0.7
                          ? Icons.brightness_high
                          : _brightness > 0.3
                              ? Icons.brightness_medium
                              : Icons.brightness_low,
                      color: Colors.amber,
                      size: 48,
                    ),
                    SizedBox(height: 16),
                    Text(
                      '屏幕亮度',
                      style: TextStyle(
                        color: Colors.white70,
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(height: 16),
                    SliderTheme(
                      data: SliderThemeData(
                        trackHeight: 6,
                        thumbShape: RoundSliderThumbShape(enabledThumbRadius: 10),
                        overlayShape: RoundSliderOverlayShape(overlayRadius: 20),
                        activeTrackColor: Colors.amber,
                        inactiveTrackColor: Colors.white.withOpacity(0.1),
                        thumbColor: Colors.amber,
                        overlayColor: Colors.amber.withOpacity(0.2),
                      ),
                      child: Slider(
                        value: _brightness,
                        min: 0.1,
                        max: 1.0,
                        onChanged: (v) {
                          setState(() => _brightness = v);
                          widget.onChanged(v);
                        },
                      ),
                    ),
                    SizedBox(height: 8),
                    Text(
                      '${(_brightness * 100).round()}%',
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.5),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
