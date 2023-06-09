package com.sshtools.pushsftp.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import com.sshtools.common.files.AbstractFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "lls", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "List directory")
public class Lls extends SftpCommand {

	static final String SftpLongnameDateFormat = "MMM dd  yyyy";
	static final String SftpLongnameDateFormatWithTime = "MMM dd HH:mm";
	
	@Option(names = "-l", description = "show files with the long name format")
	boolean longnames;
	
	@Option(names = "-a", description = "show hidden files")
	boolean showHidden;

	@Parameters(index = "0", arity="0..1", paramLabel="PATH", description = "path of directory to list")
	Optional<Path> path;

	@Override
	protected Integer onCall() throws Exception {
		
		if(longnames) {
			printLongnames();
		} else {
			printNames();
		}

		return 0;
	}

	private void printNames() throws IOException, SftpStatusException, SshException {

		var maximumFilenameLength = 0;
		var columns = getRootCommand().getTerminal().getWidth();
		
		try(var stream = Files.newDirectoryStream(expandLocalSingle(path))) {
			for(var p : stream) {
				String displayName = p.getFileName().toString();
				if(Utils.isBlank(displayName) || (displayName.startsWith(".") && !showHidden)) {
					continue;
				}
				maximumFilenameLength = Math.max(displayName.length() + 1, maximumFilenameLength);
			}
		}
		
		int printingColumns = 1;
		if(maximumFilenameLength < (columns / 2)) {
			printingColumns = columns / maximumFilenameLength;
		}
		
		if(printingColumns > 1) {
			var format = "%1$-"+ (columns / printingColumns) + "s";

			try(var stream = Files.newDirectoryStream(expandLocalSingle(path))) {
				var itr = stream.iterator();
				while(itr.hasNext()) {
					for(int i=0;i<printingColumns;i++) {
						System.out.print(String.format(format, itr.next().getFileName()));
						if(!itr.hasNext()) {
							break;
						}
					}
					System.out.println();
				}
			}
		} else {
			try(var stream = Files.newDirectoryStream(expandLocalSingle(path))) {
				for(var p : stream) {
					System.out.println(p.getFileName().toString());
				}
			}
		}
	}
	
	private String getLongname(AbstractFile file) throws FileNotFoundException, IOException, PermissionDeniedException {
		var attrs = file.getAttributes();
		return String.format("%9s %01d %-9s %-9s %10d %12s %s",
				attrs.toPermissionsString(),
				attrs.linkCount(),
				attrs.bestUsername(),
				attrs.bestGroup(),
				attrs.size().longValue(),
				getModTimeStringInContext(attrs.lastModifiedTime(), Locale.getDefault()),
				file.getName());
	}

	private String getModTimeStringInContext(FileTime mtime,
			Locale locale) {
		if (mtime == null) {
			return "";
		}

		SimpleDateFormat df;
		long mt = mtime.toMillis();
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
