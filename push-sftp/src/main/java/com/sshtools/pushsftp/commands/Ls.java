package com.sshtools.pushsftp.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "ls", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "List directory")
public class Ls extends SftpCommand {

	@Option(names = "-l", description = "show files with the long name format")
	boolean longnames;
	
	@Option(names = "-a", description = "show hidden files")
	boolean showHidden;
	
	@Override
	protected Integer onCall() throws Exception {
		
		if(longnames) {
			printLongnames();
		} else {
			printNames();
		}

		return 0;
	}

	private void printNames() throws SftpStatusException, SshException {

		var sftp = getSftpClient();
		
		var results = new TreeSet<String>();
		int maximumFilenameLength = 0;
		int columns = getRootCommand().getTerminal().getWidth();
		
 		var it = sftp.lsIterator();
		while(it.hasNext()) {
			var file = it.next();
			var displayName = longnames ? file.getLongname() : file.getFilename();
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
			var format = "%1$-"+ (columns / printingColumns) + "s";
			var itr = results.iterator();
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
			for(var result : results) {
				System.out.println(result);
			}
		}
	}

	private void printLongnames() throws SftpStatusException, SshException {
		
		var sftp = getSftpClient();
		var results = new ArrayList<SftpFile>();
		
		var it = sftp.lsIterator();
		while(it.hasNext()) {
			var file = it.next();
			if(file.getFilename().startsWith(".") && !showHidden) {
				continue;
			}
			results.add(file);
		}
		
		Collections.sort(results, (o1, o2) -> o1.getFilename().compareTo(o2.getFilename()));
		
		for(var result : results) {
			System.out.println(result.getLongname());
		}
	}

}
