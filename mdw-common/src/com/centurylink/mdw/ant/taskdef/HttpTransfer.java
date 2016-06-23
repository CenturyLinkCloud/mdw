/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.codec.binary.Base64;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class HttpTransfer extends Task {

	private final static String tempDirName = "httptransfer";

	private String srcURL = null;
	private boolean overwrite = true;
	private String destURL = null;
	private String destDirectory = null;
	private String destURLUser = null;
	private String destURLPWD = null;
	private String fileNames = null;

	public String getDestDirectory() {
		return destDirectory;
	}

	public void setDestDirectory(String destDirectory) {
		this.destDirectory = destDirectory;
	}

	public String getFileNames() {
		return fileNames;
	}

	public void setFileNames(String fileNames) {
		this.fileNames = fileNames;
	}

	private boolean failOnError=true;


	public String getSrcURL() {
		return srcURL;
	}

	public void setSrcURL(String srcURL) {
		this.srcURL = srcURL;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public String getDestURL() {
		return destURL;
	}

	public void setDestURL(String destURL) {
		this.destURL = destURL;
	}

	public String getDestURLUser() {
		return destURLUser;
	}

	public void setDestURLUser(String destURLUser) {
		this.destURLUser = destURLUser;
	}

	public String getDestURLPWD() {
		return destURLPWD;
	}

	public void setDestURLPWD(String destURLPWD) {
		this.destURLPWD = destURLPWD;
	}

	public boolean isFailOnError() {
		return failOnError;
	}

	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	@Override
	public void execute() throws BuildException {
		 if (srcURL == null || fileNames == null || destURL == null)
		      throw new BuildException("Null Source URL or NULL Dest URL or FileName ");
		log("Downloading file from: " + srcURL + " ...");
		log("Uploading file to: " + destURL + " ...");
		try {
			performFilesAction(fileNames);
		} catch (Exception e) {
			log(" Build exception "+e.getMessage());
			throw new BuildException(e.getMessage());
		}
		// clear temp directory
		clearTempFile();
	}

	protected void performFilesAction(String fileNames) throws MalformedURLException{
		String[] fileNameArray = fileNames.split(",");
		for (String fileName : fileNameArray) {
			performFileAction(fileName.trim());
		}
	}

	protected void performFileAction(String fileName) throws MalformedURLException{
		// Download from src directory
		download(getURLWithFile(srcURL,fileName),getTempFile(fileName));
		// 	upload to dest
		uploadFile(getURLWithFile(destURL,fileName),getTempFile(fileName),destURLUser,destURLPWD,overwrite);
	}


	protected void download(URL srcURL, File file)
	{
	    try
	    {
	      log("source url... " + srcURL.toString());
	      HttpURLConnection conn = (HttpURLConnection) srcURL.openConnection();

	      HttpURLConnection.setFollowRedirects(true);
	      FileOutputStream fos = new FileOutputStream(file);
	      log("Downloading ... " + file);

	      byte[] buffer = new byte[2048];
	      InputStream is = conn.getInputStream();
	      while (true)
	      {
	         int bytesRead = is.read(buffer);
	         if (bytesRead == -1)
	           break;
	          fos.write(buffer, 0, bytesRead);
	         }
	        fos.close();
	        is.close();
	        conn.disconnect();
	        log("Downloaded ... " + file);
	    }
	    catch (IOException e)
	    {
	      String msg = "Unable to connect to URL: " + srcURL + " - Unable to download file. ";
	      if (isFailOnError())
	        throw new BuildException(msg+e.getMessage());
	      else
	        log(msg);
	    }
	}

	public void uploadFile(URL destURL, File file,String userId, String password, boolean overwrite )
	  {
	    try
	    {
	      log("target url... " + destURL.toString());
	      long fileLastModified = file.lastModified();

	      HttpURLConnection conn = (HttpURLConnection) destURL.openConnection();
	      long urlLastModified = conn.getLastModified();
	      conn.disconnect();

	      if (!overwrite && (urlLastModified >= fileLastModified))
	      {
	        log("Destination file is up-to-date, not uploading.");
	        return;
	      }
	      else
	      {
	        conn = (HttpURLConnection) destURL.openConnection();
	        conn.setRequestProperty("Content-Type", "application/octet-stream");
	        conn.setRequestMethod("PUT");
	        if (userId != null) {
	          String value = userId + ":" + password;
	          conn.setRequestProperty("Authorization", "Basic " + new String(Base64.encodeBase64(value.getBytes())));
	        }

	        conn.setDoOutput(true);

	        OutputStream outStream = conn.getOutputStream();

	        log("Uploading... " + file);

	        InputStream inStream = new FileInputStream(file);

	        byte[] buf = new byte[1024];
	        int len = 0;
	        while (len != -1)
	        {
	          len = inStream.read(buf);
	          if (len > 0)
	            outStream.write(buf, 0, len);
	        }

	        inStream.close();
	        outStream.close();
	        conn.disconnect();

	        int code = conn.getResponseCode();
	        if (code < 200 || code >= 300)
	        {
	          String response = conn.getResponseMessage();
	          throw new BuildException("Error uploading file: " + code + " -- " + response);
	        }
	        log("  Uploaded: " + destURL);
	      }
	    }
	    catch (IOException e)
	    {
            if (isFailOnError())
                throw new BuildException(e.getMessage(), e);
            else
                log(e.getMessage());
	    }
	}

	protected URL getURLWithFile(String urlString,String fileName) throws MalformedURLException{
	     if (! urlString.endsWith("/")) {
	    	 urlString = urlString + "/";
	     }
	     return new URL(urlString+fileName);
	}

	protected File getTempFile(String fileName){
		 File tempDir = new File("./"+tempDirName);
		 if ( ! tempDir.exists()) {
			 tempDir.mkdir();
		 }
		 return new File("./"+tempDirName+"/"+fileName);
	}

	private void clearTempFile(){
		File dir = new File("./"+tempDirName);
		if (dir.isDirectory()) {
			File[] fileArray = dir.listFiles();
			for (File file : fileArray) {
				file.delete();
			}
			dir.delete();
		}
	}
}
