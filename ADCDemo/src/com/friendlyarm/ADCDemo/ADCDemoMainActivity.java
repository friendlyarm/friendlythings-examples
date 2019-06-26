package com.friendlyarm.ADCDemo;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedWriter;
import android.os.Message;
import android.os.Handler;
import android.os.Looper;
import java.io.*;

import com.friendlyarm.FriendlyThings.HardwareControler;
import com.friendlyarm.FriendlyThings.BoardType;

public class ADCDemoMainActivity extends Activity {

	MyCustomAdapter dataAdapter = null;
	private static final String TAG = "ADCDemo";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.adc_main);
		Log.d("ADCDemo", "BoardID: " + mBoardType);
		timer.schedule(task, 0, 500);
	}

    static int readNumFromFile(String path, String fileName) {
    	int result = 0;
		File file = new File(path,fileName);
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line = br.readLine();
		    br.close();

			try {
			    result = Integer.parseInt(line);
			} catch(NumberFormatException nfe) {
			   System.out.println("Could not parse " + nfe);
			   result = 0;
			} 
		}
		catch (IOException e) {
		    //You'll need to add proper error handling here
		}
		return result;
    }

	private int mBoardType = HardwareControler.getBoardType();
	private Timer timer = new Timer();
	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {

			// Array list of countries
			ArrayList<ADC> adcValueList = new ArrayList<ADC>();

			switch (msg.what) {
			case 1:
				if (mBoardType == BoardType.NanoPC_T4 || mBoardType == BoardType.NanoPi_M4 || mBoardType == BoardType.NanoPi_NEO4 || mBoardType == BoardType.SOM_RK3399) {
					//path: 
					//ADC
					//in_voltage0_raw
					//in_voltage2_raw
					//in_voltage3_raw

					int[] channels = { 0, 2, 3 };
					int[] adc = { 0, 0, 0 };
					final String path = "/sys/devices/platform/ff100000.saradc/iio:device0";

					adc[0] = readNumFromFile(path, "in_voltage0_raw");
					adc[1] = readNumFromFile(path, "in_voltage2_raw");
					adc[2] = readNumFromFile(path, "in_voltage3_raw");

					for (int i = 0; i < 3; i++) {
						ADC adcObj = new ADC(adc[i], String.format("[AIN%d]",
								channels[i]));
						adcValueList.add(adcObj);
					}
				} else if (mBoardType == BoardType.S3C6410_COMMON) {
					int[] channels = { 0, 1, 4, 5, 6, 7 };
					int[] adc = HardwareControler.readADCWithChannels(channels);

					for (int i = 0; i < 6; i++) {
						ADC adcObj = new ADC(adc[i], String.format("[AIN%d]",
								channels[i]));
						adcValueList.add(adcObj);
					}

				} else if (mBoardType == BoardType.S5PV210_COMMON) {
					int[] channels = { 0, 1, 6, 7, 8, 9 };
					int[] adc = HardwareControler.readADCWithChannels(channels);

					for (int i = 0; i < 6; i++) {
						ADC adcObj = new ADC(adc[i], String.format("[AIN%d]",
								channels[i]));
						adcValueList.add(adcObj);
					}

				} else if (mBoardType == BoardType.S5P4412_COMMON) {
					int[] channels = { 0, 1, 2, 3 };
					int[] adc = HardwareControler.readADCWithChannels(channels);

					for (int i = 0; i < 4; i++) {
						ADC adcObj = new ADC(adc[i], String.format("[AIN%d]",
								channels[i]));
						adcValueList.add(adcObj);
					}
					
				} else if (mBoardType == BoardType.Smart4418SDK) {
					int[] channels = { 1, 3, 4, 5, 6, 7 };
					int[] adc = HardwareControler.readADCWithChannels(channels);

					for (int i = 0; i < channels.length; i++) {
						ADC adcObj = new ADC(adc[i], String.format("[AIN%d]",
								channels[i]));
						adcValueList.add(adcObj);
					}
				} else {
					int adc = HardwareControler.readADCWithChannel(0);
					ADC adcObj = new ADC(adc, String.format("[AIN%d]", 0));
					adcValueList.add(adcObj);
				}

				dataAdapter = new MyCustomAdapter(getApplicationContext(),
						R.layout.adc_listview_item, adcValueList);
				ListView listView = (ListView) findViewById(R.id.listView1);
				listView.setAdapter(dataAdapter);

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
	

	private class MyCustomAdapter extends ArrayAdapter<ADC> {

		private ArrayList<ADC> adcValueList;

		public MyCustomAdapter(Context context, int textViewResourceId,
				ArrayList<ADC> adcValueList) {
			super(context, textViewResourceId, adcValueList);
			this.adcValueList = new ArrayList<ADC>();
			this.adcValueList.addAll(adcValueList);
		}

		private class ViewHolder {
			TextView nameTextView;
			TextView valueTextView;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			ViewHolder holder = null;
			Log.v("ConvertView", String.valueOf(position));

			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.adc_listview_item, null);

				holder = new ViewHolder();
				
				holder.nameTextView = (TextView) convertView
						.findViewById(R.id.listTextView1);
				
				holder.valueTextView = (TextView) convertView
						.findViewById(R.id.listTextView2);
				
				convertView.setTag(holder);

			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ADC adcObj = adcValueList.get(position);
			if (holder.nameTextView != null) {
				holder.nameTextView.setText(adcObj.getName());
			}
			if (holder.valueTextView != null) {
				holder.valueTextView.setText(String.valueOf(adcObj.getValue()));
			}
			
			return convertView;

		}

	}
}