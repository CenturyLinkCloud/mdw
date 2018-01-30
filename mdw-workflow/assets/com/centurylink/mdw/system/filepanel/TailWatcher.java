package com.centurylink.mdw.system.filepanel;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.io.UncheckedIOException;
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

    public TailWatcher(Path file, int lastLine) {
        this.file = file;
        this.lastLine = lastLine;
        this.refCount = 1;
    }

    int refCount;
    boolean done;

    public void watch() throws IOException {
        try {
            new Thread(() -> {
                try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                    file.getParent().register(watcher, ENTRY_MODIFY); // TODO: delete, modify?
                    while (!done) {
                        try {
                            WatchKey key = watcher.take();
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (((Path)event.context()).getFileName().equals(file.getFileName())) {
                                    FileInfo fileInfo = new FileInfo(file.toFile());
                                    FileView fileView = new FileView(fileInfo, lastLine);
                                    WebSocketMessenger websocket = WebSocketMessenger.getInstance();
                                    if (websocket != null) {
                                        websocket.send(file.toAbsolutePath().toString(),
                                                fileView.getJson().toString());
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
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }).start();
        }
        catch (UncheckedIOException ex) {
            throw ex.getCause();
        }
    }
}
