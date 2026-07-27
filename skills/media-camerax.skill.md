---
name: media-camerax
description: Camera integration standards for OnlyTechTake using CameraX.
---

# Media (CameraX)

Camera functionality is implemented using the Jetpack CameraX library.

## Implementation Details
1. **Preview**: Use `PreviewView` for real-time camera feed.
2. **Image Capture**: Use `ImageCapture` for photos. Handle orientation and rotation correctly.
3. **Video Capture**: Use `VideoCapture` (Recorder API) for video recording.
4. **Lifecycle**: CameraX is lifecycle-aware. Bind use cases to `ProcessCameraProvider`.

## Best Practices
- **Permissions**: Check for `CAMERA` and `RECORD_AUDIO` permissions before binding.
- **Optimization**: Use `QualitySelector` for video to balance size and quality.
- **Rotation**: Always consider device rotation in `ImageCapture` metadata.

## References
- [CameraX Overview](https://developer.android.com/media/camera/camerax)
- [Migrate Camera1 to CameraX](https://developer.android.com/media/camera/camerax/camera1-to-camerax)
