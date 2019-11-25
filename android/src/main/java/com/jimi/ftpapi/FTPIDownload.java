package com.jimi.ftpapi;

public interface FTPIDownload {
	
	public int STATE_NORMAL = 0;
	
	public int STATE_PLAY = 1;
	public int STATE_WAIT = 2;
	public int STATE_PAUSE = 3;
	public int STATE_INTERRUPT =4;
	public int STATE_DONE = 5;

	public int STATE_ERROR_IO = 1<<4;
	public int STATE_ERROR_SPACE_FULL = 2<<4;
	public int STATE_ERROR_TIME_OUT = 3<<4;
	public int STATE_ERROR_NETWORKS = 4<<4;
	public int STATE_ERROR_SERVER = 5<<4;
	
	public void download(String pUrl, String pSavePath);
	
	public void setState(int pState);
}
