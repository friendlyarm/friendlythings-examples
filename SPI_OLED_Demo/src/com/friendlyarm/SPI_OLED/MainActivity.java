package com.friendlyarm.SPI_OLED;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;
import com.friendlyarm.SPI_OLED.R;
import com.friendlyarm.SPI_OLED.OLED;

public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "SPILCD";
    private EditText toEditor = null;
    private int mBoardType = HardwareControler.getBoardType();
    private boolean mBoardSupported = true;
    
    private final String devName = "/dev/spidev0.0";   /*For S5P4418*/
    //private final int gpioPin_For_DC = 75;               /*GPIOC11 on Smart4418*/
    //private final int gpioPin_For_Reset = 74;          /*GPIOC10 on Smart4418*/
    
    private int gpioPin_For_DC;
    private int gpioPin_For_Reset;
    
    OLED oled;

    private Timer mExportGPIOTimer = new Timer();
    private Timer mFlashTextTimer = new Timer();
    private int mSPIInitStep = 0; 
    private final int STEP_INIT_GPIO = 1;
    private final int STEP_INIT_SPI = 2;
    private final int ID_TIMER_EXPORT_GPIO = 1;
    private final int ID_TIMER_FLASH_TEXT = 2;
    
    @Override
    public void onDestroy() {
        mExportGPIOTimer.cancel();
        mFlashTextTimer.cancel();
        
        HardwareControler.unexportGPIOPin( gpioPin_For_DC );
        HardwareControler.unexportGPIOPin( gpioPin_For_Reset );
        oled.Deinit();
        
        super.onDestroy();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainactivity);
        setTitle("SPI OLED");

        Button backButton = (Button)findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
        ((Button)findViewById(R.id.cleanButton)).setOnClickListener(this);
       
        toEditor = (EditText)findViewById(R.id.toEditor);
        toEditor.setText("ABCDEFGHIJKOMNOPabcdefghijkomnop1234567890123456,./<>?;':\"[]{}*@");

         if (mBoardType>=BoardType.S5P4418_BASE && mBoardType<=BoardType.S5P4418_MAX) {
             gpioPin_For_DC = 75;            /* GPIOC11 on Smart4418 */
             gpioPin_For_Reset = 74;         /* GPIOC10 on Smart4418 */
        } else if (mBoardType>=BoardType.S5P6818_BASE && mBoardType<=BoardType.S5P6818_MAX) {
             gpioPin_For_DC = 68;            /* GPIOC4 on T3, PIn17 */
             gpioPin_For_Reset = 71;         /* GPIOC7 on T3, Pin18 */
         } else if (mBoardType>=BoardType.RK3399_BASE && mBoardType<=BoardType.RK3399_MAX) {
             gpioPin_For_DC = 33;            /* GPIO1_A1 on T4, NEO4, M4, Pin11 */
             gpioPin_For_Reset = 36;         /* GPIO1_A4 on T4, NEO4, M4, Pin15 */
         } else if (mBoardType == BoardType.NanoPC_T6) {
             gpioPin_For_DC = 108;           /* GPIO3_B4, Pin18 */
             gpioPin_For_Reset = 107;        /* GPIO3_B3, Pin16 */
         } else {
            Toast.makeText(this, String.format("Current implementation only supports S5P4418/S5P6818/RK3399/NanoPC_T6."),
                Toast.LENGTH_SHORT).show();
            mBoardSupported = false;
         }
        
        /* no focus when begin */
        toEditor.clearFocus();
        toEditor.setFocusable(false);
        toEditor.setFocusableInTouchMode(true);
        

        oled = new OLED();
        if (HardwareControler.exportGPIOPin( gpioPin_For_DC ) == 0 
                && HardwareControler.exportGPIOPin( gpioPin_For_Reset ) == 0) {
            
            Log.d(TAG, "exportGPIOPin ok");
            
            /*
             * 1->set direction gpio  
             * 2->set gpio value  
             * 3->unexport  
             * >3 quit mExportGPIOTimer
             */
            
            mSPIInitStep = STEP_INIT_GPIO;  
            mExportGPIOTimer.schedule(init_task, 100, 100); 
        } else {
            Toast.makeText(this,"exportGPIOPin failed!",Toast.LENGTH_SHORT).show();
        }
    }
    
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case ID_TIMER_EXPORT_GPIO:
                mExportGPIOTimer.cancel();
                break;
            case ID_TIMER_FLASH_TEXT:
                mFlashTextTimer.cancel();
                break;
            }
            super.handleMessage(msg);
        }
    };
    
    private void quitTimer(int timerId) {
        Message message = new Message();
        message.what = timerId;
        handler.sendMessage(message);
    }
    
    private TimerTask init_task = new TimerTask() {
        public void run() {
            if (mSPIInitStep == STEP_INIT_GPIO) {
                if (oled.Init(devName, gpioPin_For_DC, gpioPin_For_Reset) == 0) {
                    mSPIInitStep ++;
                } else {
                    quitTimer(ID_TIMER_EXPORT_GPIO);
                }
            } else if (mSPIInitStep == STEP_INIT_SPI) {
                mSPIInitStep ++;
                quitTimer(ID_TIMER_EXPORT_GPIO);
                mFlashTextTimer.schedule(flash_text_task, 1000, 1000);
            }
        }
    }; 
    
    public static String[] splitByByteSize(String content, int size){
        byte[] bytes = content.getBytes();
        int totalSize = bytes.length;
        int partNum = 0;
        if(totalSize == 0){
            return new String[0];
        }
        if(totalSize % size == 0){
            partNum = totalSize / size;
        }else{
            partNum = totalSize / size + 1;
        }
        String[] arr = new String[partNum];
        int arrIndex = -1;
        for(int i=0;i<totalSize;i++){
            if(i%size == 0){
                arrIndex++;
                arr[arrIndex] = "";
            }
            arr[arrIndex]+=((char)bytes[i]);
        }
        return arr;

    }

    private TimerTask flash_text_task = new TimerTask() {
        public void run() {
            Log.d(TAG, "Enter flash_text_task");
            final int MAX_CHATS_PER_LINE = 16;
            final int MAX_LINE = 4;
            String displayText = toEditor.getText().toString();
            //clear
            if (oled.OLEDCleanScreen() != 0) {
                Log.e(TAG, "oled.OLEDCleanScreen failed.");
            } else {
                Log.e(TAG, "oled.OLEDCleanScreen ok.");
            }
            if (displayText.length() > 0) {
                Log.e(TAG, "need display text: " + displayText);
                String[] lines = splitByByteSize(displayText, MAX_CHATS_PER_LINE);
                for (int i=0; i<MAX_LINE && i<lines.length; i++) {
                    Log.e(TAG, "display line: " + i + ", text: " + lines[i]);
                    if (oled.OLEDDisp8x16Str(0, i*MAX_CHATS_PER_LINE, lines[i].getBytes()) != 0) {
                        Log.e(TAG, "oled.OLEDDisp8x16Str failed.");
                    } else {
                        Log.e(TAG, "oled.OLEDDisp8x16Str ok.");
                    }
                }
            }
            quitTimer(ID_TIMER_FLASH_TEXT);
            Log.d(TAG, "Leave flash_text_task");
            
        }
    };
    
    public void onClick(View v)
    {
        switch (v.getId()) {
        case R.id.backButton:
            finish();
            break;
        case R.id.cleanButton:
            toEditor.setText("");
            break;
        }
    }
}
