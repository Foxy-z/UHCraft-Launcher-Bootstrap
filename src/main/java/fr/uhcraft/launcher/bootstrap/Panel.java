package fr.uhcraft.launcher.bootstrap;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InvalidObjectException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class Panel extends JPanel {
    private final Bootstrap instance;

    private final JLabel label;
    private final JProgressBar progressBar;

    private static final String VERSION_URL = "http://res01.uhcraft.fr/uhcraft/update/version.txt";
    public static final String VERSION_FILENAME = "bootstrap_version.txt";


    private static final String DOWNLOAD_FAILED = "Impossible de vérifier la version, vérifiez votre connexion internet !";
    private static final String DOWNLOAD_IN_PROGRESS = "Téléchargement du launcher en cours...";
    private static final String VERSION_FOUND = "Version du launcher: %s";

    public Panel(Bootstrap instance) {
        this.instance = instance;

        add(label = new JLabel(DOWNLOAD_IN_PROGRESS));
        add(progressBar = new JProgressBar(0, 100));

        progressBar.setOrientation(SwingConstants.HORIZONTAL);
        progressBar.setValue(0);

        checkVersion();
    }

    private void sendRetryVersionCheck() {
        final JButton retryButton = new JButton("Réessayer");
        retryButton.addActionListener(actionEvent -> {
            label.setText(DOWNLOAD_FAILED);
            remove(retryButton);
            checkVersion();
        });
        add(retryButton);
    }

    private void checkVersion() {
        final CompletableFuture<String> future = getCurrentVersion();
        future.exceptionally((e) -> {
            sendRetryVersionCheck();
            return null;
        }).thenAccept((version) -> {
            if (version == null) return;

            label.setText(String.format(VERSION_FOUND, version));
            if (version.equals(getLocalVersion())) {
                progressBar.setValue(100);
                Path launcher = Paths.get(System.getProperty("user.home") + "/.UHCraft").resolve("UHCraft-" + version + ".jar");
                startLauncher(launcher);
            } else {
                Path home = Paths.get(System.getProperty("user.home") + "/.UHCraft");
                CompletableFuture<Path> callback = new CompletableFuture<>();
                DownloadThread thread = new DownloadThread(callback, progressBar, home, version);
                thread.start();
                callback.thenAccept(this::startLauncher);
            }
        });
    }

    private String getLocalVersion() {
        Path home = Paths.get(System.getProperty("user.home") + "/.UHCraft").resolve(VERSION_FILENAME);
        if (!Files.exists(home)) return "";
        try (BufferedReader reader = Files.newBufferedReader(home)) {
            return reader.readLine();
        } catch (IOException ignored) {
        }
        return "";
    }

    private CompletableFuture<String> getCurrentVersion() {
        CompletableFuture<String> future = new CompletableFuture<>();
        instance.run(() -> {
            try {
                final URLConnection connection = new URL(VERSION_URL).openConnection();
                connection.connect();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String version = reader.readLine();
                    if (version.isEmpty()) {
                        future.completeExceptionally(new InvalidObjectException("Version cannot be null."));
                    } else {
                        future.complete(version);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            } catch (IOException e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void startLauncher(Path path) {
        try {
            Runtime.getRuntime().exec("java -jar " + path.toAbsolutePath().toString());
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
