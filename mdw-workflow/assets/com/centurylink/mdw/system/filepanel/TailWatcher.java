package com.centurylink.mdw.system.filepanel;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import com.centurylink.mdw.common.service.WebSocketMessenger;
import com.centurylink.mdw.model.system.FileInfo;

public class TailWatcher {

    private Path file;
    public Path getFile() { return file; }

    private int lastLine;
    public int getLastLine() { return lastLine; }

    private WatchService watcher;
    private Thread watcherThread;
    private long lastModified;

    public TailWatcher(Path file, int lastLine) {
        this.file = file;
        this.lastLine = lastLine;
    }

    int refCount;
    boolean done;

    public void watch() throws IOException {
        try {
            watcher = FileSystems.getDefault().newWatchService();
            watcherThread = new Thread(() -> {
                try {
                    file.getParent().register(watcher, ENTRY_MODIFY);
                    while (!done) {
                        try {
                            WatchKey key = watcher.take();
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (((Path)event.context()).getFileName().equals(file.getFileName())) {
                                    if (file.toFile().lastModified() != lastModified) { // avoid duplicate events
                                        send();
                                    }
                                }
                            }
                            key.reset();
                        }
                        catch (InterruptedException ex) {
                            done = true;
                        }
                    }
                }
                catch (ClosedByInterruptException ex) {
                    done = true;
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
            watcherThread.start();
        }
        catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }

    public void stop() throws IOException {
        done = true;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        if (watcher != null) {
            watcher.close();
        }
    }

    boolean send() throws IOException {
        File f = file.toFile();
        lastModified = f.lastModified();
        FileInfo fileInfo = new FileInfo(f);
        FileView fileView = new FileView(fileInfo, lastLine);
        WebSocketMessenger websocket = WebSocketMessenger.getInstance();
        boolean subscribers = false;
        if (websocket != null) {
            subscribers = websocket.send(file.toAbsolutePath().toString().replace('\\', '/'),
                    fileView.getJson().toString());
        }
        lastLine = fileInfo.getLineCount() - 1;
        return subscribers;
    }
}
