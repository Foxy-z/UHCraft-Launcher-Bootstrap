package fr.uhcraft.launcher.bootstrap;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadThread extends Thread {
    private final String version;
    private final Path home;
    private final JProgressBar progressBar;
    private final CompletableFuture<Path> callback;

    public DownloadThread(CompletableFuture<Path> callback, JProgressBar progressBar, Path home, String version) {
        this.progressBar = progressBar;
        this.home = home;
        this.version = version;
        this.callback = callback;
    }

    /**
     * @noinspection BusyWait
     */
    public void run() {
        String url = "http://res01.uhcraft.fr/uhcraft/update/" + version + "/UHCraft-" + version + ".jar";
        try {
            if (!Files.exists(home)) {
                Files.createDirectories(home);
                Path parent = home;
                while (parent != null) {
                    try {
                        parent.getFileSystem().provider().checkAccess(parent);
                    } catch (IOException e) {
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final Path file = home.resolve("UHCraft-" + version + ".jar");
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        AtomicBoolean end = new AtomicBoolean();
        end.set(false);

        int downloadSize = downloadFile(url, file, () -> {
            try {
                Files.write(home.resolve(Panel.VERSION_FILENAME), version.getBytes(), StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                e.printStackTrace();
            }

            end.set(true);
            callback.complete(file);
        });
        System.out.println("Download size: " + downloadSize);

        new Thread(() -> {
            while (!end.get()) {
                long progression = 0;
                try {
                    progression = Files.size(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Raw downloaded size: " + progression);
                progressBar.setValue((int) (progression * 100 / downloadSize));
                System.out.println("Progression: " + progression * 100 / downloadSize);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).run();
    }

    public static int downloadFile(String fromUrl, Path filePath, Runnable callback) {
        try {
            URL url = new URL(fromUrl);

            final URLConnection conn = url.openConnection();
            int length = conn.getContentLength();
            new Thread(() -> {
                try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(filePath)) {
                    byte[] buffer = new byte[1024];
                    int numRead;
                    while ((numRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numRead);
                    }
                    callback.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            return length;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
