module com.sshtools.pushsftp {
	requires java.prefs;
	requires java.net.http;
	requires com.sshtools.sequins;
	requires info.picocli;
	requires com.sshtools.common.logger;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.commands;
	requires com.install4j.runtime;
	requires org.jline.reader;
	requires org.jline.console;
	requires org.jline.builtins;
	requires picocli.shell.jline3;
	requires org.jline.terminal;
	requires org.jline.style;
	opens com.sshtools.pushsftp;
	opens com.sshtools.pushsftp.commands;
	exports com.sshtools.pushsftp;
}