package com.markusherzog.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class HttpClient {
	
	public static final String POST = "POST";
	public static final String GET = "GET";

	private static final String BOUNDARY = "V2ymHFg03ehbqgZCaKO6jy";
	private static final String LINE_END = "\r\n";
	private static final String TWO_HYPHENS = "--";
	
	private String url = null;
	private int responseCode;
	private String response;
	private HttpURLConnection conn = null;
	private DataOutputStream dataOS = null;
	
	public HttpClient(String url, String method) {
		new HttpClient(url, method, null, null);
	}
	
	public HttpClient(String url, String method, String username, String password) {
		this.url = url;
    	// Make a connect to the server
		try {
	        URL _url = new URL(url);
	        System.setProperty("http.keepAlive", "false");
	        conn = (HttpURLConnection) _url.openConnection();
	        if(username != null && password != null) {
	        	auth(username, password);
	        }
	        
	        conn.setDoOutput(true);  
            conn.setDoInput(true);  
            conn.setUseCaches(false);
            conn.setConnectTimeout(30000);
            conn.setRequestMethod(method);
            if(POST.equals(method)) {
	            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            }
            conn.setRequestProperty("Connection", "Keep-Alive");
            dataOS = new DataOutputStream(conn.getOutputStream());
            
		} catch(MalformedURLException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 *  Put the authentication details in the request      
	 */
	public void auth(final String username, final String password) {
        String usernamePassword = username + ":" + password;  
        String encodedUsernamePassword = Base64.encodeBytes(usernamePassword.getBytes());  
        conn.setRequestProperty ("Authorization", "Basic " + encodedUsernamePassword);  
	}
	
	/**
	 * @param name
	 * @param value
	 */
	public void addParameter(final String name, final String value) {	
		try {
			// This is the standard format for a multipart request
			StringBuffer param = new StringBuffer();  
			param.append(TWO_HYPHENS).append(BOUNDARY).append(LINE_END);  
	        // These strings are sent in the request body. They provide information about the file being uploaded
			param.append("Content-Disposition: form-data; name=\"" + name + "\"").append(LINE_END);
	        param.append(LINE_END);
	        param.append(value);  
	        param.append(LINE_END);
	        dataOS.writeBytes(param.toString());
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addParameter(final String name, final String filename, final byte[] value) {	
		try {
			StringBuffer param = new StringBuffer();  
	        param.append(TWO_HYPHENS).append(BOUNDARY).append(LINE_END);  
	        // These strings are sent in the request body. They provide information about the file being uploaded
	        param.append("Content-Disposition: form-data; name=\"" +name + "\"; filename=\"" +filename + "\"").append(LINE_END);  
	        // param.append("Content-Type: image/jpeg").append(LINE_END);
	        param.append("Content-Length: ").append(value.length).append(LINE_END);
	        param.append(LINE_END);
	        
	        dataOS.writeBytes(param.toString());
	        
	        dataOS.write(value);
	
	        dataOS.writeBytes(LINE_END);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public String get() {
		try {
			dataOS.flush();  
	        dataOS.close();
	        // Ensure we got the HTTP 200 response code  
	        this.responseCode = conn.getResponseCode();  
//	        if (this.responseCode != 200) {  
//	            return (String.format("Received the response code %d from the URL %s", responseCode, url));  
//	        }
	        
	        // Read the response  
	        InputStream is = conn.getInputStream();  
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	        byte[] bytes = new byte[1024];  
	        int bytesRead;  
	        while((bytesRead = is.read(bytes)) != -1) {  
	            baos.write(bytes, 0, bytesRead);  
	        }  
	        byte[] bytesReceived = baos.toByteArray();  
	        baos.close();  
	  
	        is.close();  
	        return new String(bytesReceived);  
	        
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {  
	            conn.disconnect();  
	        }  
		}
		return "";
	}
	
	public int post() throws IOException {
		
		try {
			String end = TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_END;
			dataOS.writeBytes(end);
	        dataOS.flush();  
	        dataOS.close();  
	        
	        // Read the response  
	        InputStream is = null;
	        try {
	        	is = conn.getInputStream();
	        } catch(IOException e) {
	        	// retry
	        	conn.getInputStream();
	        }
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	        byte[] bytes = new byte[1024];  
	        int bytesRead;  
	        while((bytesRead = is.read(bytes)) != -1) {  
	            baos.write(bytes, 0, bytesRead);  
	        }  
	        byte[] bytesReceived = baos.toByteArray();  
	        baos.close();  
	  
	        is.close();  
	        response = new String(bytesReceived); 
	        
	        return conn.getResponseCode();
		} catch(IOException e) {
			throw e;
		} finally {  
	        if (conn != null) {  
	            conn.disconnect();  
	        }  
	    }
	}

	public String getResponse() {
		return response;
	}
	
	public int getResponseCode() {
		return responseCode;
	}
}
