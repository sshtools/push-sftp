package com.sshtools.pushsftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jline.builtins.Completers.FileNameCompleter;
import org.jline.builtins.Styles;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.StyleResolver;

import com.sshtools.client.SshClient;
import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpClient.SftpClientBuilder;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.client.tasks.PushTask.PushTaskBuilder;
import com.sshtools.commands.ChildUpdateCommand;
import com.sshtools.commands.CliCommand;
import com.sshtools.commands.ExceptionHandler;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.jaul.AppCategory;
import com.sshtools.jaul.ArtifactVersion;
import com.sshtools.jaul.JaulApp;
import com.sshtools.pushsftp.commands.Bye;
import com.sshtools.pushsftp.commands.Cd;
import com.sshtools.pushsftp.commands.Chgrp;
import com.sshtools.pushsftp.commands.Chmod;
import com.sshtools.pushsftp.commands.Chown;
import com.sshtools.pushsftp.commands.Df;
import com.sshtools.pushsftp.commands.Get;
import com.sshtools.pushsftp.commands.Help;
import com.sshtools.pushsftp.commands.Info;
import com.sshtools.pushsftp.commands.Lcd;
import com.sshtools.pushsftp.commands.Lls;
import com.sshtools.pushsftp.commands.Lmkdir;
import com.sshtools.pushsftp.commands.Ln;
import com.sshtools.pushsftp.commands.Lpwd;
import com.sshtools.pushsftp.commands.Ls;
import com.sshtools.pushsftp.commands.Mkdir;
import com.sshtools.pushsftp.commands.Pull;
import com.sshtools.pushsftp.commands.Push;
import com.sshtools.pushsftp.commands.Put;
import com.sshtools.pushsftp.commands.Pwd;
import com.sshtools.pushsftp.commands.Rename;
import com.sshtools.pushsftp.commands.Rm;
import com.sshtools.pushsftp.commands.Rmdir;
import com.sshtools.pushsftp.commands.SftpCommand;
import com.sshtools.pushsftp.commands.Symlink;
import com.sshtools.pushsftp.commands.Umask;
import com.sshtools.sequins.Progress.Level;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.shell.jline3.PicocliCommands;

@Command(name = "push-sftp-interactive", description = "Push secure file transfer", subcommands = { Ls.class, Cd.class, Lcd.class, Pwd.class, Lls.class, 
		Lpwd.class, Help.class, Rm.class, Rmdir.class, Df.class,
		Mkdir.class, Rename.class, Lmkdir.class, Ln.class, Symlink.class, Umask.class, Bye.class, Chgrp.class, 
		Chown.class, Chmod.class, Push.class, Pull.class, Put.class, Get.class,
		ChildUpdateCommand.class, Info.class
		}, versionProvider = PSFTPInteractive.Version.class)
@JaulApp(id = "com.sshtools.PushSFTP", category = AppCategory.CLI, updaterId = "47", updatesUrl = "https://sshtools-public.s3.eu-west-1.amazonaws.com/push-sftp/${phase}/updates.xml")
public class PSFTPInteractive extends CliCommand {
	
	public final static class Version implements IVersionProvider {

		@Override
		public String[] getVersion() throws Exception {
			return new String[] { ArtifactVersion.getVersion("push-sftp", "com.sshtools", "push-sftp") };
		}
		
	}

	SftpClient sftp;

	@Option(names = { "-h", "--host" }, paramLabel = "HOSTNAME", description = "the hostname of the SSH server. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> host;

	@Option(names = { "--help" }, usageHelp = true)
    boolean help;

	@Option(names = { "-v", "--version" }, versionHelp = true)
    boolean version;

	@Option(names = { "--prompt" }, description = "promt for the hostname and username if not supplied.")
    boolean promptHostAndUser;

	@Option(names = { "-p", "--port" }, paramLabel = "PORT", description = "the port of the SSH server")
    int port = 22;

	@Option(names = { "-u", "--user" }, paramLabel = "USER", description = "the username to authenticate with. Either this must be supplied or the username can be supplied in the destination")
    Optional<String> username;
	
	@Option(names = { "-b", "--batch" }, paramLabel = "SCRIPT", description = "run a batch script")
	Optional<File> batchFile;

	@Option(names = { "-d", "--local-dir" }, paramLabel = "PATH", description = "The local directory to start in")
    Optional<Path> localDirectory;
	
	@Option(names = { "-r", "--remote-dir" }, paramLabel = "PATH", description = "The remote directory to start in")
	Optional<String> remoteDirectory ;
	
	@Parameters(index = "0", arity = "0..1", description = "The remote server, with optional username.")
	private Optional<String> destination;
	
	@Parameters(index = "1", arity = "0..1", description = "A number of files to pass directly to a 'push' command.")
	private Path[] files;
	
	private Optional<String> cachedHostname = Optional.empty();
	private Optional<String> cachedUsername = Optional.empty();
	private Optional<Integer> cachedPort = Optional.empty();

	public PSFTPInteractive() {
		super(Optional.empty());
	}

	public SftpClient getSftpClient() {
		return sftp;
	}

	@Override
	protected void onConnected(SshClient ssh) {
		try {
			var bitmapBldr = io().createBitmap();
			var img = bitmapBldr.build("logo.png", PSFTPInteractive.class);
			img.draw();
			
			sftp = SftpClientBuilder.create().withClient(ssh).build();
			
			sftp.lcd(getLcwd().toAbsolutePath().toString());
			if(remoteDirectory.isPresent()) {
				sftp.cd(remoteDirectory.get());
			}
		} catch (SshException | PermissionDeniedException | IOException | SftpStatusException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public int getPort() {
		return cachedPort.orElse(port);
	}
	
	@Override
	protected void configureSystemRegistry(SystemRegistryImpl systemRegistry, PicocliCommands commands, Map<String, CommandLine> subs) {
		
		var remote = new RemoteFileNameCompleter();
		var remoteDirs = new RemoteFileNameCompleter() {

			@Override
			protected boolean accept(SftpFile path) {
				return super.accept(path) && path.attributes().isDirectory();
			}
			
		};
		var local = new FileNameCompleter() {
			@Override
			protected Path getUserDir() {
				return getLcwd();
			}

			@Override
	        protected String getSeparator(boolean useForwardSlash) {
	            return useForwardSlash ? "/" : ( isWindowsParsing() ? "\\" : "/" );
	        }
		};
		var localDirs = new FileNameCompleter() {
			@Override
			protected Path getUserDir() {
				return getLcwd();
			}

			@Override
			protected boolean accept(Path path) {
				return super.accept(path) && Files.isDirectory(path);
			}

			@Override
	        protected String getSeparator(boolean useForwardSlash) {
	            return useForwardSlash ? "/" : ( isWindowsParsing() ? "\\" : "/" );
	        }
		};
		
		systemRegistry.addCompleter(new Completer() {
			
			@Override
			public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
				var cmd = line.words().get(0);
				var cli  = subs.get(cmd);
				if(cli != null && cli.getCommand() instanceof SftpCommand) {
					SftpCommand sub = cli.getCommand();
					var mode = sub.completionMode();
					switch(mode) {
					case LOCAL:
					case LOCAL_THEN_REMOTE: /* TODO remote when below is supported */
						local.complete(reader, line, candidates);
						break;
					case DIRECTORIES_LOCAL:
					case DIRECTORIES_LOCAL_THEN_REMOTE: /* TODO remote when below is supported */
						localDirs.complete(reader, line, candidates);
						break;
					case REMOTE:
					case REMOTE_THEN_LOCAL:  /* TODO remote when below is supported */
						remote.complete(reader, line, candidates);
						break;
					case DIRECTORIES_REMOTE:
					case DIRECTORIES_REMOTE_THEN_LOCAL:  /* TODO remote when below is supported */
						remoteDirs.complete(reader, line, candidates);
						break;
//					case LOCAL_THEN_REMOTE:
						// TODO base on index
//						local.complete(reader, line, candidates);
//						remote.complete(reader, line, candidates);
//						break;
//					case REMOTE_THEN_LOCAL:
						// TODO base on index
//						remote.complete(reader, line, candidates);
//						local.complete(reader, line, candidates);
//						break;
					default:
						break;
					}
				}
			}
		});
	}

	protected boolean startCLI() throws IOException, InterruptedException, SftpStatusException, SshException {

		if(files != null && files.length > 0) {
			try (var progress = io().progressBuilder().withRateLimit().build()) {
				getSshClient().runTask(PushTaskBuilder.create().
					withClients((idx) -> {
						if (idx == 0)
							return ssh;
						else {
							try {
								return connect(false, idx < 2);
							} catch (IOException e) {
								throw new UncheckedIOException(e);
							} catch (SshException e) {
								throw new IllegalStateException(e);
							}
						}
					}).
					withPrimarySftpClient(getSftpClient()).
					withPaths(files).
					withRemoteFolder(Path.of(getSftpClient().pwd())).
					withProgressMessages((fmt, args) -> progress.message(Level.NORMAL, fmt, args)).
					withProgress(SftpCommand.fileTransferProgress(io(), progress, "Uploading {0}")).build());
			}
			return false;
		}
		
		if(localDirectory.isPresent()) {
			if(!Files.exists(localDirectory.get())) {
				io().error("{0} not found!", localDirectory.get());
				return false;
			}
		}
		if(batchFile.isEmpty()) {
			return true;
		}
		File script = batchFile.get();
		if(script.exists()) {
			io().message("Executing batch script {0}", script.getName());
			try(InputStream in = new FileInputStream(script)) {
				source(in);
			}
		} else {
			io().error("{0} not found!", script);
		}
		return false;
	}

	@Override
	public String getHost() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.lastIndexOf('@');
			return idx == -1 ? dst : dst.substring(idx  +1);
		}
		return host.orElseGet(() -> {
			if(promptHostAndUser) {
				if(cachedHostname.isEmpty()) {
					var h = io().prompt("Hostname (Enter for {0}):", "localhost");
					if(h == null) {
						throw new IllegalArgumentException("Host must be supplied either as an option or as part of the destination.");					
					}
					else {
						if(h.equals(""))
							h = "localhost";
						var idx = h.indexOf(':');
						if(idx == -1) {
							cachedHostname = Optional.of(h);
							cachedPort = Optional.empty();
						}
						else {
							cachedHostname = Optional.of(h.substring(0, idx));
							cachedPort = Optional.of(Integer.parseInt(h.substring(idx  +1)));
						}
						return cachedHostname.get();
					}
				}
				else 
					return cachedHostname.get();
			}
			else
				throw new IllegalArgumentException("Host must be supplied either as an option or as part of the destination.");
		});
	}

	@Override
	public String getUsername() {
		if(destination.isPresent()) {
			var dst = destination.get();
			var idx = dst.lastIndexOf('@');
			if(idx > -1)
				return dst.substring(0, idx);
		}
		return username.orElseGet(() -> {
			if(promptHostAndUser) {
				if(cachedUsername.isEmpty()) {
					var u = io().prompt("Username (Enter for {0}):", System.getProperty("user.name"));
					if(u == null) {
						throw new IllegalArgumentException("Username must be supplied either as an option or as part of the destination.");					
					}
					else {
						if(u.equals(""))
							u = System.getProperty("user.name");
						cachedUsername = Optional.of(u);
						return cachedUsername.get();
					}
				}
				else 
					return cachedUsername.get();
			}
			else 
				throw new IllegalArgumentException("Username must be supplied either as an option or as part of the destination.");
		});
	}

	@Override
	protected void beforeCommand() { 
		if(getHost().equals(""))
			throw new IllegalArgumentException("Host name must be supplied.");	
	}

	@Override
	protected boolean canExit() {
		return false;
	}

	@Override
	protected void error(String msg, Exception e) {
		io().error(msg, e);
	}

	@Override
	protected boolean isQuiet() {
		return true;
	}

	@Override
	protected String getCommandName() {
		return "push-sftp";
	}
	
	public static void main(String[] args) throws Exception {
		var cmd = new PSFTPInteractive();
		System.exit(new CommandLine(cmd).setExecutionExceptionHandler(new ExceptionHandler(cmd)).execute(args));
	}

	public SshClient getSshClient() {
		return ssh;
	}

	@Override
	protected Object createInteractiveCommand() {
		return new PSFTPCommands(this);
	}

	@Override
	public Path getLcwd() {
		return localDirectory.orElse(super.getLcwd());
	}
	
	public void setLocalDirectory(Path path) {
		localDirectory = Optional.of(localDirectory.isPresent() ? localDirectory.get().resolve(path) : path);
	}
	


    /**
     * A file name completer takes the buffer and issues a list of
     * potential completions.
     * <p>
     * This completer tries to behave as similar as possible to
     * <i>bash</i>'s file name completion (using GNU readline)
     * with the following exceptions:
     * <ul>
     * <li>Candidates that are directories will end with "/"</li>
     * <li>Wildcard regular expressions are not evaluated or replaced</li>
     * <li>The "~" character can be used to represent the user's home,
     * but it cannot complete to other users' homes, since java does
     * not provide any way of determining that easily</li>
     * </ul>
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
     * @since 2.3
     */
    class RemoteFileNameCompleter implements org.jline.reader.Completer {

        public void complete(LineReader reader, ParsedLine commandLine, final List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;

            String buffer = commandLine.word().substring(0, commandLine.wordCursor());

            String current;
            String curBuf;
            String sep = getSeparator(reader.isSet(LineReader.Option.USE_FORWARD_SLASH));
            int lastSep = buffer.lastIndexOf(sep);
            try {
                if (lastSep >= 0) {
                    curBuf = buffer.substring(0, lastSep + 1);
                    if (curBuf.startsWith("~")) {
                    	var userHome = getUserHome();
                        if (curBuf.startsWith("~" + sep)) {
                            current = userHome + curBuf.substring(2);
                        } else {
                        	var parentIdx = userHome.lastIndexOf('/');
                        	if(parentIdx > 0) {
                                current = userHome.substring(0, parentIdx + 1) + curBuf.substring(1);	
                        	}
                        	else
                                current = userHome + curBuf.substring(1);
                        }
                    } else {
                        current = getUserDir() + "/" + curBuf;
                    }
                } else {
                    curBuf = "";
                    current = getUserDir();
                }
                StyleResolver resolver = Styles.lsStyle();
                for(var it = sftp.lsIterator(current); it.hasNext(); ) {
                	var p = it.next();
                	if(!accept(p))
                		continue;
                	String value = curBuf + p.getFilename();
                    if (p.attributes().isDirectory()) {
                        candidates.add(new Candidate(
                                value + (reader.isSet(LineReader.Option.AUTO_PARAM_SLASH) ? sep : ""),
                                getDisplay(io().terminal(), p, resolver, sep),
                                null,
                                null,
                                reader.isSet(LineReader.Option.AUTO_REMOVE_SLASH) ? sep : null,
                                null,
                                false));
                    } else {
                        candidates.add(new Candidate(
                                value,
                                getDisplay(io().terminal(), p, resolver, sep),
                                null,
                                null,
                                null,
                                null,
                                true));
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        protected boolean accept(SftpFile path) {
            return !path.getFilename().startsWith(".");
        }

        protected String getUserDir() throws SftpStatusException, SshException {
            return sftp.pwd();
        }

        protected String getUserHome() throws SftpStatusException, SshException {
            return sftp.getDefaultDirectory();
        }

        protected String getSeparator(boolean useForwardSlash) {
            return useForwardSlash ? "/" : ( isWindowsParsing() ? "\\" : "/" );
        }

        protected String getDisplay(Terminal terminal, SftpFile p, StyleResolver resolver, String separator) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            String name = p.getFilename();
            int idx = name.lastIndexOf(".");
            String type = idx != -1 ? ".*" + name.substring(idx) : null;
            if (p.attributes().isLink()) {
                sb.styled(resolver.resolve(".ln"), name).append("@");
            } else if (p.attributes().isDirectory()) {
                sb.styled(resolver.resolve(".di"), name).append(separator);
            } else if (type != null && resolver.resolve(type).getStyle() != 0) {
                sb.styled(resolver.resolve(type), name);
            } else if (p.attributes().isFile()) {
                sb.styled(resolver.resolve(".fi"), name);
            } else {
                sb.append(name);
            }
            return sb.toAnsi(terminal);
        }
    }
}
