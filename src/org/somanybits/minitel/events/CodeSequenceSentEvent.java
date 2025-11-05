/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel.events;

/**
 *
 * @author eddy
 */
public class CodeSequenceSentEvent {

    static final public int SEQ_80_COL_MODE = 0x1370;
    static final public int SEQ_40_COL_MODE = 0x1371;

    private byte[] codesequence;

    public CodeSequenceSentEvent(byte[] codesequence) {
        this.codesequence = codesequence;
    }

    public byte[] getSequenceCode() {
        return this.codesequence;
    }
}
