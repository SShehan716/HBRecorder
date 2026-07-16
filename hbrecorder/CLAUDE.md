# hbrecorder module notes

This is a customized fork of the public HBRecorder library — treat its README (`README.md`, at repo root) as documentation for the *original* upstream library's public API (`setOutputPath`, `setAudioSource`, `enableCustomSettings`, etc.), not as a changelog for local modifications. When changing recording/audio/mux behavior, the real logic lives in `HBRecorder.java`, `ScreenRecordService.java`, `InternalAudioCapture.java`, and `AvMuxer.java` — cross-check those against the README's documented behavior rather than assuming the README is current.
