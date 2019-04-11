package com.zendogames.paletteswapper;

import com.badlogic.gdx.Gdx;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

public class PaletteWatcher {
    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;

    private Thread watchThread;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public PaletteWatcher(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();

        register(dir);
    }

    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_MODIFY);
        keys.put(key, dir);
    }

    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        watchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (;;) {
                    // wait for key to be signalled
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException x) {
                        return;
                    }

                    Path dir = keys.get(key);
                    if (dir == null) {
                        System.err.println("WatchKey not recognized!!");
                        continue;
                    }

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind kind = event.kind();
                        if (kind == OVERFLOW) continue;

                        // Context for directory entry event is the file name of entry
                        WatchEvent<Path> ev = cast(event);
                        final Path name = ev.context();
                        final Path child = dir.resolve(name);

                        if (kind == ENTRY_MODIFY) {
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    if (name.getFileName().toFile().equals(Palette.textureFile))
                                        Palette.updateTexture();
                                }
                            });
                        }
                        // print out event
//						System.out.format("%s: %s\n", event.kind().name(), child);

                        // if directory is created, and watching recursively, then
                        // register it and its sub-directories
                    }

                    // reset key and remove from set if directory no longer accessible
                    boolean valid = key.reset();
                    if (!valid) {
                        keys.remove(key);
                        // all directories are inaccessible
                        if (keys.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        });
        watchThread.start();
    }

    public void stop() {
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }
}
