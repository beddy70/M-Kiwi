package org.somanybits.minitel.hardware;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Capture les événements de 3 boutons-poussoirs câblés sur les GPIO BCM 20, 21 et 25.
 *
 * Utilise le sysfs GPIO avec l'offset dynamique de gpiochip512 (Linux 6.x),
 * la même technique que GPIOLed. Un thread de polling à 10 ms lit la valeur
 * du GPIO et détecte les transitions.
 *
 * Câblage supposé : bouton entre GPIO et GND, pull-up externe ou via device tree.
 * Appui = état LOW (0), relâché = état HIGH (1).
 */
public class GPIOButton {

    /** Numéros BCM des 3 broches bouton. */
    public static final int[] GPIO_PINS = { 20, 21, 26 };
    public static final int   COUNT     = GPIO_PINS.length;

    private static final String SYSFS_GPIO   = "/sys/class/gpio";
    private static final String CHIP_PRIMARY = "gpiochip512";
    private static final String CHIP_LEGACY  = "gpiochip0";

    private static final long POLL_MS = 10;

    public interface Listener {
        void onPressed(int index);
        void onReleased(int index);
    }

    private final int[]     sysfsNum  = new int[COUNT];
    private final boolean[] lastState = new boolean[COUNT]; // true = HIGH
    private Listener listener;
    private Thread   pollThread;
    private boolean  available = false;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean init() {
        try {
            int base = readBase();

            for (int i = 0; i < COUNT; i++) {
                sysfsNum[i] = base + GPIO_PINS[i];
                export(sysfsNum[i]);
                write(sysfsNum[i], "direction", "in");
                configurePullUp(GPIO_PINS[i]);  // pull-up interne : pin HIGH au repos
            }
            Thread.sleep(20); // stabilisation pull-up avant lecture état initial
            for (int i = 0; i < COUNT; i++) {
                lastState[i] = readValue(sysfsNum[i]);
                System.out.println("GPIOButton GPIO " + GPIO_PINS[i]
                        + " état initial=" + (lastState[i] ? "HIGH" : "LOW"));
            }

            available = true;
            System.out.println("GPIOButton initialisé — GPIO " + Arrays.toString(GPIO_PINS)
                    + " (base sysfs=" + base + ")");

            pollThread = new Thread(this::pollLoop, "gpio-button-poll");
            pollThread.setDaemon(true);
            pollThread.start();

        } catch (Exception | Error e) {
            System.out.println("GPIOButton non disponible (" + e.getClass().getSimpleName()
                    + "): " + e.getMessage());
            available = false;
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    public boolean isPressed(int index) {
        if (!available || index < 0 || index >= COUNT) return false;
        return !readValue(sysfsNum[index]); // LOW = pressé
    }

    public void close() {
        available = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void pollLoop() {
        while (available && !Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < COUNT; i++) {
                boolean current = readValue(sysfsNum[i]);
                if (current != lastState[i]) {
                    lastState[i] = current;
                    if (listener != null) {
                        if (!current) listener.onPressed(i);   // HIGH→LOW = appui
                        else          listener.onReleased(i);  // LOW→HIGH = relâché
                    }
                }
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // ── Accès sysfs ───────────────────────────────────────────────────────────

    private int readBase() throws IOException {
        for (String chip : new String[]{ CHIP_PRIMARY, CHIP_LEGACY }) {
            Path p = Paths.get(SYSFS_GPIO, chip, "base");
            if (Files.exists(p)) {
                return Integer.parseInt(Files.readString(p).trim());
            }
        }
        throw new IOException("Aucun gpiochip trouvé dans " + SYSFS_GPIO);
    }

    private void export(int num) throws IOException, InterruptedException {
        // Unexport d'abord pour garantir un état propre
        Path gpioDir = Paths.get(SYSFS_GPIO, "gpio" + num);
        if (Files.exists(gpioDir)) {
            Files.writeString(Paths.get(SYSFS_GPIO, "unexport"), String.valueOf(num));
            Thread.sleep(50);
        }
        Files.writeString(Paths.get(SYSFS_GPIO, "export"), String.valueOf(num));
        Thread.sleep(100);
    }

    /** Active le pull-up interne via raspi-gpio (BCM pin number). */
    private void configurePullUp(int bcmPin) {
        try {
            Process p = new ProcessBuilder("raspi-gpio", "set",
                    String.valueOf(bcmPin), "ip", "pu")
                    .redirectErrorStream(true).start();
            p.waitFor(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("GPIOButton: pull-up non configuré sur GPIO "
                    + bcmPin + " (" + e.getMessage() + ")");
        }
    }

    private void write(int num, String attr, String value) throws IOException {
        Files.writeString(Paths.get(SYSFS_GPIO, "gpio" + num, attr), value);
    }

    private boolean readValue(int num) {
        try {
            return "1".equals(Files.readString(
                    Paths.get(SYSFS_GPIO, "gpio" + num, "value")).trim());
        } catch (IOException e) {
            return false;
        }
    }
}
