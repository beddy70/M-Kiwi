#!/bin/bash
# Set /dev/serial0 to 1220 baud
sudo stty -F /dev/serial0 1200 cs7 parenb -parodd -cstopb -ixon -ixoff -echo -icanon -opost
# Sequence to switch Mintel serial device in 9600 baud speed (only Minitel 2) 
sudo bash -c "printf '\x1B\x3A\x6B\x7F' > /dev/serial0"
