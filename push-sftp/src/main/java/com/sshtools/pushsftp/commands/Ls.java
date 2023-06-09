package com.sshtools.pushsftp.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.TreeSet;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ls", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "List directory")
public class Ls extends SftpCommand {

	@Option(names = "-l", description = "show files with the long name format")
	boolean longnames;
	
	@Option(names = "-a", description = "show hidden files")
	boolean showHidden;

	@Parameters(index = "0", arity="0..1", paramLabel="PATH", description = "path of directory to list")
	Optional<String> path;
	
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

		
		var results = new TreeSet<String>();
		int maximumFilenameLength = 0;
		int columns = getRootCommand().getTerminal().getWidth();

		var it = lsIterator();
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
		if(maximumFilenameLength < (columns / 2) && maximumFilenameLength > 0) {
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

	private Iterator<SftpFile> lsIterator() throws SftpStatusException, SshException {
		var sftp = getSftpClient();
 		var it = path.isPresent() ? sftp.lsIterator(path.get()) : sftp.lsIterator();
		return it;
	}

	private void printLongnames() throws SftpStatusException, SshException {

		var it = lsIterator();
		var results = new ArrayList<SftpFile>();
		while(it.hasNext()) {
			var file = it.next();
			if(file.getFilename().startsWith(".") && !showHidden) {
				continue;
			}
			results.add(file);
		}
		
		Collections.sort(results, (o1, o2) -> o1.getFilename().compareTo(o2.getFilename()));
		
		for(var result : results) {
			System.out.println(SftpClient.formatLongname(result));
		}
	}

}
