module com.sshtools.pushsftp.jfx {
	requires java.prefs;
	requires java.net.http;
	requires transitive com.sshtools.jajafx;
	requires org.kordamp.ikonli.fontawesome5;
	requires com.sshtools.sequins;
	requires info.picocli;
	requires org.kordamp.ikonli.javafx;
	requires com.sshtools.common.logger;
	requires com.install4j.runtime;
	requires transitive com.sshtools.synergy.client;
	requires transitive com.sshtools.agent;
	requires com.sshtools.simjac;
	requires com.sshtools.twoslices;
	requires java.keyring;
	opens com.sshtools.pushsftp.jfx;
	exports com.sshtools.pushsftp.jfx;
	requires org.freedesktop.dbus;
	requires static org.freedesktop.dbus.transport.jre;

//    requires org.scenicview.scenicview;
}