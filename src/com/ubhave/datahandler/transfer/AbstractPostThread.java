/* **************************************************
 Copyright (c) 2015, University of Cambridge
 Neal Lathia, neal.lathia@cl.cam.ac.uk

This application was developed as part of the EPSRC Ubhave (Ubiquitous and
Social Computing for Positive Behaviour Change) Project. For more
information, please visit http://www.emotionsense.org

 Contributors:
 Mihai Ghete <m.ghete@bikecityguide.org>, BikeCityGuide Apps GmbH, 2015

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ************************************************** */

package com.ubhave.datahandler.transfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import com.ubhave.datahandler.config.DataHandlerConfig;
import com.ubhave.datahandler.config.DataTransferConfig;
import com.ubhave.datahandler.except.DataHandlerException;

public abstract class AbstractPostThread extends Thread
{
	private final static int STATUS_OK = 200;
	private final String serverUrl, expectedResponse;
	
	protected final HashMap<String, String> params;
	protected final DataHandlerConfig config;
	protected final String postKey;
	
	protected AbstractPostThread() throws DataHandlerException
	{
		config = DataHandlerConfig.getInstance();
		this.serverUrl = (String) config.get(DataTransferConfig.POST_DATA_URL);
		this.expectedResponse = (String) config.get(DataTransferConfig.POST_RESPONSE_ON_SUCCESS);
		this.postKey = (String) config.get(DataTransferConfig.POST_KEY);
		this.params = getPostParams();
	}
	
	protected void post(final MultipartEntity multipartEntity) throws Exception
	{
		
		if (params != null)
		{
			for (String key : params.keySet())
			{
				String value = params.get(key);
				multipartEntity.addPart(key, new StringBody(value));
			}
		}
		
		HttpURLConnection conn = (HttpURLConnection)new URL(serverUrl).openConnection();
		conn.setConnectTimeout(60 * 1000);
		conn.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
		conn.setRequestProperty("Content-Length", Long.toString(multipartEntity.getContentLength()));
		conn.setDoOutput(true);
		
		multipartEntity.writeTo(conn.getOutputStream());

		int redirects = 0;
		while (conn.getResponseCode() == 302 && redirects < 5) {
			redirects++;
			String redirectUrl = conn.getHeaderField("Location");
			conn.disconnect();
			
			conn = (HttpURLConnection)new URL(redirectUrl).openConnection();
			conn.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());
			conn.setRequestProperty("Content-Length", Long.toString(multipartEntity.getContentLength()));
			conn.setConnectTimeout(60 * 1000);
			conn.setDoOutput(true);
			
			multipartEntity.writeTo(conn.getOutputStream());
		}
		
		String response = convertStreamToString(conn.getInputStream());
		int status = conn.getResponseCode();
		
		if (status != STATUS_OK || !expectedResponse.equals(response))
		{
			throw new DataHandlerException(DataHandlerException.POST_FAILED);
		}
	}
	
	private String convertStreamToString(InputStream is)
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				sb.append((line));
			}
			reader.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	protected HashMap<String, String> getPostParams()
	{
		HashMap<String, String> paramsMap = new HashMap<String, String>();
		if (config.containsConfig(DataTransferConfig.POST_PARAMETERS))
		{
			try
			{
				JSONObject json = (JSONObject) config.get(DataTransferConfig.POST_PARAMETERS);
				Iterator<?> keyIterator = json.keys();
				while (keyIterator.hasNext())
				{
					try
					{
						String key = (String) keyIterator.next();
						String value = json.getString(key);
						paramsMap.put(key, value);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			}
			catch (DataHandlerException e)
			{
				e.printStackTrace();
			}
		}
		return paramsMap;
	}
}
