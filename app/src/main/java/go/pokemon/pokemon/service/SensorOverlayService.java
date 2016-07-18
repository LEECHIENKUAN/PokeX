package go.pokemon.pokemon.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.text.DecimalFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import go.pokemon.pokemon.R;
import go.pokemon.pokemon.lib.Prefs;
import go.pokemon.pokemon.module.SensorView;

/**
 * Service to create overlay
 * Created by hiking on 2016/7/17.
 */
public class SensorOverlayService extends Service {

	@BindView(R.id.textView_sensor_x) TextView mSensorXTextView;
	@BindView(R.id.textView_sensor_y) TextView mSensorYTextView;
	@BindView(R.id.textView_latitude) TextView mLatitudeTextView;
	@BindView(R.id.textView_longitude) TextView mLongitudeTextView;
	@BindView(R.id.sensorView) SensorView mSensorView;

	private WindowManager mWindowManager;
	private View mRootView;

	private DecimalFormat mSensorFormat, mLocationFormat;

	public static ComponentName getComponentName() {
		return new ComponentName("go.pokemon.pokemon",
				"go.pokemon.pokemon.service.SensorOverlayService");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null; // Not used
	}

	@Override
	public void onCreate() {
		super.onCreate();

		initValues();
		inflateViews();
		setUpViews();
	}

	private void initValues() {
		mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		mSensorFormat = new DecimalFormat("0.00");
		mSensorFormat.setPositivePrefix("+");
		mLocationFormat = new DecimalFormat("0.00000");
		mLocationFormat.setPositivePrefix("+");
	}

	private void inflateViews() {
		mRootView = View.inflate(this, R.layout.overlay_debug, null);
		ButterKnife.bind(this, mRootView);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent.getExtras() != null) {
			onDataReceived(intent.getExtras());
		}
		return super.onStartCommand(intent, flags, startId);
	}

	private void onDataReceived(Bundle bundle) {
		if (bundle.containsKey("sensorX")) {
			onSensorEvent(bundle);
		} else if (bundle.containsKey("latitude")) {
			onLocationUpdate(bundle);
		}
	}

	public static Bundle createSensorEventBundle(SensorEvent sensorEvent) {
		float sensorX = sensorEvent.values[0];
		float sensorY = sensorEvent.values[1];

		Bundle bundle = new Bundle();
		bundle.putDouble("sensorX", sensorX);
		bundle.putDouble("sensorY", sensorY - 5);
		return bundle;
	}

	private void onSensorEvent(Bundle bundle) {
		double sensorX = bundle.getDouble("sensorX");
		double sensorY = bundle.getDouble("sensorY");
		float sensorThreshold = Prefs.getFloat(this, Prefs.KEY_SENSOR_THRESHOLD);

		mSensorXTextView.setText("Sensor X: " + mSensorFormat.format(sensorX));
		mSensorYTextView.setText("Sensor Y: " + mSensorFormat.format(sensorY));

		boolean sensorOverThreshold =
				sensorX * sensorX + sensorY * sensorY >= sensorThreshold * sensorThreshold;
		int sensorTextColor = ContextCompat.getColor(this,
				sensorOverThreshold ? R.color.yellow_500 : R.color.white_text_secondary);
		mSensorXTextView.setTextColor(sensorTextColor);
		mSensorYTextView.setTextColor(sensorTextColor);

		mSensorView.setSensorValues((float) sensorX, (float) sensorY);
	}

	public static Bundle createLocationUpdateBundle(double latitude, double longitude) {
		Bundle bundle = new Bundle();
		bundle.putDouble("latitude", latitude);
		bundle.putDouble("longitude", longitude);
		return bundle;
	}

	private void onLocationUpdate(Bundle bundle) {
		double latitude = bundle.getDouble("latitude");
		double longitude = bundle.getDouble("longitude");

		mLatitudeTextView.setText(mLocationFormat.format(latitude));
		mLongitudeTextView.setText(mLocationFormat.format(longitude));
	}

	private void setUpViews() {
		// Set up window
		WindowManager.LayoutParams params =
				new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
						WindowManager.LayoutParams.WRAP_CONTENT,
						WindowManager.LayoutParams.TYPE_PHONE,
						WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.TOP | Gravity.START;
		params.x = 0;
		params.y = 0;
		mWindowManager.addView(mRootView, params);

		// Drag window listener
		mRootView.setOnTouchListener(new View.OnTouchListener() {

			private int initialX;
			private int initialY;
			private float initialTouchX;
			private float initialTouchY;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				WindowManager.LayoutParams params =
						(WindowManager.LayoutParams) v.getLayoutParams();
				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						initialX = params.x;
						initialY = params.y;
						initialTouchX = event.getRawX();
						initialTouchY = event.getRawY();
						return true;
					case MotionEvent.ACTION_UP:
						return true;
					case MotionEvent.ACTION_MOVE:
						params.x = initialX + (int) (event.getRawX() - initialTouchX);
						params.y = initialY + (int) (event.getRawY() - initialTouchY);
						mWindowManager.updateViewLayout(mRootView, params);
						return true;
				}
				return false;
			}
		});

		mSensorView.setThreshold(Prefs.getFloat(this, Prefs.KEY_SENSOR_THRESHOLD));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (mRootView != null) {
			mWindowManager.removeView(mRootView);
		}
	}
}

