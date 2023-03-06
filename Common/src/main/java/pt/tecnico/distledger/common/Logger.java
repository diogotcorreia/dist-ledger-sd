package pt.tecnico.distledger.common;


import lombok.Setter;
import lombok.Value;

import java.util.Objects;

@Value(staticConstructor = "of")
public class Logger {
    @Setter
    private static boolean debug = false;

    String name;

    private String getPrefix() {
        if (Objects.isNull(name) || name.isBlank()) {
            return "";
        }
        return "[" + name + "] ";
    }

    public void debug(String message, Object... args) {
        if (debug) {
            System.err.printf("[DEBUG] " + getPrefix() + message + "%n", args);
        }
    }

    public void info(String message, Object... args) {
        System.out.printf(getPrefix() + message + "%n", args);
    }

    public void warn(String message, Object... args) {
        System.err.printf("[WARN] " + getPrefix() + message + "%n", args);
    }

    public void error(String message, Object... args) {
        System.err.printf("[ERROR] " + getPrefix() + message + "%n", args);
    }

}
