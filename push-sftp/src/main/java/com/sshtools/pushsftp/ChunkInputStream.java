package com.sshtools.pushsftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class ChunkInputStream extends InputStream {

	RandomAccessFile file;
	long length;
	
	ChunkInputStream(RandomAccessFile file, long length) {
		this.file = file;
		this.length = length;
	}
	
	@Override
	public int read() throws IOException {
		if(length > 0) {
			length--;
			return file.read();
		}
		return -1;
	}
	
	@Override
	public int read(byte[] b, int off, int len) throws IOException {	
		if(length > 0) {
			int max = (int) Math.min(len, length);
			length-=max;
			
			return file.read(b, off, max);
		}
		return -1;
	}

}
