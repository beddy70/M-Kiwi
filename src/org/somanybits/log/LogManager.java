/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.log;

/**
 *
 * @author eddy
 */
public class LogManager {

    public final static int MSG_TYPE_MESSAGE = 0;
    public final static int MSG_TYPE_WARNING = 1;
    public final static int MSG_TYPE_ERROR = 2;

    public static final String ANSI_RESET = "\u001B[0m";

    //BOLD - INK COLOR
    public static final String ANSI_BOLD_BLACK = "\u001B[1;30m";
    public static final String ANSI_BOLD_RED = "\u001B[1;31m";
    public static final String ANSI_BOLD_GREEN = "\u001B[1;32m";
    public static final String ANSI_BOLD_YELLOW = "\u001B[1;33m";
    public static final String ANSI_BOLD_BLUE = "\u001B[1;34m";
    public static final String ANSI_BOLD_PURPLE = "\u001B[1;35m";
    public static final String ANSI_BOLD_CYAN = "\u001B[1;36m";
    public static final String ANSI_BOLD_WHITE = "\u001B[1;37m";

    //INK COLOR
    public static final String ANSI_BLACK = "\u001B[0;30m";
    public static final String ANSI_RED = "\u001B[0;31m";
    public static final String ANSI_GREEN = "\u001B[0;32m";
    public static final String ANSI_YELLOW = "\u001B[0;33m";
    public static final String ANSI_BLUE = "\u001B[0;34m";
    public static final String ANSI_PURPLE = "\u001B[0;35m";
    public static final String ANSI_CYAN = "\u001B[0;36m";
    public static final String ANSI_WHITE = "\u001B[0;37m";

    //BACKGROUND COLOR
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    public static final String DEFLAUT_PREFIX = "> Message : ";
    int counter_err = 0, counter_warn = 0;

    String msg;
    String prefix = DEFLAUT_PREFIX;

    public void addLog(String msg) {
        buildLog(MSG_TYPE_MESSAGE);
        this.msg += msg;
        System.out.println(this.msg + ANSI_RESET);
    }

    public void addLog(String msg, int logtype) {

        buildLog(logtype);
        this.msg += msg;
        System.out.println(this.msg + ANSI_RESET);
    }

    public void addLog(String msg, int logtype, int line, int col) {
        buildLog(logtype);
        this.msg += msg + " line:" + line + " col:" + col;
        System.out.println(this.msg + ANSI_RESET);
    }

    private void buildLog(int logtype) {
        this.msg = "";
        switch (logtype) {
            case MSG_TYPE_MESSAGE:
                this.msg += this.prefix + this.msg;
                break;
            case MSG_TYPE_WARNING:
                this.msg += ANSI_BOLD_YELLOW + "> Warning : " + msg;
                counter_warn++;
                break;
            case MSG_TYPE_ERROR:
                this.msg += ANSI_BOLD_RED + "> Error   : " + msg;
                counter_err++;
                break;
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void getStatus() {
        System.err.println("> " + ANSI_BOLD_WHITE + "Status  : " + counter_warn + " warning(s) " + counter_err + " error(s)" + ANSI_RESET);
    }

    public int getNumError() {
        return this.counter_err;
    }
}
