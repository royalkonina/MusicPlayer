package com.example.user.musicplayer;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.IOException;

public class PlayerService extends Service {

  private MediaPlayer mediaPlayer;
  private int status = PlayerActivity.STATUS_IDLE;
  public static final int SERVICE_ID = 214;
  private static final Uri MUSIC_FILE = Uri.parse("android.resource://com.example.user.musicplayer/raw/sample_music_file");

  @Override
  public void onCreate() {
    startForeground(SERVICE_ID, getNotification());
    mediaPlayer = new MediaPlayer();
    try {
      mediaPlayer.setDataSource(getApplicationContext(), MUSIC_FILE);
    } catch (IOException e) {
      e.printStackTrace();
    }
    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        sendBroadcast(new Intent(PlayerActivity.ACTION_STATUS).putExtra(PlayerActivity.EXTRA_STATUS, PlayerActivity.STATUS_STOPPED));
        stopForeground(true);
      }
    });
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    mediaPlayer.stop();
    mediaPlayer.release();
    mediaPlayer = null;
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent.getAction() != null && intent.getAction().equals(PlayerActivity.ACTION_STATUS)) {
      int newStatus = intent.getIntExtra(PlayerActivity.EXTRA_STATUS, PlayerActivity.STATUS_IDLE);
      switch (newStatus) {
        case PlayerActivity.STATUS_IDLE:
          //do nothing
          break;
        case PlayerActivity.STATUS_PAUSED:
          mediaPlayer.pause();
          break;

        case PlayerActivity.STATUS_STOPPED:
          mediaPlayer.seekTo(0);
          mediaPlayer.stop();
          stopForeground(true);
          break;
        case PlayerActivity.STATUS_PLAYING:
          if (status == PlayerActivity.STATUS_IDLE || status == PlayerActivity.STATUS_STOPPED) {
            startForeground(SERVICE_ID, getNotification());
            try {
              Log.d("status", String.valueOf(status));
              mediaPlayer.prepare();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          mediaPlayer.start();
          new Thread(new Runnable() {
            @Override
            public void run() {
              while (mediaPlayer.isPlaying()) {
                sendBroadcast(new Intent(PlayerActivity.ACTION_PROGRESS).putExtra(PlayerActivity.EXTRA_SEEKBAR_PROGRESS, mediaPlayer.getCurrentPosition() / 1000));
              }
            }
          }).start();
          break;
      }
      status = newStatus;
    } else {
      int currentTime = intent.getIntExtra(PlayerActivity.EXTRA_SEEKBAR_PROGRESS, 0);
      mediaPlayer.seekTo(currentTime * 1000);
    }
    return super.onStartCommand(intent, flags, startId);
  }

  private Notification getNotification() {
    RemoteViews views = new RemoteViews(getPackageName(),
            R.layout.notification_panel);
    views.setViewVisibility(R.id.b_play_notification, View.VISIBLE);
    Intent stopIntent = new Intent();
    stopIntent.setAction(PlayerActivity.ACTION_STATUS);
    stopIntent.putExtra(PlayerActivity.EXTRA_STATUS, PlayerActivity.STATUS_STOPPED);
    views.setOnClickPendingIntent(R.id.b_play_notification, PendingIntent.getService(this, 0, stopIntent, 0));

    Intent playIntent = new Intent();
    playIntent.setAction(PlayerActivity.ACTION_STATUS);
    playIntent.putExtra(PlayerActivity.EXTRA_STATUS, PlayerActivity.STATUS_PLAYING);
    views.setOnClickPendingIntent(R.id.b_play_notification, PendingIntent.getService(this, 0, playIntent, 0));

    Notification.Builder builder = new Notification.Builder(this);
    @SuppressWarnings("deprecation")
    Notification notification=builder.getNotification();

    notification.contentView = views;
    return notification;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return new PlayerBinder();
  }

  class PlayerBinder extends Binder {
    public PlayerBinder() {
    }

    public int getStatus() {
      return status;
    }

    public int getAudioDuration() {
      MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
      metaRetriever.setDataSource(getApplicationContext(), MUSIC_FILE);
      return Integer.parseInt(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000;
    }

  }


}
