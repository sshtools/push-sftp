package com.sshtools.pushsftp;

import com.sshtools.commands.ChildUpdateCommand;
import com.sshtools.commands.InteractiveSshCommand;
import com.sshtools.commands.SshCommand;
import com.sshtools.pushsftp.commands.Bye;
import com.sshtools.pushsftp.commands.Cd;
import com.sshtools.pushsftp.commands.Chgrp;
import com.sshtools.pushsftp.commands.Chmod;
import com.sshtools.pushsftp.commands.Chown;
import com.sshtools.pushsftp.commands.Get;
import com.sshtools.pushsftp.commands.Help;
import com.sshtools.pushsftp.commands.Lcd;
import com.sshtools.pushsftp.commands.Lls;
import com.sshtools.pushsftp.commands.Lpwd;
import com.sshtools.pushsftp.commands.Ls;
import com.sshtools.pushsftp.commands.Mkdir;
import com.sshtools.pushsftp.commands.Push;
import com.sshtools.pushsftp.commands.Put;
import com.sshtools.pushsftp.commands.Pwd;
import com.sshtools.pushsftp.commands.Rm;
import com.sshtools.pushsftp.commands.Rmdir;
import com.sshtools.pushsftp.commands.Umask;

import picocli.CommandLine.Command;

@Command(name = "push-sftp", mixinStandardHelpOptions = false, 
			description = "Interactive shell", 
			subcommands = { Ls.class, Cd.class, Lcd.class, Pwd.class, Lls.class, 
					Lpwd.class, Help.class, Rm.class, Rmdir.class,
					Mkdir.class, Umask.class, Bye.class, Chgrp.class, 
					Chown.class, Chmod.class, Push.class, Put.class, Get.class,
					ChildUpdateCommand.class
					})
public class PSFTPCommands implements InteractiveSshCommand {

	private final SshCommand root;
	
	public PSFTPCommands(SshCommand root) {
		this.root = root;
	}

	@Override
	public SshCommand rootCommand() {
		return root;
	}
}
