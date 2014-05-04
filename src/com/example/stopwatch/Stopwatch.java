package com.example.stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Stopwatch extends Activity {
	
	private static final String LOG_TAG = "StopWatch";
	
	private static final int STOPWATCHSTT_STOP = 0;
	private static final int STOPWATCHSTT_START = 1;
	private int stopWatchStt = STOPWATCHSTT_STOP;
	
	private TextView timeText = null;
	private Button startButton = null;
	private Button timeLapButton = null;
	
	private static final int LAPBTN_STT_LAP = 0;
	private static final int LAPBTN_STT_RESET = 1;
	private int lapBtnStt = LAPBTN_STT_RESET;
	
	private Thread lapTimeThread = null;
	private boolean startLap = false;
	private long baseTime = 0;
	private long prevTime = 0;
	
	private final int STOPWATCH_UPDATE_TIME_MSG = 0;
	private final int STOPWATCH_UPDATE_LAP_MSG = 1;
	
	private LapRecordAdapter lapRecordAdapter = null;
	private ListView lapListView = null;
	
	private int lapNum = 0;
	
	private LinearLayout lapListTitle = null;
	
	private WakeLock wakeLock = null;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stopwatch);
		
		timeText = (TextView)findViewById(R.id.timeText);
		
		startButton = (Button)findViewById(R.id.startButton);
		startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (STOPWATCHSTT_START == stopWatchStt) {
					// Should stop stopWatch
					startButton.setText("Start");
					timeLapButton.setText("Reset");
					
					lapBtnStt = LAPBTN_STT_RESET;
					stopWatchStt = STOPWATCHSTT_STOP;
					
					// Stop the thread of updating time
					stopLapTimeThread();
					
					releaseWakeLock();
					
				} else if (STOPWATCHSTT_STOP == stopWatchStt) {
					// Should start stopWatch
					// Change the text of startButton
					startButton.setText("Stop");
					
					timeLapButton.setEnabled(true);
					timeLapButton.setText("Lap");
					
					lapBtnStt = LAPBTN_STT_LAP;
					stopWatchStt = STOPWATCHSTT_START;
					
					baseTime = System.currentTimeMillis();
					prevTime = baseTime;
					
					// Start another thread to update the time
					startLapTimeThread();
					
					requireWakeLock();
				}
			}
		});
		
		timeLapButton = (Button)findViewById(R.id.timeLapButton);
		timeLapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (LAPBTN_STT_LAP == lapBtnStt) {
					updateLapList();
				} else if (LAPBTN_STT_RESET == lapBtnStt) {
					clearLap();
				}
			}
		});
		
		lapRecordAdapter = new LapRecordAdapter(this);
		lapListView = (ListView)findViewById(R.id.lapListView);
		lapListView.setAdapter(lapRecordAdapter);
		
		lapListTitle = (LinearLayout)findViewById(R.id.lapListTitle);
		lapListTitle.setVisibility(View.INVISIBLE);
	}
	
	private void stopLapTimeThread() {
		if (null != lapTimeThread) {
			lapTimeThread = null;
		}
	}
	
	private void startLapTimeThread() {
		if (null == lapTimeThread) {
			try {
				lapTimeThread = new Thread(new Runnable () {
					@Override
					public void run() {
						// Moves the current thread to the background
						android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
						
						while (STOPWATCHSTT_START == stopWatchStt) {
					    //while (true) {
							Message msg = Message.obtain();
							if (startLap) {
								msg.what = STOPWATCH_UPDATE_LAP_MSG;
								startLap = false;
							} else {
								msg.what = STOPWATCH_UPDATE_TIME_MSG;
							}
							
							handler.sendMessage(msg);
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
				});
				lapTimeThread.start();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private Handler handler = new Handler () {
		@Override
		public void handleMessage(Message msg) {
			// judge if the code of msg is 1
			if (STOPWATCHSTT_START == stopWatchStt)
			{
				switch(msg.what) {
				case STOPWATCH_UPDATE_TIME_MSG:
					updateTime();
					break;
				//case STOPWATCH_UPDATE_LAP_MSG:
				//	updateLap();
				//	break;
				default:
					break;
				}
			}
		}
	};
	
	private String formatTime(long deltaTime) {
		Log.d(LOG_TAG, "Delta Time: " + deltaTime);
		int millis = (int)(deltaTime % 1000);
		deltaTime /= 1000;  // second unit
		int seconds = (int)(deltaTime % 60);
		deltaTime /= 60;    // minute unit
		int mins = (int)(deltaTime % 60);
		int hours = (int)(deltaTime / 60);   // hour unitf
		Log.d(LOG_TAG, "time: " + hours + ":" + mins + ":" + seconds + ":" + millis);
	    return String.format("%1$02d:%2$02d:%3$02d:%4$03d", hours, mins, seconds, millis);
	}
	
	/**
	 * This function performs to display the time from the start point
	 */
	private void updateTime() {
		long currentTime = System.currentTimeMillis();
		String timeString = formatTime(currentTime - baseTime);
		if (null != timeText) {
			timeText.setText(timeString);
		}
	}
	
	/**
	 * This function performs to calculate the lap
	 */
	private void updateLapList() {
		long currentTime = System.currentTimeMillis();
		String lapTimeString = formatTime(currentTime - baseTime);
		if (baseTime == prevTime) {
			prevTime = currentTime;
		}
		
		String deltaTimeString = formatTime(currentTime - prevTime);
		
		prevTime = currentTime;
		
		showLap(lapTimeString, deltaTimeString);
	}
	
	/**
	 * This function performs to show the lapTime
	 * */
	private void showLap(String lapTimeString, String deltaTimeString) {
		// show the title of lap list if it's the fist to add lap
		if (null != lapListTitle && lapListTitle.getVisibility() == View.INVISIBLE) {
			lapListTitle.setVisibility(View.VISIBLE);
		}
		
		if (null != lapRecordAdapter) {
			lapNum++;
			lapRecordAdapter.addLapRecord(new LapRecord("Lap"+lapNum, lapTimeString, deltaTimeString));
		}
	}
	
	private void clearLap() {
		// Clear timeText
		if (null != timeText) {
			timeText.setText("00:00:00:000");
		}
		
		// Clear the lap time list
		if (null != lapRecordAdapter) {
			lapRecordAdapter.clearAll();
		}
		
		// Reset the lap number
		lapNum = 0;
		
		if (null != timeLapButton) {
			timeLapButton.setText("Lap");
			timeLapButton.setEnabled(false);
		}
		
		// Hide the title of lap list
		if (null != lapListTitle) {
			lapListTitle.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stopwatch, menu);
		return true;
	}
	
	/**
	 * Description This function performs to require the wake lock, and then ask the screen keeps light if the wake lock is required
	 * */
	private void requireWakeLock() {
		if (null == wakeLock) {
			PowerManager powerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
			wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Stopwatch");
			if (null != wakeLock) {
				wakeLock.acquire();
			}
		}
	}
	
	/**
	 * Description: This function performs to release the wakeLock if it's required
	 * */
	private void releaseWakeLock() {
		if (null != wakeLock) {
			wakeLock.release();
			wakeLock = null;
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		releaseWakeLock();
	}
}
