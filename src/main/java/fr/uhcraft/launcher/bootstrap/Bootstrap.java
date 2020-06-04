package fr.uhcraft.launcher.bootstrap;

import javax.swing.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Bootstrap extends JFrame {

    final ExecutorService service = Executors.newCachedThreadPool();

    public Bootstrap() {
        setTitle("UHCraft Moddé | Saison 2");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        setSize(400, 150);
        setContentPane(new Panel(this));

        setVisible(true);
    }

    public static void main(String... args) {
        new Bootstrap();
    }

    public void run(Runnable runnable) {
        service.submit(runnable);
    }

    public <V> void run(Callable<V> callable) {
        service.submit(callable);
    }
}
