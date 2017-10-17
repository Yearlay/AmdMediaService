package com.haoke.media;

public interface MediaPlayerListener {

	public void onStart();
	public void onPause();
	public void onStop();
	public void onPreparing();
	public void onPrepared();
	public void onCompletion();
	public void onSeekCompletion();
	public void onError();
	public void onIOException();
	public void onSurfaceCreated();
	public void onSurfaceDestroyed();
}
