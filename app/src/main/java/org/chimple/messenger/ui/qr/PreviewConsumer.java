package org.chimple.messenger.ui.qr;

import android.hardware.Camera;

@SuppressWarnings("deprecation")
public interface PreviewConsumer {

	void start(Camera camera);

	void stop();
}
