package com.sshtools.pushsftp.commands;

import static com.sshtools.common.util.IOUtils.toByteSize;

import java.util.Optional;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "df", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "Display current remote directory")
public class Df extends SftpCommand {
	
	@Option(names = "-H", description = "human readable sizes")
	private boolean humanReadable;
	
	@Option(names = "-i", description = "show inodes instead of space.")
	private boolean inodes;

	@Parameters(index = "0", arity="0..1", paramLabel="PATH", description = "path of directory to list")
	private Optional<String> path;
	
	@Override
	protected Integer onCall() throws Exception {
		if(inodes) {
			getTerminal().messageln("     Inodes        Used       Avail      (root)    %Capacity");
		}
		else {
			getTerminal().messageln("        Size         Used        Avail       (root)    %Capacity");
		}
		var stat = getSftpClient().statVFS(expandRemoteSingle(path));
		if(inodes) {
			getTerminal().messageln(String.format("%11d %11d %11d %11d %10d%%", 
						stat.getINodes(), stat.getINodes() - stat.getFreeINodes(), 
						stat.getAvailINodes(), 0, 0));
		}
		else {
			if(humanReadable) {
				getTerminal().messageln(String.format("%12s %12s %12s %12s %11d%%", 
							toByteSize(stat.getSize()), 
							toByteSize(stat.getUsed()), 
							toByteSize(stat.getAvailForNonRoot()),
							toByteSize(stat.getAvail()),    
							stat.getCapacity()));
			}
			else { 
				getTerminal().messageln(String.format("%12d %12d %12d %12d %11d%%", 
							stat.getSize(), 
							stat.getUsed(), 
							stat.getAvailForNonRoot(),
							stat.getAvail(),    
							stat.getCapacity()));
			}
		}
		return 0;
	}
}
