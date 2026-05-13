package org.somanybits.minitel.hardware;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalInput;
import com.pi4j.io.gpio.digital.DigitalInputConfig;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.gpio.digital.PullResistance;

/**
 * Capture les événements de 3 boutons-poussoirs câblés sur les GPIO BCM 20, 21 et 25.
 *
 * Les boutons sont numérotés par indice (0–2) :
 *   index 0 → GPIO 20
 *   index 1 → GPIO 21
 *   index 2 → GPIO 25
 *
 * Câblage supposé : bouton entre GPIO et GND, résistance interne PULL_UP activée.
 * Appui = état LOW, relâché = état HIGH.
 *
 * Un anti-rebond logiciel (10 ms) complète l'anti-rebond matériel de Pi4J.
 * Graceful degradation : si Pi4J ou les GPIO ne sont pas disponibles,
 * {@link #isAvailable()} retourne false et aucune exception n'est propagée.
 *
 * Exemple :
 * <pre>
 *   GPIOButton buttons = new GPIOButton();
 *   buttons.setListener(new GPIOButton.Listener() {
 *       public void onPressed(int index)  { System.out.println("BTN " + index + " pressé");  }
 *       public void onReleased(int index) { System.out.println("BTN " + index + " relâché"); }
 *   });
 *   buttons.init();
 * </pre>
 */
public class GPIOButton {

    /** Numéros BCM des 3 broches bouton. */
    public static final int[] GPIO_PINS = { 20, 21, 25 };
    public static final int COUNT = GPIO_PINS.length;

    /** Durée minimale entre deux événements (anti-rebond logiciel, en ms). */
    private static final long DEBOUNCE_MS = 10;

    // ── Interface listener ────────────────────────────────────────────────────

    public interface Listener {
        /** Appuyé (front descendant sur pull-up). */
        void onPressed(int index);
        /** Relâché (front montant sur pull-up). */
        void onReleased(int index);
    }

    // ── Champs internes ───────────────────────────────────────────────────────

    private Context      pi4j;
    private DigitalInput[] inputs;
    private Listener     listener;
    private boolean      available = false;

    private final long[] lastEventMs = new long[COUNT];

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    /**
     * Définit le listener avant ou après l'init.
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Initialise Pi4J et configure les 3 entrées GPIO avec pull-up.
     * @return true si toutes les entrées sont prêtes, false sinon (silencieux)
     */
    public boolean init() {
        try {
            pi4j  = Pi4J.newAutoContext();
            inputs = new DigitalInput[COUNT];

            for (int i = 0; i < COUNT; i++) {
                final int idx = i;

                DigitalInputConfig cfg = DigitalInput.newConfigBuilder(pi4j)
                        .id("btn-gpio" + GPIO_PINS[i])
                        .name("Button GPIO" + GPIO_PINS[i])
                        .address(GPIO_PINS[i])
                        .pull(PullResistance.PULL_UP)
                        .debounce(3000L)   // 3 ms anti-rebond matériel (µs)
                        .build();

                inputs[i] = pi4j.create(cfg);

                inputs[i].addListener(event -> {
                    long now = System.currentTimeMillis();
                    if (now - lastEventMs[idx] < DEBOUNCE_MS) return;
                    lastEventMs[idx] = now;

                    if (listener == null) return;
                    if (event.state() == DigitalState.LOW) {
                        listener.onPressed(idx);
                    } else if (event.state() == DigitalState.HIGH) {
                        listener.onReleased(idx);
                    }
                });
            }

            available = true;
            System.out.println("GPIOButton initialisé — GPIO " + java.util.Arrays.toString(GPIO_PINS));
        } catch (Exception | Error e) {
            System.out.println("GPIOButton non disponible (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            available = false;
            shutdown();
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    /**
     * Lit l'état instantané d'un bouton (sans événement).
     * @return true si le bouton est pressé en ce moment, false sinon
     */
    public boolean isPressed(int index) {
        if (!available || index < 0 || index >= COUNT) return false;
        return inputs[index].state() == DigitalState.LOW;
    }

    /** Libère les ressources Pi4J. */
    public void close() {
        available = false;
        shutdown();
    }

    private void shutdown() {
        if (pi4j != null) {
            try { pi4j.shutdown(); } catch (Exception ignored) {}
            pi4j = null;
        }
    }
}
