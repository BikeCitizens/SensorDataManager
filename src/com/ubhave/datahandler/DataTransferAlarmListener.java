package com.ubhave.datahandler;

import android.content.Context;
import android.content.Intent;

import com.ubhave.datahandler.alarm.AlarmListener;
import com.ubhave.datahandler.alarm.PolicyAlarm;
import com.ubhave.datahandler.config.DataHandlerConstants;
import com.ubhave.datahandler.config.DataTransferConfig;

public class DataTransferAlarmListener implements AlarmListener
{
	private final Context context;
	private final ESDataManager dataManager;
	private final PolicyAlarm policyAlarm;
	
	public DataTransferAlarmListener(final Context context, final ESDataManager dataManager)
	{
		this.context = context;
		this.dataManager = dataManager;
		policyAlarm = getPolicyAlarm();
	}
	
	public void setConnectionTypeAndStart(int connectionType)
	{
		if (connectionType == DataTransferConfig.CONNECTION_TYPE_WIFI)
		{
			policyAlarm.setTransferPolicy(PolicyAlarm.TRANSFER_POLICY.WIFI_ONLY);
		}
		else if (connectionType == DataTransferConfig.CONNECTION_TYPE_ANY)
		{
			policyAlarm.setTransferPolicy(PolicyAlarm.TRANSFER_POLICY.ANY_NETWORK);
		}
		else if (connectionType == DataTransferConfig.CONNECTION_TYPE_WIFI_NO_TIMEOUT)
		{
			policyAlarm.setTransferPolicy(PolicyAlarm.TRANSFER_POLICY.WIFI_ONLY_NO_TIMEOUT);
		}
		
		policyAlarm.setListener(this);
		policyAlarm.start();
	}
	
	public void configUpdated()
	{
		policyAlarm.alarmIntervalUpdated();
	}
	
	public void stop()
	{
		policyAlarm.stop();
	}
	
	private PolicyAlarm getPolicyAlarm()
	{
		return new PolicyAlarm(DataHandlerConstants.TRANSFER_ALARM_ID, context,
				new Intent(DataHandlerConstants.ACTION_NAME_DATA_TRANSFER_ALARM),
				DataHandlerConstants.REQUEST_CODE_DATA_TRANSFER,
				DataHandlerConstants.ACTION_NAME_DATA_TRANSFER_ALARM,
				DataTransferConfig.TRANSFER_ALARM_INTERVAL,
				DataTransferConfig.WAIT_FOR_WIFI_INTERVAL_MILLIS);
	}
	
	@Override
	public boolean intentMatches(final Intent intent)
	{
		return true;
	}

	@Override
	public void alarmTriggered()
	{
		new Thread()
		{
			public void run()
			{
				dataManager.transferStoredData();
			}
		}.start();
	}
}
