package com.sshtools.pushsftp.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.TreeSet;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.util.Utils;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ls", usageHelpAutoWidth = true, mixinStandardHelpOptions = true, description = "List directory")
public class Ls extends SftpCommand {

	@Option(names = "-l", description = "show files with the long name format")
	boolean longnames;
	
	@Option(names = "-a", description = "show hidden files")
	boolean showHidden;

	@Parameters(index = "0", arity="0..1", paramLabel="PATH", description = "path of directory to list")
	Optional<String> path;
	
	@Override
	protected Integer onCall() throws Exception {
		
		if(longnames) {
			printLongnames();
		} else {
			printNames();
		}

		return 0;
	}

	private void printNames() throws SftpStatusException, SshException, IOException, PermissionDeniedException {

		
		var results = new TreeSet<String>();
		int maximumFilenameLength = 0;
		int columns = getRootCommand().getTerminal().getWidth();

		var it = lsIterator();
		while(it.hasNext()) {
			var file = it.next();
			var displayName = longnames ? file.getLongname() : file.getFilename();
			if(Utils.isBlank(displayName) || (displayName.startsWith(".") && !showHidden)) {
				continue;
			}
			maximumFilenameLength = Math.max(displayName.length(), maximumFilenameLength);
			results.add(displayName);
		}
		
		int printingColumns = 1;
		if(maximumFilenameLength < (columns / 2) && maximumFilenameLength > 0) {
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
			for(var result : results) {
				System.out.println(result);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Iterator<SftpFile> lsIterator() throws SftpStatusException, SshException, IOException, PermissionDeniedException {
		var sftp = getSftpClient();
		if(path.isPresent()) {
			var expanded = expandRemoteArray(path.get());
			if(expanded.length == 1)
				return lsPath(expanded[0]);
			else {
				var l = new ArrayList<Iterator<SftpFile>>();
				for(var path : expanded) {
					l.add(lsPath(path));
				}
				return new CompoundIterator<SftpFile>(l.toArray(new Iterator[0]));
			}
		}
		else
			return sftp.lsIterator();
	}

	private Iterator<SftpFile> lsPath(String path) throws SftpStatusException, SshException {
		var resolved = getSftpClient().getSubsystemChannel().getFile(path);
		if(resolved.attributes().isDirectory())
			return  getSftpClient().lsIterator(path);
		else
			return Arrays.asList(resolved).iterator();
	}

	private void printLongnames() throws SftpStatusException, SshException, IOException, PermissionDeniedException {

		var it = lsIterator();
		var results = new ArrayList<SftpFile>();
		while(it.hasNext()) {
			var file = it.next();
			if(file.getFilename().startsWith(".") && !showHidden) {
				continue;
			}
			results.add(file);
		}
		
		Collections.sort(results, (o1, o2) -> o1.getFilename().compareTo(o2.getFilename()));
		
		for(var result : results) {
			System.out.println(SftpClient.formatLongname(result));
		}
	}
	
	class CompoundIterator<T> implements Iterator<T> {

	    private final LinkedList<Iterator<T>> iteratorQueue;
	    private Iterator<T> current;

	    @SafeVarargs
	    public CompoundIterator(final Iterator<T>... iterators) {
	        this.iteratorQueue = new LinkedList<Iterator<T>>();
	        for (var iterator : iterators) {
	            iteratorQueue.push(iterator);
	        }
	        current = Collections.<T>emptyList().iterator();
	    }

	    public boolean hasNext() {
	       var curHasNext = current.hasNext();
	        if (!curHasNext && !iteratorQueue.isEmpty()) {
	            current = iteratorQueue.pop();
	            return current.hasNext();
	        } else {
	            return curHasNext;
	        }
	    }

	    public T next() {
	        if (current.hasNext()) {
	            return current.next();
	        }
	        if (!iteratorQueue.isEmpty()) {
	            current = iteratorQueue.pop();
	        }
	        return current.next();
	    }

	    public void remove() {
	        throw new UnsupportedOperationException("remove() unsupported");
	    }
	}

}
