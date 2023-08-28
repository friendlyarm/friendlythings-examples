package com.friendlyarm.GPIODemo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import android.net.Uri;
import android.content.Intent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemClickListener;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import com.friendlyarm.FriendlyThings.GPIOEnum;
import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;

public class GPIODemoMainActivity extends Activity {
    private static final String TAG = "GPIODemo";
    private Timer timer = new Timer();
    private int step = 0; 
    private Map<String, Integer> demoGPIOPins = new HashMap<String, Integer>();  
    
    static int STEP_INIT_GPIO_DIRECTION = 1;
    static int STEP_INIT_VIEW = 2;
    
    @Override
        public void onDestroy() {
            timer.cancel();
            super.onDestroy();
        }

    public void onMoreSamplesPressed(View view) {
        Uri uri = Uri.parse("http://wiki.friendlyelec.com/wiki/index.php/FriendlyThings_for_Rockchip");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent); 
    }

    private void displayListView() {
        Log.d(TAG, "displayListView");

        ArrayList<GPIOPin> pinList = new ArrayList<GPIOPin>();
        List<Map.Entry<String,Integer>> list = new ArrayList<Map.Entry<String,Integer>>(demoGPIOPins.entrySet());
        Collections.sort(list,new Comparator<Map.Entry<String,Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                    Map.Entry<String, Integer> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });

        Log.d(TAG, "{{{{ DUMP GPIO Values");
        for(Map.Entry<String,Integer> entry:list){
            int value = HardwareControler.getGPIOValue(entry.getValue());
            if (value == GPIOEnum.HIGH) {
                Log.d(TAG, entry.getKey() + "(" + String.valueOf(entry.getValue()) + ") = HIGH");
            } else {
                Log.d(TAG, entry.getKey() + "(" + String.valueOf(entry.getValue()) + ") = LOW");
            }
            GPIOPin pin = new GPIOPin(entry.getValue(), entry.getKey(), value);
            pinList.add(pin);
        } 
        Log.d(TAG, "}}}}");

        dataAdapter = new MyCustomAdapter(this, R.layout.listview_item,
                pinList);
        ListView listView = (ListView) findViewById(R.id.listView1);
        listView.setAdapter(dataAdapter);
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    timer.cancel();
                    displayListView();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private TimerTask init_task = new TimerTask() {
        public void run() {
            Log.d(TAG, "init_task " + step);
            if (step == STEP_INIT_GPIO_DIRECTION) {
                for (Integer sysPinNum: demoGPIOPins.values()) {
                    int currentDirection = HardwareControler.getGPIODirection(sysPinNum);
                    Log.v(TAG, String.format("currentDirection(%d) == %d", sysPinNum, currentDirection));
                    if (currentDirection < 0 || currentDirection != GPIOEnum.OUT) {
                        if (HardwareControler.setGPIODirection(sysPinNum, GPIOEnum.OUT) != 0) {
                            Log.v(TAG, String.format("setGPIODirection(%d) to OUT failed", sysPinNum));
                        } else {
                            Log.v(TAG, String.format("setGPIODirection(%d) to OUT OK", sysPinNum));
                        }
                    }
                }
                step ++;
            } else if (step == STEP_INIT_VIEW) {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }

        }
    }; 
    //////////////////////////////////////

    MyCustomAdapter dataAdapter = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpiodemo_main);
        setTitle("GPIO Demo");

        int boardType = HardwareControler.getBoardType();
        if (boardType == BoardType.Smart4418SDK) {
            demoGPIOPins.put("GPIOC9", 73);
            demoGPIOPins.put("GPIOC10", 74);
            demoGPIOPins.put("GPIOC11", 75);
            demoGPIOPins.put("GPIOC12", 76);
        } else if (boardType == BoardType.NanoPC_T2
                || boardType == BoardType.NanoPC_T3
                || boardType == BoardType.NanoPC_T3T
                ) {
            demoGPIOPins.put("Pin17", 68);
            demoGPIOPins.put("Pin18", 71);
            demoGPIOPins.put("Pin19", 72);
            demoGPIOPins.put("Pin20", 88);
            demoGPIOPins.put("Pin21", 92);
            demoGPIOPins.put("Pin22", 58);
        } else if (boardType == BoardType.NanoPC_T4) {
            demoGPIOPins.put("Pin11", 33);
            demoGPIOPins.put("Pin12", 50);
            demoGPIOPins.put("Pin15", 36);
            demoGPIOPins.put("Pin16", 54);
            demoGPIOPins.put("Pin18", 55);
            demoGPIOPins.put("Pin22", 56);
            demoGPIOPins.put("Pin37", 96);
            demoGPIOPins.put("Pin38", 125);
            demoGPIOPins.put("Pin40", 126);
        } else if (boardType == BoardType.NanoPi_M4 || boardType == BoardType.NanoPi_M4v2 || boardType == BoardType.NanoPi_M4B || boardType == BoardType.NanoPi_NEO4 || boardType == BoardType.NanoPi_NEO4v2) {
            demoGPIOPins.put("Pin11", 33);
            demoGPIOPins.put("Pin12", 50);
            demoGPIOPins.put("Pin15", 36);
            demoGPIOPins.put("Pin16", 54);
            demoGPIOPins.put("Pin18", 55);
            demoGPIOPins.put("Pin22", 56);
        } else if (boardType == BoardType.SOM_RK3399 || boardType == BoardType.SOM_RK3399v2) {
            demoGPIOPins.put("Pin7", 41);
            demoGPIOPins.put("Pin9", 42);
        } else if (boardType == BoardType.NanoPC_T6) {
            demoGPIOPins.put("Pin07", 106);  // GPIO3_B2
            demoGPIOPins.put("Pin12", 111);  // GPIO3_B7
            demoGPIOPins.put("Pin15",  39);  // GPIO1_A7
            demoGPIOPins.put("Pin16", 107);  // GPIO3_B3
            demoGPIOPins.put("Pin18", 108);  // GPIO3_B4
            demoGPIOPins.put("Pin26",  40);  // GPIO1_B0
            //spi0 (m2)
            //demoGPIOPins.put("Pin19",  42);  // GPIO1_B2
            //demoGPIOPins.put("Pin21",  41);  // GPIO1_B1
            //demoGPIOPins.put("Pin22",  45);  // GPIO1_B5
            //demoGPIOPins.put("Pin23",  43);  // GPIO1_B3
            //demoGPIOPins.put("Pin24",  44);  // GPIO1_B4
        } else if (boardType == BoardType.NanoPi_R6C) {
            //i2s1 (m0)
            demoGPIOPins.put("Pin07", 128);  // GPIO4_A0
            demoGPIOPins.put("Pin11", 129);  // GPIO4_A1
            demoGPIOPins.put("Pin13", 130);  // GPIO4_A2
            demoGPIOPins.put("Pin15", 133);  // GPIO4_A5
            demoGPIOPins.put("Pin16", 134);  // GPIO4_A6
            demoGPIOPins.put("Pin18", 137);  // GPIO4_B1
            demoGPIOPins.put("Pin22", 138);  // GPIO4_B2
        } else if (boardType == BoardType.NanoPi_R6S) {
            //spi0
            demoGPIOPins.put("Pin03",  43);  // GPIO1_B3
            demoGPIOPins.put("Pin05",  41);  // GPIO1_B1
            demoGPIOPins.put("Pin06",  44);  // GPIO1_B4
            demoGPIOPins.put("Pin07",  42);  // GPIO1_B2
            //uart1 (m1)
            demoGPIOPins.put("Pin09",  47);  // GPIO1_B7
            demoGPIOPins.put("Pin10",  46);  // GPIO1_B6
            //uart5 (m1)
            //demoGPIOPins.put("Pin11", 116);  // GPIO3_C4
            //demoGPIOPins.put("Pin12", 117);  // GPIO3_C5
        } else if (boardType == BoardType.NanoPi_R5S || boardType == BoardType.NanoPi_R5S_LTS) {
            //spi1
            demoGPIOPins.put("Pin03", 115);  // GPIO3_C3
            demoGPIOPins.put("Pin05", 114);  // GPIO3_C2
            demoGPIOPins.put("Pin06",  97);  // GPIO3_A1
            demoGPIOPins.put("Pin07", 113);  // GPIO3_C1
            //uart9 (m1)
            //demoGPIOPins.put("Pin09", 149);  // GPIO4_C5
            //demoGPIOPins.put("Pin10", 150);  // GPIO4_C6
            //uart7 (m1)
            demoGPIOPins.put("Pin11", 116);  // GPIO3_C4
            demoGPIOPins.put("Pin12", 117);  // GPIO3_C5
        } else if (android.os.Build.VERSION.RELEASE.contains("4.1.2")) {
            demoGPIOPins.put("LED 1", 281);
            demoGPIOPins.put("LED 2", 282);
            demoGPIOPins.put("LED 3", 283);
            demoGPIOPins.put("LED 4", 284);
        }

        if (demoGPIOPins.size() == 0) {
            Toast.makeText(this, String.format("Not found any GPIO pin."),
                    Toast.LENGTH_SHORT).show();
        } else {
            // export all pins
            for (Integer sysPinNum: demoGPIOPins.values()) {
                if (HardwareControler.exportGPIOPin(sysPinNum) != 0) {
                    Toast.makeText(this, String.format("exportGPIOPin(%d) failed!", sysPinNum),
                            Toast.LENGTH_SHORT).show();
                }
            }
            step = STEP_INIT_GPIO_DIRECTION;
            timer.schedule(init_task, 300, 200);
        }
    }

    private class MyCustomAdapter extends ArrayAdapter<GPIOPin> {
        private ArrayList<GPIOPin> pinList;
        public MyCustomAdapter(Context context, int resourceId,
                ArrayList<GPIOPin> pinList) {
            super(context, resourceId, pinList);
            this.pinList = new ArrayList<GPIOPin>();
            this.pinList.addAll(pinList);
        }

        private class ViewHolder {
            Switch switchView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.listview_item, null);
                viewHolder = new ViewHolder();
                viewHolder.switchView = (Switch) convertView.findViewById(R.id.pin_switch);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // reinit switchView
            GPIOPin pin = pinList.get(position);
            viewHolder.switchView.setText(pin.getName());
            viewHolder.switchView.setChecked(pin.getValue() == GPIOEnum.HIGH);
            viewHolder.switchView.setTag(pin);
            /*
            viewHolder.switchView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (!buttonView.isPressed()) {
                        return;
                    }
                    Switch sw = (Switch) buttonView;
                    GPIOPin newPinData = (GPIOPin) sw.getTag();
                    if (isChecked) {
                        newPinData.setValue(GPIOEnum.HIGH);
                    } else {
                        newPinData.setValue(GPIOEnum.LOW);
                    }
                    if (HardwareControler.setGPIOValue(newPinData.getSysPinNum(),newPinData.getValue()) == 0) {
                        Log.v(TAG, String.format("setGPIOValue" + newPinData.getName() + " (%d) OK", newPinData.getSysPinNum()));
                    } else {
                        Log.v(TAG, String.format("setGPIOValue" + newPinData.getName() + " (%d) Failed", newPinData.getSysPinNum()));
                    }
                    pinList.set(position, newPinData);
                }
            });
            */
            viewHolder.switchView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // you might keep a reference to the CheckBox to avoid this class cast
                    Switch sw = (Switch) v;
                    GPIOPin newPinData = (GPIOPin) sw.getTag();
                    if (sw.isChecked()) {
                        newPinData.setValue(GPIOEnum.HIGH);
                    } else {
                        newPinData.setValue(GPIOEnum.LOW);
                    }
                    if (HardwareControler.setGPIOValue(newPinData.getSysPinNum(),newPinData.getValue()) == 0) {
                        Log.v(TAG, String.format("setGPIOValue" + newPinData.getName() + " (%d) OK", newPinData.getSysPinNum()));
                    } else {
                        Log.v(TAG, String.format("setGPIOValue" + newPinData.getName() + " (%d) Failed", newPinData.getSysPinNum()));
                    }
                    pinList.set(position, newPinData);
                }
            });
            return convertView;
        }
    }
}
