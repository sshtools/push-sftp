package com.sshtools.pushsftp.commands;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "chown", usageHelpAutoWidth = true, mixinStandardHelpOptions = false, description = "Change owner of file path")
public class Chown extends SftpCommand  {

	@Option(names = { "-h" }, description = "Do not follow symlinks")
	private boolean dontFollowSymlinks;

	@Parameters(index = "0", description = "The identifiers of the new owner")
	private String uid;

	@Parameters(index = "1", arity = "1", description = "Path to change group of")
	private String path;
	
	public Chown() {
		super(FilenameCompletionMode.REMOTE);
	}
	
	@Override
	protected Integer onCall() throws Exception {
		
		var UID = new StringBuffer();
		var GID = new StringBuffer();
		
		if(uid.contains(":")) {
			String[] tmp = uid.split(":");
			UID.append(tmp[0].matches("\\d+") ? tmp[0] : getUID(tmp[0]));
			if(tmp.length > 1) {
				GID.append(tmp[1]);
			}
		} else {
			UID.append(uid);
		}
		
		expandRemoteAndDo((fp) -> {
		
			if(GID.length() > 0) {
				getSftpClient().chown(UID.toString(), GID.toString(), fp);
			} else {
				if(UID.length() > 0) {
					getSftpClient().chown(UID.toString(), fp);
				} else {
					getSftpClient().chown(uid, fp);
				}
			}
		}, false, path);
		return 0;
	}
}
