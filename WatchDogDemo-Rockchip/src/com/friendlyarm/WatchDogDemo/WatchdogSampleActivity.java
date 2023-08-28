package com.friendlyarm.WatchDogDemo;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.FileCtlEnum;
import com.friendlyarm.FriendlyThings.WatchDogEnum;
import com.friendlyarm.FriendlyThings.BoardType;

import com.friendlyarm.WatchDogDemo.R;
import com.friendlyarm.Utils.CommonFuncs;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;
import android.os.Message;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WatchdogSampleActivity extends Activity {
    private static final String TAG = "WatchDogDemo";
    private int mWatchDogFD = -1;
    Button mStartButton;
    Button mFeedButton;
    TextView mCountDownTextView;
    TextView mWillResetTextView;
    private Timer mTimer = new Timer();
    int nWatchDogTimeout = 21;  //rk3399: 1,2,5,10,21
    int mCountDown = nWatchDogTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStartButton = (Button)findViewById(R.id.button_start);
        mFeedButton = (Button)findViewById(R.id.button_feed);
        mCountDownTextView = (TextView)findViewById(R.id.text_countdown);
        mWillResetTextView = (TextView)findViewById(R.id.text_willreset);
        mCountDownTextView.setText(Integer.toString(mCountDown) + "s");
    }

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				mCountDownTextView.setText(Integer.toString(mCountDown) + "s");
				if (mCountDown == 0) {
					mWillResetTextView.setText("Keepalive missed, machine will reset");
				}
				break;
			}
			super.handleMessage(msg);
		}
	};

	private TimerTask mTask = new TimerTask() {
		public void run() {
			mCountDown --;
			if (mCountDown < 0) {
				mCountDown = 0;
			}
			Message message = new Message();
			message.what = 1;
			mHandler.sendMessage(message);
		}
	};

    public void onStartPressed(View view) {
    	if (mWatchDogFD < 0) {
			mWatchDogFD = HardwareControler.open("/dev/watchdog", FileCtlEnum.O_WRONLY);
			if (mWatchDogFD < 0) {
				Log.d(TAG, "Fail to open /dev/watchdog");
			} else {
				mStartButton.setEnabled(false);
				Log.d(TAG, "Open /dev/watchdog OK");

				ByteBuffer byteBuffer = ByteBuffer.allocate(4);
				byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
				byteBuffer.putInt(nWatchDogTimeout);
				if (HardwareControler.ioctl(mWatchDogFD, WatchDogEnum.WDIOC_SETTIMEOUT, byteBuffer.array()) < 0) {
					Log.d(TAG, "Fail to ioctl WDIOC_SETTIMEOUT");
				} else {
					ByteBuffer wrapped = ByteBuffer.wrap(byteBuffer.array()); 
					wrapped.order(ByteOrder.LITTLE_ENDIAN);
					int num = wrapped.getInt();
					Log.d(TAG, "Set Wathdog timeout OK, set value: " + Integer.toString(num));
				}

				byte[] timeoutGet = { 0, 0, 0, 0 }; 
				if (HardwareControler.ioctl(mWatchDogFD, WatchDogEnum.WDIOC_GETTIMEOUT, timeoutGet) < 0) {
					Log.d(TAG, "Fail to ioctl WDIOC_GETTIMEOUT");
				} else {
					ByteBuffer wrapped = ByteBuffer.wrap(timeoutGet);
					wrapped.order(ByteOrder.LITTLE_ENDIAN);
					int num = wrapped.getInt();
					Log.d(TAG, "get Wathdog timeout OK, current value: " + Integer.toString(num));
					mCountDown = nWatchDogTimeout = num;
					mCountDownTextView.setText(Long.toString(mCountDown) + "s");
				}

				mTimer.schedule(mTask, 0, 1000);
			}
    	}
    }

    public void onFeedPressed(View view) {
    	if (mWatchDogFD>0) {
    		HardwareControler.write(mWatchDogFD, "a".getBytes());
    		mCountDown = nWatchDogTimeout;
    		mCountDownTextView.setText(Long.toString(mCountDown) + "s");
    		mWillResetTextView.setText("");
    	}
    }
}
