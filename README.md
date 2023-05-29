# Push SFTP
A dedicated SFTP client for increasing throughput performance on high latency WAN/Internet transfers.

This repository hosts both the command line interface, and the graphical interface called FileDrop. SSH and SFTP operations are supplied by the [Maverick Synergy Java SSH Library](https://jadaptive.com/en/products/open-source-java-ssh) which includes the "push" mechanism itself.

## Installers

End-user installers and documentation is available at [jadaptive.com](https://jadaptive.com/push-sftp).

## Development

The project uses Maven as a build system. You should be able to import the pom.xml into any modern Java IDE. Pull requests are welcomed and encouraged for new features and bug fixes. 

### Implementing commands

If you want to implement a new child command within the interactive command you first need to extend [SftpCommand](https://github.com/sshtools/push-sftp/blob/main/push-sftp/src/main/java/com/sshtools/pushsftp/commands/SftpCommand.java).

```java
import picocli.CommandLine.Command;

@Command(name = "hello", description = "Hello world example")
public class Hello extends SftpCommand {

	@Override
	protected Integer onCall() throws Exception {
		
		getTerminal().getWriter().println("Hello world!");
		return 0;
	}
}
```

Then add the command class to the [PSFTPCommands](https://github.com/sshtools/push-sftp/blob/main/push-sftp/src/main/java/com/sshtools/pushsftp/PSFTPCommands.java) @Command annotation, in the subcommands array.

```java
@Command(name = "push-sftp", mixinStandardHelpOptions = false, 
			description = "Interactive shell", 
			subcommands = { Ls.class, Cd.class, Lcd.class, Pwd.class, Lls.class, 
					Lpwd.class, Help.class, Rm.class, Rmdir.class,
					Mkdir.class, Umask.class, Bye.class, Chgrp.class, 
					Chown.class, Chmod.class, Push.class, Put.class, Get.class,
					ChildUpdateCommand.class, Info.class, Hello.class
					})
public class PSFTPCommands implements InteractiveSshCommand {
```

If you fail to do this the command will not appear in the help and will not be executable within the interactive command shell. 

To use the current SFTP connection [SftpCommand](https://github.com/sshtools/push-sftp/blob/main/push-sftp/src/main/java/com/sshtools/pushsftp/commands/SftpCommand.java) provides a method to return an [SftpClient](https://github.com/sshtools/maverick-synergy/blob/e5862bc41e9dcf8b4f28509ce47e6852c2dd768d/maverick-synergy-client/src/main/java/com/sshtools/client/sftp/SftpClient.java)

```java
var sftp = getSftpClient()
```
