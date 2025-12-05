#!/usr/bin/env python3
"""
Script de vibration pour manettes USB via evdev.
Usage: rumble.py <event_device> <duration_ms> [intensity]
"""
import sys
import os
import struct
import time

def rumble(device, duration_ms, intensity=1.0):
    """Vibration via écriture directe."""
    try:
        fd = os.open(device, os.O_RDWR)
        
        strong = int(min(1.0, max(0.0, intensity)) * 0xFFFF)
        weak = int(strong * 0.5)
        
        # ff_effect pour FF_RUMBLE (0x50)
        effect = bytearray(48)
        struct.pack_into('<H', effect, 0, 0x50)   # type = FF_RUMBLE
        struct.pack_into('<h', effect, 2, -1)     # id = -1 (nouveau)
        struct.pack_into('<H', effect, 4, 0)      # direction
        struct.pack_into('<H', effect, 6, 0)      # trigger.button
        struct.pack_into('<H', effect, 8, 0)      # trigger.interval
        struct.pack_into('<H', effect, 10, duration_ms)  # replay.length
        struct.pack_into('<H', effect, 12, 0)     # replay.delay
        struct.pack_into('<H', effect, 14, strong)  # rumble.strong
        struct.pack_into('<H', effect, 16, weak)    # rumble.weak
        
        # EVIOCSFF ioctl
        import fcntl
        EVIOCSFF = 0x40304580
        result = bytearray(effect)
        fcntl.ioctl(fd, EVIOCSFF, result)
        effect_id = struct.unpack_from('<h', result, 2)[0]
        
        # Jouer l'effet
        now = time.time()
        event = struct.pack('<qqHHi', int(now), int((now % 1) * 1000000), 0x15, effect_id, 1)
        os.write(fd, event)
        
        time.sleep(duration_ms / 1000.0)
        
        # Arrêter
        now = time.time()
        event = struct.pack('<qqHHi', int(now), int((now % 1) * 1000000), 0x15, effect_id, 0)
        os.write(fd, event)
        
        os.close(fd)
        print(f"OK {duration_ms}ms")
        return True
        
    except Exception as e:
        print(f"Erreur: {e}")
        return False

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: rumble.py <device> <duration_ms> [intensity]")
        sys.exit(1)
    
    device = sys.argv[1]
    duration = int(sys.argv[2])
    intensity = float(sys.argv[3]) if len(sys.argv) > 3 else 1.0
    
    sys.exit(0 if rumble(device, duration, intensity) else 1)
