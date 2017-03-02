package org.chimple.messenger.ui.qr;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

@SuppressWarnings("deprecation")
public class QrCodeDecoder implements PreviewConsumer, PreviewCallback {

	private static final String TAG =
			QrCodeDecoder.class.getPackage().getName();

	private final Reader reader = new QRCodeReader();
	private final ResultCallback callback;

	private boolean stopped = false;

	public QrCodeDecoder(ResultCallback callback) {
		this.callback = callback;
	}

	public void start(Camera camera) {
		Log.d(TAG, "Started");
		stopped = false;
		askForPreviewFrame(camera);
	}

	public void stop() {
		Log.d(TAG, "Stopped");
		stopped = true;
	}

	private void askForPreviewFrame(Camera camera) {
		if(!stopped) camera.setOneShotPreviewCallback(this);
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		if(!stopped) {
			Size size = camera.getParameters().getPreviewSize();
			new DecoderTask(camera, data, size.width, size.height).execute();
		}
	}

	private class DecoderTask extends AsyncTask<Void, Void, Void> {

		final Camera camera;
		final byte[] data;
		final int width, height;

		DecoderTask(Camera camera, byte[] data, int width, int height) {
			this.camera = camera;
			this.data = data;
			this.width = width;
			this.height = height;
		}

		@Override
		protected Void doInBackground(Void... params) {
			long now = System.currentTimeMillis();
			LuminanceSource src = new PlanarYUVLuminanceSource(data, width,
					height, 0, 0, width, height, false);
			BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(src));
			Result result = null;
			try {
				result = reader.decode(bitmap);
			} catch(ReaderException e) {
				return null; // No barcode found
			} finally {
				reader.reset();
			}
			long duration = System.currentTimeMillis() - now;
			Log.d(TAG, "Decoding barcode took " + duration + " ms");
			callback.handleResult(result);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			askForPreviewFrame(camera);
		}
	}

	public interface ResultCallback {

		void handleResult(Result result);
	}
}
