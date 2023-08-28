package com.friendlyarm.SerialPortDemo;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.util.Log;
import android.text.Html;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;
import com.friendlyarm.SerialPortDemo.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class SerialPortDataProcessView extends Activity implements OnClickListener {
    private static final String TAG = "SerialPort";
    private TextView fromTextView = null;
    private EditText toEditor = null;
    private final int MAXLINES = 200;
    private StringBuilder remoteData = new StringBuilder(256 * MAXLINES);

    private String devName = "";
    private int speed = 115200;
    private int dataBits = 8;
    private int stopBits = 1;
    private int devfd = -1;

    private int mBoardType = HardwareControler.getBoardType();

    public void onMoreSamplesPressed(View view) {
        Uri uri = Uri.parse("http://wiki.friendlyelec.com/wiki/index.php/FriendlyThings_for_Rockchip");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        timer.cancel();
        if (devfd != -1) {
            HardwareControler.close(devfd);
            devfd = -1;
        }
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.serialport_dataprocessview_landscape);
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setContentView(R.layout.serialport_dataprocessview);
        }

        if (mBoardType > BoardType.RK3588_BASE && mBoardType <= BoardType.RK3588_MAX) {
            if (mBoardType == BoardType.NanoPC_T6) {
                // NanoPC-T6 UART6
                devName = "/dev/ttyS6";
            } else if (mBoardType == BoardType.NanoPi_R6S || mBoardType == BoardType.NanoPi_R6C) {
                // NanoPi-R6S/R6C - UART5
                devName = "/dev/ttyS5";
            } else {
                devName = "/dev/ttyS6";
            }
        } else if (mBoardType > BoardType.RK3568_BASE && mBoardType <= BoardType.RK3568_MAX) {
            // NanoPi-R5S UART9
            devName = "/dev/ttyS9";
        } else if (mBoardType > BoardType.RK3399_BASE && mBoardType <= BoardType.RK3399_MAX) {
            // NanoPC-T4 UART
            devName = "/dev/ttyS4";
        } else {
            devName = "/dev/ttyS1";
        }

        String winTitle = devName + "," + speed + "," + dataBits + "," + stopBits;
        setTitle(winTitle);

        ((Button)findViewById(R.id.sendButton)).setOnClickListener(this);

        fromTextView = (TextView)findViewById(R.id.fromTextView);
        toEditor = (EditText)findViewById(R.id.toEditor);

        /* no focus when begin */
        toEditor.clearFocus();
        toEditor.setFocusable(false);
        toEditor.setFocusableInTouchMode(true);

        devfd = HardwareControler.openSerialPort( devName, speed, dataBits, stopBits );
        if (devfd >= 0) {
            timer.schedule(task, 0, 500);
        } else {
            devfd = -1;
            fromTextView.append("Fail to open " + devName + "!");
        }
    }

    private final int BUFSIZE = 512;
    private byte[] buf = new byte[BUFSIZE];
    private Timer timer = new Timer();
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case 1:
                if (HardwareControler.select(devfd, 0, 0) == 1) {
                    int retSize = HardwareControler.read(devfd, buf, BUFSIZE);
                    if (retSize > 0) {
                        String str = new String(buf, 0, retSize);
                        remoteData.append(str);

                        //Log.d(TAG, "#### LineCount: " + fromTextView.getLineCount() + ", remoteData.length()=" + remoteData.length());
                        if (fromTextView.getLineCount() > MAXLINES) {
                            int nLineCount = fromTextView.getLineCount();
                            int i = 0;
                            for (i = 0; i < remoteData.length(); i++) {
                                if (remoteData.charAt(i) == '\n') {
                                    nLineCount--;

                                    if (nLineCount <= MAXLINES) {
                                        break;
                                    }
                                }
                            }
                            remoteData.delete(0, i);
                            //Log.d(TAG, "#### remoteData.delete(0, " + i + ")");
                            fromTextView.setText(remoteData.toString());
                        } else {
                            fromTextView.append(str);
                        }

                        ((ScrollView)findViewById(R.id.scroolView)).fullScroll(View.FOCUS_DOWN);
                    }
                }
                break;
            }
            super.handleMessage(msg);
        }
    };
    private TimerTask task = new TimerTask() {
        public void run() {
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };

    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.sendButton:
            String str = toEditor.getText().toString();
            if (str.length() > 0) {
                if (str.charAt(str.length()-1) != '\n') {
                    str = str + "\n";
                }
                int ret = HardwareControler.write(devfd, str.getBytes());
                if (ret > 0) {
                    toEditor.setText("");

                    str = ">>> " + str;
                    if (remoteData.length() > 0) {
                        if (remoteData.charAt(remoteData.length()-1) != '\n') {
                            remoteData.append('\n');
                            fromTextView.append("\n");
                        }
                    }
                    remoteData.append(str);
                    fromTextView.append(str);

                    ((ScrollView)findViewById(R.id.scroolView)).fullScroll(View.FOCUS_DOWN);
                } else {
                    Toast.makeText(this,"Fail to send!",Toast.LENGTH_SHORT).show();
                }
            }

            break;
        }
    }
}
