package com.example.user.musicplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class PlayerActivity extends AppCompatActivity {

  public static final int STATUS_IDLE = 1;
  public static final int STATUS_PLAYING = 2;
  public static final int STATUS_PAUSED = 3;
  public static final int STATUS_STOPPED = 4;
  public static final String EXTRA_STATUS = "EXTRA_STATUS";
  public static final String ACTION_PROGRESS = "ACTION_PROGRESS";
  public static final String ACTION_STATUS = "ACTION_STATUS";
  public static final String EXTRA_SEEKBAR_PROGRESS = "EXTRA_SEEKBAR_PROGRESS";

  private Button playPauseButton;
  private Button stopButton;
  private TextView statusTextView;
  private SeekBar seekBar;

  private int playerStatus = STATUS_IDLE;
  private ServiceConnection serviceConnection;
  private BroadcastReceiver playerBroadcastReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.a_player);

    initViews();

    serviceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) iBinder;
        playerStatus = binder.getStatus();
        seekBar.setMax(binder.getAudioDuration());
        updateViews();
      }

      @Override
      public void onServiceDisconnected(ComponentName componentName) {
        //do nothing
      }
    };

    bindService(new Intent(this, PlayerService.class), serviceConnection, BIND_AUTO_CREATE);

    playerBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(ACTION_PROGRESS)) {
          int progress = intent.getIntExtra(EXTRA_SEEKBAR_PROGRESS, -1);
          seekBar.setProgress(progress);
        } else{
          playerStatus = STATUS_STOPPED;
          updateViews();
        }
      }
    };
    IntentFilter intentFilter = new IntentFilter(ACTION_PROGRESS);
    intentFilter.addAction(ACTION_STATUS);
    registerReceiver(playerBroadcastReceiver, intentFilter);

  }

  @Override
  protected void onDestroy() {
    unbindService(serviceConnection);
    unregisterReceiver(playerBroadcastReceiver);
    super.onDestroy();
  }

  private void updateViews() {
    playPauseButton.setText(R.string.play);
    switch (playerStatus) {
      case STATUS_IDLE:
        statusTextView.setText(R.string.idle);
        break;
      case STATUS_PAUSED:
        statusTextView.setText(R.string.paused);
        break;
      case STATUS_PLAYING:
        statusTextView.setText(R.string.playing);
        playPauseButton.setText(R.string.pause);
        break;
      case STATUS_STOPPED:
        seekBar.setProgress(0);
        statusTextView.setText(R.string.stopped);
        break;
    }
  }

  private void initViews() {
    seekBar = (SeekBar) findViewById(R.id.sb_player_seek_bar);
    playPauseButton = (Button) findViewById(R.id.b_playPause);
    stopButton = (Button) findViewById(R.id.b_stop);
    statusTextView = (TextView) findViewById(R.id.tv_status);
    playPauseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        playerStatus = playerStatus == STATUS_PLAYING ? STATUS_PAUSED : STATUS_PLAYING;
        updateViews();
        startService(new Intent(PlayerActivity.this, PlayerService.class).putExtra(EXTRA_STATUS, playerStatus).setAction(ACTION_STATUS));
      }
    });
    stopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        playerStatus = STATUS_STOPPED;
        updateViews();
        startService(new Intent(PlayerActivity.this, PlayerService.class).putExtra(EXTRA_STATUS, playerStatus).setAction(ACTION_STATUS));
      }
    });

    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean changedByUser) {
        if (changedByUser) {
          startService(new Intent(PlayerActivity.this, PlayerService.class).putExtra(EXTRA_SEEKBAR_PROGRESS, progress));
        }
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
        //do nothing
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        //do nothing
      }
    });

  }
}
