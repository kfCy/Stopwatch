package com.example.stopwatch;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
	
	// This thread performs to get the current time and send it to UI thread to be displayed
	private Thread updateTimeThread = null;
	private long baseTime = 0;
	private long prevTime = 0;
	
	// This thread performs to get the lap time and handle it and send it to UI thread to be displayed
	private Thread lapTimeThread = null;
	private Handler lapTimeHandler = null;
	
	private final int STOPWATCH_UPDATE_TIME_MSG = 0;
	private final int STOPWATCH_CAL_LAP_TIME = 1;
	private final int STOPWATCH_ADD_LAP = 2;
	
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
				    stopStopwatch();
					
				} else if (STOPWATCHSTT_STOP == stopWatchStt) {
					// Should start stopWatch
					startStopwatch();
				}
			}
		});
		
		timeLapButton = (Button)findViewById(R.id.timeLapButton);
		timeLapButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (LAPBTN_STT_LAP == lapBtnStt) {
					sendMsgToCalLapTime();
				} else if (LAPBTN_STT_RESET == lapBtnStt) {
					clearLap();
				}
			}
		});
		// Should disable the timeLapButton when first entering to the Stopwatch
		timeLapButton.setEnabled(false);
		
		lapRecordAdapter = new LapRecordAdapter(this);
		lapListView = (ListView)findViewById(R.id.lapListView);
		lapListView.setAdapter(lapRecordAdapter);
		
		lapListTitle = (LinearLayout)findViewById(R.id.lapListTitle);
		lapListTitle.setVisibility(View.INVISIBLE);
	}
	
	private void stopStopwatch() {
		// Change the text of startButton to "Start"
		startButton.setText("Start");
		// Change the text of timeLapButton to "Reset"
		timeLapButton.setText("Reset");
		
		lapBtnStt = LAPBTN_STT_RESET;
		// Changing the status of Stopwatch to STOPWATCHSTT_STOP to stop the child thread 
		stopWatchStt = STOPWATCHSTT_STOP;
		
		// Stop the thread of updating time
		stopUpdateTimeThread();
		
		// Stop the thread of lap time
		stopLapTimeThread();
		
		releaseWakeLock();
	}
	
	private void startStopwatch() {
		// Change the text of startButton
		startButton.setText("Stop");
		
		timeLapButton.setEnabled(true);
		timeLapButton.setText("Lap");
		
		lapBtnStt = LAPBTN_STT_LAP;
		stopWatchStt = STOPWATCHSTT_START;
		
		baseTime = System.currentTimeMillis();
		prevTime = baseTime;
		
		// Start another thread to update the time
		startUpdateTimeThread();
		
		// Also start lapTimeThread to receive the msg from Lap Button 
		// and then calculate the current time and delta time
		startLapTimeThread();
		
		requireWakeLock();
	}
	
	private void stopUpdateTimeThread() {
		if (null != updateTimeThread) {
			updateTimeThread = null;
		}
	}
	
	private void startUpdateTimeThread() {
		if (null == updateTimeThread) {
			try {
				updateTimeThread = new Thread(new Runnable () {
					@Override
					public void run() {
						// Moves the current thread to the background
						android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
						
						while (STOPWATCHSTT_START == stopWatchStt) {
							long currentTime = System.currentTimeMillis();
							String timeString = formatTime(currentTime - baseTime);
							
							Message msg = Message.obtain();
							msg.what = STOPWATCH_UPDATE_TIME_MSG;
							msg.obj = timeString;
							
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
				updateTimeThread.start();
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void startLapTimeThread () {
		if (null == lapTimeThread) {
			try {
				lapTimeThread = new Thread (new Runnable () {
					public void run () {
						Looper.prepare();
						
						lapTimeHandler = new Handler() {
							@Override
							public void handleMessage(Message msg) {
								switch(msg.what) {
								case STOPWATCH_CAL_LAP_TIME:
									LapRecord lapRecord = calLapTime();
									sendMessageToUIThread(lapRecord);
									break;
								}
							}
						};
						
						Looper.loop();
					}
					
					private void sendMessageToUIThread(LapRecord lapRecord) {
						Message msg = Message.obtain();
						msg.what = STOPWATCH_ADD_LAP;
						msg.obj = lapRecord;
						handler.sendMessage(msg);
					}
				});
				
				lapTimeThread.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void stopLapTimeThread() {
		if (null != lapTimeThread) {
			lapTimeThread = null;
		}
		
		if (null != lapTimeHandler) {
			lapTimeHandler.getLooper().quit();
		}
	}
	
	/**
	 *  This function performs to send the message of calculating the Lap time and delta time
	 */
	private void sendMsgToCalLapTime() {
		if (null != lapTimeHandler) {
			Message msg = Message.obtain();
			msg.what = STOPWATCH_CAL_LAP_TIME;
			lapTimeHandler.sendMessage(msg);
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
					showTime((String)msg.obj);
					break;
				case STOPWATCH_ADD_LAP:
					showLap((LapRecord)msg.obj);
					break;
				default:
					break;
				}
			}
		}
	};
	
	private String formatTime(long deltaTime) {
		//Log.d(LOG_TAG, "Delta Time: " + deltaTime);
		int millis = (int)(deltaTime % 1000);
		deltaTime /= 1000;  // second unit
		int seconds = (int)(deltaTime % 60);
		deltaTime /= 60;    // minute unit
		int mins = (int)(deltaTime % 60);
		int hours = (int)(deltaTime / 60);   // hour unitf
		//Log.d(LOG_TAG, "time: " + hours + ":" + mins + ":" + seconds + ":" + millis);
	    return String.format("%1$02d:%2$02d:%3$02d:%4$03d", hours, mins, seconds, millis);
	}
	
	/**
	 * This function performs to display the time from the start point
	 */
	private void showTime(String timeString) {
		if (null != timeText) {
			timeText.setText(timeString);
		}
	}
	
	/**
	 * This function performs to calculate the lap
	 */
	private LapRecord calLapTime() {
		long currentTime = System.currentTimeMillis();
		String lapTimeString = formatTime(currentTime - baseTime);
		if (baseTime == prevTime) {
			prevTime = currentTime;
		}
		
		String deltaTimeString = formatTime(currentTime - prevTime);
		
		prevTime = currentTime;
		
		return new LapRecord("Lap"+lapNum, lapTimeString, deltaTimeString);
	}
	
	/**
	 * This function performs to show the lapTime
	 * */
	private void showLap(LapRecord lapRecord) {
		// show the title of lap list if it's the fist to add lap
		if (null != lapListTitle && lapListTitle.getVisibility() == View.INVISIBLE) {
			lapListTitle.setVisibility(View.VISIBLE);
		}
		
		if (null != lapRecordAdapter) {
			lapNum++;
			lapRecordAdapter.addLapRecord(lapRecord);
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
	 * Description This function performs to require the wake lock, and then ask the screen keeps bright if the wake lock is required
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
