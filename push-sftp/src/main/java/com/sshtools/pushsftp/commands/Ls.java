package com.sshtools.pushsftp.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ls", mixinStandardHelpOptions = true, description = "List directory")
public class Ls extends SftpCommand {

	@Option(names = "-l", description = "show files with the long name format")
	boolean longnames;
	
	@Option(names = "-a", description = "show hidden files")
	boolean showHidden;
	
	@Override
	public Integer call() throws Exception {
		
		if(longnames) {
			printLongnames();
		} else {
			printNames();
		}

		return 0;
	}

	private void printNames() throws SftpStatusException, SshException {

		SftpClient sftp = getSftpClient();
		
		Set<String> results = new TreeSet<>();
		int maximumFilenameLength = 0;
		int columns = 120;
		try {
			columns = Integer.parseInt(System.getenv("COLUMNS"));
		} catch(NumberFormatException e) {} 
 		Iterator<com.sshtools.client.sftp.SftpFile> it = sftp.lsIterator();
		while(it.hasNext()) {
			SftpFile file = it.next();
			String displayName = longnames ? file.getLongname() : file.getFilename();
			if(Utils.isBlank(displayName) || (displayName.startsWith(".") && !showHidden)) {
				continue;
			}
			maximumFilenameLength = Math.max(displayName.length(), maximumFilenameLength);
			results.add(displayName);
		}
		
		int printingColumns = 1;
		if(maximumFilenameLength < (columns / 2)) {
			printingColumns = columns / maximumFilenameLength;
		}
		
		if(printingColumns > 1) {
			String format = "%1$-"+ (columns / printingColumns) + "s";
			Iterator<String> itr = results.iterator();
			while(itr.hasNext()) {
				for(int i=0;i<printingColumns;i++) {
					System.out.print(String.format(format, itr.next()));
					if(!itr.hasNext()) {
						break;
					}
				}
				System.out.println();
			}
		} else {
			for(String result : results) {
				System.out.println(result);
			}
		}
	}

	private void printLongnames() throws SftpStatusException, SshException {
		
		SftpClient sftp = getSftpClient();
		
		List<SftpFile> results = new ArrayList<>();
		
		Iterator<com.sshtools.client.sftp.SftpFile> it = sftp.lsIterator();
		while(it.hasNext()) {
			SftpFile file = it.next();
			if(file.getFilename().startsWith(".") && !showHidden) {
				continue;
			}
			results.add(file);
		}
		
		Collections.sort(results, new Comparator<SftpFile>() {

			@Override
			public int compare(SftpFile o1, SftpFile o2) {
				return o1.getFilename().compareTo(o2.getFilename());
			}
			
		});
		
		for(SftpFile result : results) {
			System.out.println(result.getLongname());
		}
	}

}
