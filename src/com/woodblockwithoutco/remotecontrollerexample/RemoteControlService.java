package com.woodblockwithoutco.remotecontrollerexample;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RemoteController;
import android.media.RemoteController.MetadataEditor;
import android.os.Binder;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.KeyEvent;

public class RemoteControlService extends NotificationListenerService implements RemoteController.OnClientUpdateListener {
	
	//�������(� ��������) ��� ������� �������
	private static final int BITMAP_HEIGHT = 1024;
	private static final int BITMAP_WIDTH = 1024;

	//���������� ��� ����, ����� �� ����� ������������ � �������
	private IBinder mBinder = new RCBinder();
	
	private RemoteController mRemoteController; //��� RemoteController
	private Context mContext;
	
	//callback ��� ������� ���������� �������
	private RemoteController.OnClientUpdateListener mExternalClientUpdateListener;
	
	@Override
	public IBinder onBind(Intent intent) {
		if(intent.getAction().equals("com.woodblockwithoutco.remotecontrollerexample.BIND_RC_CONTROL_SERVICE")) {
			return mBinder;
		} else {
			return super.onBind(intent);
		}
	}
	
	@Override
	public void onNotificationPosted(StatusBarNotification notification) {
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification notification) {
	}
	
	@Override
	public void onCreate() {
		//��������� �������� ���������� ��� ����������� �������������
		mContext = getApplicationContext();
		mRemoteController = new RemoteController(mContext, this);
	}
	
	@Override
	public void onDestroy() {
		setRemoteControllerDisabled();
	}
	
	//������, ������ �����, ����� ���������� ����� Binder � Activity
	
	/**
	 * �������� ��� RemoteController, �������� ��� �������� ���������� �� RemoteControlClient.
	 */
	public void setRemoteControllerEnabled() {
		if(!((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).registerRemoteController(mRemoteController)) {
			throw new RuntimeException("Error while registering RemoteController!");
		} else {
			mRemoteController.setArtworkConfiguration(BITMAP_WIDTH, BITMAP_HEIGHT);
			setSynchronizationMode(mRemoteController, RemoteController.POSITION_SYNCHRONIZATION_CHECK);
		}
	}
	
	/**
	 * ��������� ��� RemoteController. ���������� �� RemoteControlClient ��������� �� �����.
	 */
	public void setRemoteControllerDisabled() {
		((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).unregisterRemoteController(mRemoteController);
	}
	
	/**
	 * ������������� ������� callback ��� ������� ���������� RemoteControlClient.
	 * @param listener Callback ��� ����������.
	 */
	public void setClientUpdateListener(RemoteController.OnClientUpdateListener listener) {
		mExternalClientUpdateListener = listener;
	}
	
	/**
	 * �������� ������� ������ "������".
	 */
	public void sendNextKey() {
		sendKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
	}
	
	/**
	 * �������� ������� ������ "�����".
	 */
	public void sendPreviousKey() {
		sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
	}
	
	/**
	 * �������� ������� ������ "�����", ���, ���� ����� �� ������������ �� ��� ������, "������������/�����".
	 */
	public void sendPauseKey() {
		if(!sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PAUSE)) {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		}
	}
	
	/**
	 * �������� ������� ������ "������������", ���, ���� ����� �� ������������ �� ��� ������, "������������/�����".
	 */
	public void sendPlayKey() {
		if(!sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)) {
			sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
		}
	}
	
	/**
	 * @return ��������� ��������� � ����� � �������������.
	 */
	public long getEstimatedPosition() {
		return mRemoteController.getEstimatedMediaPosition();
	}
	
	/**
	 * ������������ ������ � ����� � ����������.
	 * @param ms ������ �� ������ � �������������.
	 */
	public void seekTo(long ms) {
		mRemoteController.seekTo(ms);
	}
	//����� ������� ��� Binder � Activity
	
	//helper-������
	private void setSynchronizationMode(RemoteController controller, int sync) {
		if ((sync != RemoteController.POSITION_SYNCHRONIZATION_NONE) && (sync != RemoteController.POSITION_SYNCHRONIZATION_CHECK)) {
            throw new IllegalArgumentException("Unknown synchronization mode " + sync);
        }
		
		Class<?> iRemoteControlDisplayClass;
		
		try {
			iRemoteControlDisplayClass  = Class.forName("android.media.IRemoteControlDisplay");
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException("Class IRemoteControlDisplay doesn't exist, can't access it with reflection");
		}
		
		Method remoteControlDisplayWantsPlaybackPositionSyncMethod;
		try {
			remoteControlDisplayWantsPlaybackPositionSyncMethod = AudioManager.class.getDeclaredMethod("remoteControlDisplayWantsPlaybackPositionSync", iRemoteControlDisplayClass, boolean.class);
			remoteControlDisplayWantsPlaybackPositionSyncMethod.setAccessible(true);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() doesn't exist, can't access it with reflection");
		}
		
		Object rcDisplay;
		Field rcDisplayField;
		try {
			rcDisplayField = RemoteController.class.getDeclaredField("mRcd");
			rcDisplayField.setAccessible(true);
			rcDisplay = rcDisplayField.get(mRemoteController);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Field mRcd doesn't exist, can't access it with reflection");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Field mRcd can't be accessed - access denied");
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Field mRcd can't be accessed - invalid argument");
		}
		
		AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		try {
			remoteControlDisplayWantsPlaybackPositionSyncMethod.invoke(am, iRemoteControlDisplayClass.cast(rcDisplay), true);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - access denied");
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid arguments");
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Method remoteControlDisplayWantsPlaybackPositionSync() invocation failure - invalid invocation target");
		}
	}
	
	private boolean sendKeyEvent(int keyCode) {
		//�������� ������� � ���������� ������.
		KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
		boolean first = mRemoteController.sendMediaKeyEvent(keyEvent);
		keyEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
		boolean second = mRemoteController.sendMediaKeyEvent(keyEvent);
		
		return first && second; //������� �� ���� �������� ������ �������
	}
	//����� helper-�������
	
	
	//Binder ��� ����������� Activity � �������
	public class RCBinder extends Binder {
        public RemoteControlService getService() {
            return RemoteControlService.this;
        }
    }

	//���������� ���������� RemoteController.OnClientUpdateListener. �������� ������� callback ��� ���������
	//��������� �� ��������� ��������� RemoteControlClient.
	@Override
	public void onClientChange(boolean arg0) {
		if(mExternalClientUpdateListener != null) {
			mExternalClientUpdateListener.onClientChange(arg0);
		}
	}

	@Override
	public void onClientMetadataUpdate(MetadataEditor arg0) {
		if(mExternalClientUpdateListener != null) {
			mExternalClientUpdateListener.onClientMetadataUpdate(arg0);
		}
	}

	@Override
	public void onClientPlaybackStateUpdate(int arg0) {
		if(mExternalClientUpdateListener != null) {
			mExternalClientUpdateListener.onClientPlaybackStateUpdate(arg0);
		}
	}

	@Override
	public void onClientPlaybackStateUpdate(int arg0, long arg1, long arg2, float arg3) {
		if(mExternalClientUpdateListener != null) {
			mExternalClientUpdateListener.onClientPlaybackStateUpdate(arg0, arg1, arg2, arg3);
		}
	}

	@Override
	public void onClientTransportControlUpdate(int arg0) {
		if(mExternalClientUpdateListener != null) {
			mExternalClientUpdateListener.onClientTransportControlUpdate(arg0);
		}
		
	}

	

}
