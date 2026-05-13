package org.somanybits.minitel.hardware;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

/**
 * Contrôleur de 4 LEDs câblées sur les GPIO BCM 5, 6, 12 et 13.
 *
 * Utilise le sysfs GPIO avec l'offset dynamique de gpiochip512 (Linux 6.x).
 * Sur Linux 6.x, les GPIO ne s'exportent plus avec leur numéro BCM brut :
 * il faut lire la base dans /sys/class/gpio/gpiochip512/base et ajouter
 * le numéro BCM pour obtenir le numéro sysfs réel.
 *
 * Câblage : LED active-high (anode côté GPIO, cathode via résistance à GND).
 * Graceful degradation si le sysfs n'est pas accessible.
 */
public class GPIOLed {

    /** Numéros BCM des 4 broches LED (indices 0–3). */
    public static final int[] GPIO_PINS = { 5, 6, 12, 13 };
    public static final int   COUNT     = GPIO_PINS.length;

    private static final String SYSFS_GPIO   = "/sys/class/gpio";
    private static final String CHIP_PRIMARY = "gpiochip512";
    private static final String CHIP_LEGACY  = "gpiochip0";

    private final int[] sysfsNum = new int[COUNT]; // numéros sysfs (base + BCM)
    private boolean available = false;

    public boolean init() {
        try {
            int base = readBase();

            for (int i = 0; i < COUNT; i++) {
                sysfsNum[i] = base + GPIO_PINS[i];
                export(sysfsNum[i]);
                write(sysfsNum[i], "direction", "out");
                write(sysfsNum[i], "value", "0");
            }

            available = true;
            System.out.println("GPIOLed initialisé — GPIO " + Arrays.toString(GPIO_PINS)
                    + " (base sysfs=" + base + ")");
        } catch (Exception | Error e) {
            System.out.println("GPIOLed non disponible (" + e.getClass().getSimpleName()
                    + "): " + e.getMessage());
            available = false;
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    // ── API publique ──────────────────────────────────────────────────────────

    public void set(int index, boolean on) {
        if (!available || index < 0 || index >= COUNT) return;
        writeValue(sysfsNum[index], on);
    }

    public void toggle(int index) {
        if (!available || index < 0 || index >= COUNT) return;
        writeValue(sysfsNum[index], !readValue(sysfsNum[index]));
    }

    public void allOn() {
        if (!available) return;
        for (int n : sysfsNum) writeValue(n, true);
    }

    public void allOff() {
        if (!available) return;
        for (int n : sysfsNum) writeValue(n, false);
    }

    public boolean getState(int index) {
        if (!available || index < 0 || index >= COUNT) return false;
        return readValue(sysfsNum[index]);
    }

    public void close() {
        if (!available) return;
        available = false;
        for (int n : sysfsNum) {
            try { writeValue(n, false); } catch (Exception ignored) {}
        }
    }

    // ── Accès sysfs ───────────────────────────────────────────────────────────

    /** Lit la base du chip principal ; essaie gpiochip512 puis gpiochip0. */
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

    private void write(int num, String attr, String value) throws IOException {
        Files.writeString(Paths.get(SYSFS_GPIO, "gpio" + num, attr), value);
    }

    private void writeValue(int num, boolean high) {
        try {
            Files.writeString(Paths.get(SYSFS_GPIO, "gpio" + num, "value"), high ? "1" : "0");
        } catch (IOException e) {
            System.out.println("GPIOLed erreur écriture gpio" + num + ": " + e.getMessage());
        }
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
