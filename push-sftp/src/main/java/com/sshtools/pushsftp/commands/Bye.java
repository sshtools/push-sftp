package com.sshtools.pushsftp.commands;

import org.jline.reader.EndOfFileException;

import picocli.CommandLine.Command;

@Command(name = "bye", usageHelpAutoWidth = true, aliases = { "quit", "exit" }, mixinStandardHelpOptions = false, description = "Quit interactive command")
public class Bye extends SftpCommand {

	@Override
	protected Integer onCall() throws Exception {
		throw new EndOfFileException();
	}
	
}
