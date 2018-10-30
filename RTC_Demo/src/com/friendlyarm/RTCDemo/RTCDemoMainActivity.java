package com.friendlyarm.RTCDemo;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;
import com.friendlyarm.RTCDemo.R;
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
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.content.Context;
import android.os.RemoteException;
import java.io.*;

public class RTCDemoMainActivity extends Activity {
    private static final String TAG = "RTCDemo";

    private TextView mDateTextView;
    private TextView mTimeTextView;
    private TextView mAutoWakeUpTextView;
    private SeekBar mAutoWakeUpSeekBar;
    private Button mEnableButton;
    private TextView mResultTextView;
    private int mWakeUpSeconds = 120;
    final int MSG_SHOW_DATETIME = 1;
    final int MSG_SHOW_WAKEUP_TIMEOUT = 2;
    private int mWakeUpSecondsCountDown = mWakeUpSeconds;

    private static boolean writeToFile(String fileName, String v) {
        Log.d(TAG, "#### Write " + v + " to " + fileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(v);
            writer.close();
            return true;
        } catch(IOException ie) {
            ie.printStackTrace();
            Log.e(TAG, "Write file error: " + fileName);
            Log.e(TAG, ie.toString());
        }
        return false;
    }

    private static String readFromFile(String path, String fileName) {
        File file = new File(path,fileName);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String str = br.readLine().trim();
            br.close();
            return str;
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return "";
    }


    private Timer timer = new Timer();
    private TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = MSG_SHOW_DATETIME;
            handler.sendMessage(message);
        }
    };

    private Timer timerForWakeUp = null;
    private TimerTask taskForWakeUp = null;

    private int mBoardType = HardwareControler.getBoardType();
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SHOW_DATETIME:
                if (mBoardType == BoardType.NanoPC_T4 || mBoardType == BoardType.NanoPi_M4 || mBoardType == BoardType.NanoPi_NEO4) {
                    mDateTextView.setText(readFromFile("/sys/class/rtc/rtc0/","date"));
                    mTimeTextView.setText(readFromFile("/sys/class/rtc/rtc0/","time"));
                }
                break;
            case MSG_SHOW_WAKEUP_TIMEOUT:
                mWakeUpSecondsCountDown --;
                if (mWakeUpSecondsCountDown < 0) {
                    mWakeUpSecondsCountDown = 0;
                    mEnableButton.setEnabled(true);
                    mAutoWakeUpSeekBar.setEnabled(true);
                    mResultTextView.setText("");

                    timerForWakeUp.cancel();
                    timerForWakeUp = null;
                    taskForWakeUp.cancel();
                    taskForWakeUp = null;
                    return ;
                }
                mResultTextView.setText("The board will wake up automatically after " + String.valueOf(mWakeUpSecondsCountDown) + " seconds");
                break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mDateTextView = (TextView)findViewById(R.id.text_date);
        mTimeTextView = (TextView)findViewById(R.id.text_time);
        mAutoWakeUpTextView = (TextView)findViewById(R.id.text_autoWakeUpLabel);
        mAutoWakeUpSeekBar = (SeekBar)findViewById(R.id.seekbar_autowakeup);
        mEnableButton = (Button)findViewById(R.id.button_enableAutoWakeUp);
        mResultTextView = (TextView)findViewById(R.id.text_result);

        
        mAutoWakeUpSeekBar.setProgress(mWakeUpSeconds);
        mAutoWakeUpSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i > 0) {
                    mWakeUpSeconds = i;
                }
                setGuiText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        timer.schedule(task, 0, 1000);
        setGuiText();
    }


    private void setGuiText(){
        mAutoWakeUpTextView.setText(getString(R.string.txtAutoWakeUpLabel) + " (After " + String.valueOf(mWakeUpSeconds) + "s)");
    }

    public void onEnableAutoWakeUpPressed(View view) {
        if (mBoardType == BoardType.NanoPC_T4 
            || mBoardType == BoardType.NanoPi_M4 
            || mBoardType == BoardType.NanoPi_NEO4) {
            if (writeToFile("/sys/class/rtc/rtc0/wakealarm", "+" + String.valueOf(mWakeUpSeconds))) {
                Toast.makeText(this, String.format("Performed successfully"),
                        Toast.LENGTH_SHORT).show();
                mWakeUpSecondsCountDown = mWakeUpSeconds;
                mResultTextView.setText("The board will wake up automatically after " + String.valueOf(mWakeUpSecondsCountDown) + " seconds");
                mEnableButton.setEnabled(false);
                mAutoWakeUpSeekBar.setEnabled(false);

                if (timerForWakeUp == null) {
                    timerForWakeUp = new Timer();
                }
                if (taskForWakeUp == null) {
                    taskForWakeUp = new TimerTask() {
                        public void run() {
                            Message message = new Message();
                            message.what = MSG_SHOW_WAKEUP_TIMEOUT;
                            handler.sendMessage(message);
                        }
                    };
                }
                timerForWakeUp.schedule(taskForWakeUp, 0, 1000);
            } else {
                Toast.makeText(this, String.format("Failed"),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onPoweroffPressed(View view) {
        Thread thr = new Thread("Shutdown") {
            @Override
            public void run() {
                IPowerManager pm = IPowerManager.Stub.asInterface(ServiceManager.getService(Context.POWER_SERVICE));
                try {
                    pm.shutdown(false,PowerManager.SHUTDOWN_USER_REQUESTED,false);
                } catch (RemoteException e) {
                }
            }
        };
        thr.start();
    }
}
