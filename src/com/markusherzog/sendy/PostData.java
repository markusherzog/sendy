package com.markusherzog.sendy;

import java.util.HashMap;
import java.util.Map;

public class PostData {

	private String title;
	private String body;
	private Map<String,BinaryData> binaryData;
	
	public void reset() {
		this.title = null;
		this.body = null;
		this.binaryData = null;
        }
	
	public boolean hasData() {
		if((this.title == null || this.title.trim().length() == 0)
				&& (this.body == null || this.body.trim().length() == 0)
				&& (this.binaryData == null || this.binaryData.size() == 0)
				) {
			return false;
		} else {
			return true;
		}
	}
	
	// XXX needs to handle checking of duplicate filenames.
	public void addBinaryData(final String filename, final byte[] bytes) {
		if(binaryData == null) {
			binaryData = new HashMap<String,BinaryData>();
		}
		if(filename != null && filename.trim().length() > 0 && bytes != null && bytes.length > 0) {
			binaryData.put(filename, new BinaryData(filename, bytes));
	}
		}
	
	public void removeBinaryData(final String filename) {
		binaryData.remove(filename);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
	
	class BinaryData {
		private String filename;
		private byte[] bytes;

		BinaryData(final String filename, final byte[] bytes) {
			this.filename = filename;
			this.bytes = bytes;
		}

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
	}

		public byte[] getBytes() {
			return bytes;
	}

		public void setBytes(byte[] bytes) {
			this.bytes = bytes;
		}
	}

	public Map<String, BinaryData> getBinaryData() {
		return binaryData;
	}
}
