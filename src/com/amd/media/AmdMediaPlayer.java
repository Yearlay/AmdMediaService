package com.amd.media;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import com.haoke.constant.MediaUtil.FileType;
import com.haoke.constant.MediaUtil.MediaState;
import com.haoke.util.DebugLog;
import com.haoke.video.RearView;
import com.haoke.video.VideoSurfaceView;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * 本类实现MediaPlayer方法封装
 */
public class AmdMediaPlayer {

	private final String TAG = this.getClass().getSimpleName();
	private Context mContext = null;
	private RearView mRearView = null;
	private MediaPlayer mMediaPlayer = null;
	private AmdMediaPlayerListener mListener = null;
	private int mMediaState = MediaState.IDLE;
	private SurfaceHolder mSurfaceHolder = null;
	private int mFileType = FileType.NULL;
	private boolean mHasFocus = false;
	
	public AmdMediaPlayer(Context context) {
		mContext = context;
		renew();
	}

	public void setFileType(int fileType) {
		DebugLog.v(TAG, "setFileType fileType=" + fileType);
		mFileType = fileType;
	}
	
	public boolean hasFocus() {
		return mHasFocus;
	}
	
	public void focusGain() {
		DebugLog.v(TAG, "focusGain");
		mHasFocus = true;
	}

	public void focusLossed() {
		DebugLog.v(TAG, "focusLossed");
		mHasFocus = false;
		if (mRearView != null) {
			mRearView.showRearSurface(false);
		}
	}

	public MediaPlayer getMediaPlayer() {
		return mMediaPlayer;
	}

	public void setMediaPlayerListener(AmdMediaPlayerListener listener) {
		mListener = listener;
	}

	public boolean setDataSource(String path) {
		DebugLog.v(TAG, "setDataSource path=" + path);
		synchronized (AmdMediaPlayer.this) {
			if (path == null) {
				// 路径空
				setMediaState(MediaState.ERROR);
				return false;
			}
			setMediaState(MediaState.PREPARING);

			FileInputStream is = null;
			try {
				mMediaPlayer.reset();
				mMediaPlayer.setOnPreparedListener(null);

				if (path.startsWith("content://")) {
					mMediaPlayer.setDataSource(mContext, Uri.parse(path));
				} else {
					final File file = new File(path);
					if (file.exists()) {
						is = new FileInputStream(file);
						FileDescriptor fd = is.getFD();
						mMediaPlayer.setDataSource(fd);
						is.close();
					}
				}
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mMediaPlayer.setOnPreparedListener(mPreparedListener);
				
				if (mListener != null) {
					mListener.onPreparing();
				}
				if (mFileType == FileType.VIDEO && mSurfaceHolder == null) {
				} else {
					mMediaPlayer.prepare();
				}
				
			} catch (IOException e) {
				DebugLog.e(TAG, "setDataSource IOException e=" + e);

				if (is != null) {
					try {
						is.close();
						is = null;
					} catch (Exception ex) {
						is = null;
					}
				}
				setMediaState(MediaState.ERROR);

				if (mListener != null)
					mListener.onIOException();
				return false;

			} catch (IllegalArgumentException e) {
				DebugLog.e(TAG, "setDataSource IllegalArgumentException e=" + e);

				setMediaState(MediaState.ERROR);

				if (mListener != null)
					mListener.onIOException();
				return false;

			} catch (IllegalStateException e) {
				DebugLog.e(TAG, "setDataSource IllegalStateException e=" + e);

				setMediaState(MediaState.ERROR);

				if (mListener != null)
					mListener.onIOException();
				return false;
			}

			mMediaPlayer.setOnCompletionListener(mCompletionListener);
			mMediaPlayer.setOnErrorListener(mErrorListener);
			mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
			return true;
		}
	}

	MediaPlayer.OnPreparedListener mPreparedListener = new OnPreparedListener() {
		@Override
		public void onPrepared(MediaPlayer mp) {
			// TODO Auto-generated method stub
			DebugLog.v(TAG, "mPreparedListener onPrepared mHasFocus="+mHasFocus);
			synchronized (AmdMediaPlayer.this) {
				setMediaState(MediaState.PREPARED);
				if (mListener != null) {
					mListener.onPrepared();
				}
				if (mHasFocus) {
					start();
				}
			}
			DebugLog.v(TAG, "mPreparedListener onPrepared over");
		}
	};

	MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub
			DebugLog.v(TAG, "mCompletionListener onCompletion");
			synchronized (AmdMediaPlayer.this) {
				if (getMediaState() == MediaState.PREPARED) {
					if (mListener != null)
						mListener.onCompletion();
				}
			}
		}
	};

	MediaPlayer.OnErrorListener mErrorListener = new MediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			DebugLog.e(TAG, "mErrorListener onError what:" + what + " extra:"
					+ extra);
			synchronized (AmdMediaPlayer.this) {
				setMediaState(MediaState.ERROR);

				switch (what) {
				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    renew();
                    if (mListener != null)
                        mListener.onServerDied();
                    return true;
				case MediaPlayer.MEDIA_ERROR_UNKNOWN:
					renew();
					if (mListener != null)
						mListener.onError();
					return true;

				case -38:
					renew();
					return true;

				default:
					reset();
					if (mListener != null)
						mListener.onError();
					break;
				}
				return false;
			}
		}
	};

	MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new MediaPlayer.OnSeekCompleteListener() {
		@Override
		public void onSeekComplete(MediaPlayer arg0) {
			// TODO Auto-generated method stub
			DebugLog.d(TAG, "mSeekCompleteListener onSeekComplete");
			if (getMediaState() == MediaState.PREPARED) {
				if (mListener != null)
					mListener.onSeekCompletion();
			}
		}
	};

	public void setMediaState(int state) {
		synchronized (AmdMediaPlayer.this) {
			mMediaState = state;
		}
	}

	public int getMediaState() {
		synchronized (AmdMediaPlayer.this) {
			return mMediaState;
		}
	}

	public boolean isPlaying() {
		synchronized (AmdMediaPlayer.this) {
			return mMediaPlayer.isPlaying();
		}
	}

	public void start() {
		boolean isPlaying = isPlaying();
		DebugLog.v(TAG, "start mMediaPlayer=" + mMediaPlayer + ", mMediaState="
				+ getMediaState() + ", isPlaying=" + isPlaying);
		synchronized (AmdMediaPlayer.this) {
			// modify by lyb 20170407
			if (getMediaState() == MediaState.PREPARED && isPlaying == false) {
				DebugLog.v(TAG, "start mMediaPlayer.start()");
				mMediaPlayer.start();
				if (mListener != null) {
					mListener.onStart();
				}
			}
		}
	}

	public void pause() {
	    int mediaState = getMediaState();
		DebugLog.v(TAG, "pause mMediaPlayer=" + mMediaPlayer + ", mMediaState="
				+ mediaState);
		synchronized (AmdMediaPlayer.this) {
			if (mediaState == MediaState.PREPARED) {
				mMediaPlayer.pause();
				if (mListener != null) {
					mListener.onPause();
				}
			} else if (mediaState == MediaState.PREPARING) {
			    reset();
			    if (mListener != null) {
                    mListener.onPause();
                }
			}
		}
	}

	public void stop() {
		DebugLog.v(TAG, "stop mMediaPlayer=" + mMediaPlayer);
		synchronized (AmdMediaPlayer.this) {
			setMediaState(MediaState.IDLE);
			mMediaPlayer.stop();
			if (mListener != null) {
				mListener.onStop();
			}
		}
	}

	public int getDuration() {
		synchronized (AmdMediaPlayer.this) {
			if (getMediaState() == MediaState.PREPARED) {
				int value = mMediaPlayer.getDuration();
				if (value > 10000000) // add by lyb 20170405 避免MediaPlayer返回乱码导致显示错误
					return 0;
				return value;
			}
			return 0;
		}
	}

	public int getPosition() {
		synchronized (AmdMediaPlayer.this) {
			if (getMediaState() == MediaState.PREPARED) {
				int value = mMediaPlayer.getCurrentPosition();
				if (value > 10000000) // add by lyb 20170405 避免MediaPlayer返回乱码导致显示错误
					return 0;
				return value;
			}
			return 0;
		}
	}

	public int seekTo(int time) {
		synchronized (AmdMediaPlayer.this) {
			DebugLog.v(TAG, "seekTo time=" + time + ", mMediaState="
					+ getMediaState());
			if (getMediaState() == MediaState.PREPARED) {
				if (time < 1)
					time = 1; // yyp2015/07/13

				if (time > mMediaPlayer.getDuration())
					time = mMediaPlayer.getDuration();

				// add by yyp 2015/06/10 for some file.WMA seek to last can not
				// receive completion;
				if (mMediaPlayer.getDuration() / 1000 == time / 1000) {
					// end of file,
					time = time - 1000;
				}
				DebugLog.v(TAG, "seekTo duration=" + mMediaPlayer.getDuration());
				mMediaPlayer.seekTo((int) time);

			} else {
				DebugLog.w(TAG, "seekTo, mIsInitialized is false!!");
			}
			return time;
		}
	}

	private void renew() {
		DebugLog.v(TAG, "renew mMediaPlayer=" + mMediaPlayer);
		synchronized (AmdMediaPlayer.this) {
			setMediaState(MediaState.IDLE);
			if (mMediaPlayer != null) {
				mMediaPlayer.release();
				mMediaPlayer = null;
			}
			mMediaPlayer = new MediaPlayer();
		}
	}

	public void reset() {
		DebugLog.v(TAG, "reset");
		synchronized (AmdMediaPlayer.this) {
			setMediaState(MediaState.IDLE);
			mMediaPlayer.reset();
		}
	}

	public void setVolume(float vol) {
		synchronized (AmdMediaPlayer.this) {
			mMediaPlayer.setVolume(vol, vol);
		}
	}

	public void setAudioSessionId(int sessionId) {
		synchronized (AmdMediaPlayer.this) {
			mMediaPlayer.setAudioSessionId(sessionId);
		}
	}

	public int getAudioSessionId() {
		synchronized (AmdMediaPlayer.this) {
			return mMediaPlayer.getAudioSessionId();
		}
	}

	public void setVideoView(VideoSurfaceView surfaceView) {
		synchronized (AmdMediaPlayer.this) {
			surfaceView.getHolder().addCallback(mSHCallback);
		}
	}

	public SurfaceHolder getSurfaceHolder() {
		return mSurfaceHolder;
	}

	SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceChanged(SurfaceHolder holder, int arg1, int arg2,
				int arg3) {
			// TODO Auto-generated method stub
			DebugLog.v(TAG, "mSHCallback, surfaceChanged");
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			DebugLog.v(TAG, "mSHCallback, surfaceCreated");
			synchronized (AmdMediaPlayer.this) {
				try {
					mSurfaceHolder = holder;
					resetDisplay();
					if (getMediaState() == MediaState.PREPARED) {
//						start();      //del, fix bug 16891
					} else {
						mMediaPlayer.prepare();
					}
					if (mRearView != null) {
						mRearView.showRearSurface(true);
					}
					if (mListener != null) {
						mListener.onSurfaceCreated();
					}
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					DebugLog.e(TAG, "mHandler IllegalStateException");
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					DebugLog.e(TAG, "mHandler IOException");
					e.printStackTrace();
				}
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder arg0) {
			// TODO Auto-generated method stub
			DebugLog.v(TAG, "mSHCallback, surfaceDestroyed");
			synchronized (AmdMediaPlayer.this) {
				mSurfaceHolder = null;
				// 只有切到非音频源，才可以执行reset
				// 避免从视频切音频后，surfaceDestroyed比音频的start后执行，导致音频无法播放
				if (mFileType == FileType.VIDEO) {
					DebugLog.v(TAG, "mSHCallback, surfaceDestroyed pause");
					// pause();           //del, fix bug 16891
				}
				resetDisplay();
				if (mListener != null)
					mListener.onSurfaceDestroyed();
			}
		}
	};
	
	public void resetDisplay() {
		synchronized (AmdMediaPlayer.this) {
			DebugLog.v(TAG, "resetDisplay, mSurfaceHolder:" + mSurfaceHolder);
			try {
				if (mSurfaceHolder != null) {
					mMediaPlayer.setDisplay(mSurfaceHolder);
				} else {
					mMediaPlayer.setDisplay(null);
				}
			} catch (IllegalStateException e) {
				DebugLog.e(TAG, "resetDisplay, IllegalStateException: " + e);
				e.printStackTrace();
			} catch (Exception e) {
				DebugLog.e(TAG, "resetDisplay, Exception: " + e);
				e.printStackTrace();
			}
		}
	}

}
