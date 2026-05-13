package org.somanybits.minitel.hardware;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfig;
import com.pi4j.io.gpio.digital.DigitalState;

/**
 * Contrôleur de 4 LEDs câblées sur les GPIO BCM 5, 6, 12 et 13.
 *
 * Les LEDs sont numérotées par indice (0–3) :
 *   index 0 → GPIO 5
 *   index 1 → GPIO 6
 *   index 2 → GPIO 12
 *   index 3 → GPIO 13
 *
 * Câblage supposé : LED active-high (anode côté GPIO, cathode via résistance à GND).
 * Graceful degradation : si Pi4J ou les GPIO ne sont pas disponibles,
 * {@link #isAvailable()} retourne false et aucune exception n'est propagée.
 *
 * Exemple :
 * <pre>
 *   GPIOLed leds = new GPIOLed();
 *   if (leds.init()) {
 *       leds.set(0, true);   // allume LED 0 (GPIO 5)
 *       leds.toggle(2);      // bascule LED 2 (GPIO 12)
 *       leds.allOff();
 *   }
 * </pre>
 */
public class GPIOLed {

    /** Numéros BCM des 4 broches LED. */
    public static final int[] GPIO_PINS = { 5, 6, 12, 13 };
    public static final int COUNT = GPIO_PINS.length;

    private Context        pi4j;
    private DigitalOutput[] outputs;
    private boolean         available = false;

    /**
     * Initialise Pi4J et configure les 4 sorties GPIO.
     * @return true si toutes les sorties sont prêtes, false sinon (silencieux)
     */
    public boolean init() {
        try {
            pi4j   = Pi4J.newAutoContext();
            outputs = new DigitalOutput[COUNT];

            for (int i = 0; i < COUNT; i++) {
                DigitalOutputConfig cfg = DigitalOutput.newConfigBuilder(pi4j)
                        .id("led-gpio" + GPIO_PINS[i])
                        .name("LED GPIO" + GPIO_PINS[i])
                        .address(GPIO_PINS[i])
                        .shutdown(DigitalState.LOW)
                        .initial(DigitalState.LOW)
                        .build();
                outputs[i] = pi4j.create(cfg);
            }

            available = true;
            System.out.println("GPIOLed initialisé — GPIO " + java.util.Arrays.toString(GPIO_PINS));
        } catch (Exception | Error e) {
            System.out.println("GPIOLed non disponible (" + e.getClass().getSimpleName() + "): " + e.getMessage());
            available = false;
            shutdown();
        }
        return available;
    }

    public boolean isAvailable() { return available; }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Allume ou éteint une LED.
     * @param index indice de la LED (0–3)
     * @param on    true = allumée, false = éteinte
     */
    public void set(int index, boolean on) {
        if (!available || index < 0 || index >= COUNT) return;
        if (on) outputs[index].high();
        else    outputs[index].low();
    }

    /** Bascule l'état d'une LED. */
    public void toggle(int index) {
        if (!available || index < 0 || index >= COUNT) return;
        outputs[index].toggle();
    }

    /** Allume toutes les LEDs. */
    public void allOn() {
        if (!available) return;
        for (DigitalOutput out : outputs) out.high();
    }

    /** Éteint toutes les LEDs. */
    public void allOff() {
        if (!available) return;
        for (DigitalOutput out : outputs) out.low();
    }

    /**
     * Retourne l'état courant d'une LED.
     * @return true si allumée, false si éteinte ou indisponible
     */
    public boolean getState(int index) {
        if (!available || index < 0 || index >= COUNT) return false;
        return outputs[index].state() == DigitalState.HIGH;
    }

    /** Libère les ressources Pi4J. */
    public void close() {
        available = false;
        allOff();
        shutdown();
    }

    private void shutdown() {
        if (pi4j != null) {
            try { pi4j.shutdown(); } catch (Exception ignored) {}
            pi4j = null;
        }
    }
}
