package com.sshtools.pushsftp.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.UnsignedInteger64;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "lls", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "List directory")
public class Lls extends SftpCommand {

	static final String SftpLongnameDateFormat = "MMM dd  yyyy";
	static final String SftpLongnameDateFormatWithTime = "MMM dd HH:mm";
	
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

	private void printNames() throws IOException, SftpStatusException, SshException {

		var sftp = getSftpClient();
		
		var results = new TreeSet<String>();
		var maximumFilenameLength = 0;
		var columns = getInteractiveCommand().getParentCommand().getTerminal().getWidth();
		
		try {
			var cwd = sftp.getCurrentWorkingDirectory();
			for(var file : cwd.getChildren()) {
				String displayName = file.getName();
				if(Utils.isBlank(displayName) || (displayName.startsWith(".") && !showHidden)) {
					continue;
				}
				maximumFilenameLength = Math.max(displayName.length() + 1, maximumFilenameLength);
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
				for(String result : results) {
					System.out.println(result);
				}
			}
		} catch ( PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
	}
	
	private String getLongname(AbstractFile file) throws FileNotFoundException, IOException, PermissionDeniedException {
		
		var attrs = file.getAttributes();	
		var str = new StringBuffer();
		str.append(Utils.pad(10 - attrs.getPermissionsString().length())
				+ attrs.getPermissionsString());
		if(attrs.isDirectory()) {
			str.append(" 1 ");
		} else {
			str.append(" 1 ");
		}
		if(attrs.hasUID()) {
			str.append(attrs.getUID() + Utils.pad(8 - attrs.getUID().length()));
		} else {
			str.append(String.valueOf(attrs.getUID()) + Utils.pad(8 - String.valueOf(attrs.getUID()).length()));
		}
		str.append(" ");
		if(attrs.hasGID()) {
			str.append(attrs.getGID()
					+ Utils.pad(8 - attrs.getGID().length()));
		} else {
			str.append(String.valueOf(attrs.getGID()) + Utils.pad(8 - String.valueOf(attrs.getGID()).length()));
		}
		str.append(" ");

		str.append(Utils.pad(11 - attrs.getSize().toString().length())
				+ attrs.getSize().toString());
		str.append(" ");
		
		var modTime = getModTimeStringInContext(attrs.getModifiedTime(), Locale.getDefault());
		str.append(Utils.pad(12 - modTime.length()) + modTime);
		str.append(" ");
		str.append(file.getName());

		return str.toString();
	}

	private String getModTimeStringInContext(UnsignedInteger64 mtime,
			Locale locale) {
		if (mtime == null) {
			return "";
		}

		SimpleDateFormat df;
		long mt = (mtime.longValue() * 1000L);
		long now = System.currentTimeMillis();

		if ((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
			df = new SimpleDateFormat(SftpLongnameDateFormat, locale);
		} else {
			df = new SimpleDateFormat(SftpLongnameDateFormatWithTime, locale);
		}

		return df.format(new Date(mt));
	}

	private void printLongnames() throws SftpStatusException, SshException, IOException {
		
		var sftp = getSftpClient();
		
		var results = new ArrayList<AbstractFile>();
		
		try {
			var cwd = sftp.getCurrentWorkingDirectory();
			for(var file : cwd.getChildren()) {
				var displayName = file.getName();
				if(Utils.isBlank(displayName) || (displayName.startsWith(".") && !showHidden)) {
					continue;
				}
				results.add(file);
			}
			
			Collections.sort(results, (o1, o2) -> o1.getName().compareTo(o2.getName()));
			
			for(var result : results) {
				try {
					System.out.println(getLongname(result));
				} catch (IOException | PermissionDeniedException e) {
				}
			}
		} catch(PermissionDeniedException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
