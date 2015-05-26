package com.ubhave.datastore.file;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

import com.ubhave.datahandler.config.DataHandlerConfig;
import com.ubhave.datahandler.config.DataStorageConfig;
import com.ubhave.datahandler.config.DataStorageConstants;
import com.ubhave.datahandler.except.DataHandlerException;

public class FileStoreWriter
{
	private static final String TAG = "LogFileDataStorage";

	private final DataHandlerConfig config;
	private final FileVault fileVault;
	private final FileStoreCleaner cleaner;
	
	public FileStoreWriter(final FileVault vault, final FileStoreCleaner cleaner)
	{
		this.config = DataHandlerConfig.getInstance();
		this.fileVault = vault;
		this.cleaner = cleaner;
	}
	
	public void writeData(final String directoryName, String data) throws DataHandlerException
	{
		String rootPath = (String) config.get(DataStorageConfig.LOCAL_STORAGE_ROOT_NAME);
		synchronized (FileVault.getLock(directoryName))
		{
			final File directory = getDirectory(rootPath, directoryName);
			try
			{
				File dataFile;
				if (!fileVault.isEncrypted())
				{
					dataFile = getLastestFile(directory);
					data += "\n";
				}
				else
				{
					dataFile = createNewFile(directory);
				}
				
				OutputStream cos = fileVault.openForWriting(dataFile);
				cos.write(data.getBytes());
				cos.flush();
				cos.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new DataHandlerException(DataHandlerException.IO_EXCEPTION);
			}
		}
	}
	
	private File getDirectory(final String rootPath, final String directoryName)
	{
		final File directory = new File(rootPath, directoryName);
		if (!directory.exists())
		{
			boolean directoryCreated = directory.mkdirs();
			if (DataHandlerConfig.shouldLog())
			{
				Log.d(TAG, "Created ["+directoryCreated+"]: " + directory.getAbsolutePath());
			}
		}
		return directory;
	}

	private File getLastestFile(final File directory) throws DataHandlerException, IOException
	{
		File latestFile = null;
		final File[] files = directory.listFiles();
		if (files != null)
		{
			long latestUpdate = Long.MIN_VALUE;
			for (File file : files)
			{
				if (file.isFile() && file.getName().endsWith(DataStorageConstants.JSON_FILE_SUFFIX))
				{
					long update = file.lastModified();
					if (update > latestUpdate)
					{
						latestUpdate = update;
						latestFile = file;
					}
				}
			}
		}
		if (latestFile == null)
		{
			return createNewFile(directory);
		}
		else if (fileVault.isDueForUpload(latestFile))
		{
			cleaner.moveDirectoryForUpload(directory);
			return createNewFile(directory);
		}
		else
		{
			return latestFile;
		}
	}
	
	private File createNewFile(final File directory) throws IOException
	{
		File file = new File(directory, System.currentTimeMillis() + DataStorageConstants.JSON_FILE_SUFFIX);
		while (file.exists())
		{
			file = new File(directory, System.currentTimeMillis() + DataStorageConstants.JSON_FILE_SUFFIX);
		}
		
		boolean fileCreated = file.createNewFile();
		if (DataHandlerConfig.shouldLog())
		{
			Log.d(TAG, "Created ["+fileCreated+"]: " + file.getAbsolutePath());
		}
		return file;
	}
}
