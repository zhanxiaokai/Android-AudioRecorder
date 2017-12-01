package com.phuket.tour.audiorecorder;

import java.util.Timer;
import java.util.TimerTask;

import com.phuket.tour.audiorecorder.recorder.AudioConfigurationException;
import com.phuket.tour.audiorecorder.recorder.AudioRecordRecorderService;
import com.phuket.tour.audiorecorder.recorder.StartRecordingException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
/**
 * 点击record开始录音，点击stop停止录音
 * 利用adb pull 导出PCM文件
 * 		adb pull /mnt/sdcard/vocal.pcm ~/Desktop/
 * 利用ffplay播放声音
 * 		ffplay -f s16le  -sample_rate 44100  -channels 1 -i ~/Desktop/vocal.pcm
 * 利用ffmpeg将PCM文件转换为WAV文件
 * 		ffmpeg -f s16le  -sample_rate 44100  -channels 1 -i ~/Desktop/vocal.pcm -acodec pcm_s16le ~/Desktop/ssss.wav
 */
public class AudioRecorderActivity extends Activity {

	private TextView recorder_time_tip;
	private Button recorder_btn;
	private static final int DISPLAY_RECORDING_TIME_FLAG = 100000;
	private int record = R.string.record;
	private int stop = R.string.stop;

	private boolean isRecording = false;
	private AudioRecordRecorderService recorderService;
	private String outputPath = "/mnt/sdcard/vocal.pcm";
	private Timer timer;
	private int recordingTimeInSecs = 0;
	private TimerTask displayRecordingTimeTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		findView();
		initView();
		bindListener();
	}

	private void findView() {
		recorder_time_tip = (TextView) findViewById(R.id.recorder_time_tip);
		recorder_btn = (Button) findViewById(R.id.recorder_btn);
	}

	private void initView() {
		String timeTip = "00:00";
		recorder_time_tip.setText(timeTip);
	}

	private void bindListener() {
		recorder_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isRecording) {
					isRecording = false;
					recorder_btn.setText(getString(record));
					recordingTimeInSecs = 0;
					recorderService.stop();
					mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
					displayRecordingTimeTask.cancel();
					timer.cancel();
				} else {
					isRecording = true;
					recorder_btn.setText(getString(stop));
					//启动AudioRecorder来录音
					recorderService = AudioRecordRecorderService.getInstance();
					try {
						recorderService.initMetaData();
						recorderService.start(outputPath);
						//启动一个定时器来监测时间
						recordingTimeInSecs = 0;
						timer = new Timer();
						displayRecordingTimeTask = new TimerTask() {
							@Override
							public void run() {
								mHandler.sendEmptyMessage(DISPLAY_RECORDING_TIME_FLAG);
								recordingTimeInSecs++;
							}
						};
						timer.schedule(displayRecordingTimeTask, 0, 1000);
					} catch (AudioConfigurationException e) {
						Toast.makeText(AudioRecorderActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();;
					} catch (StartRecordingException e) {
						Toast.makeText(AudioRecorderActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();;
					}
				}
			}
		});
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DISPLAY_RECORDING_TIME_FLAG:
				int minutes = recordingTimeInSecs / 60;
				int seconds = recordingTimeInSecs % 60;
				String timeTip = String.format("%02d:%02d", minutes, seconds);
				recorder_time_tip.setText(timeTip);
				break;
			default:
				break;
			}
		}
	};
}
