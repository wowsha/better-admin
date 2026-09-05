package wowsha.betteradmin.util;

import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RestartManager {
    private RestartManager() {}

    public static void restart(MinecraftServer server) {
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());

        String commandLine = System.getProperty("sun.java.command", "").trim();
        if (commandLine.isEmpty()) {
            server.halt(false);
            return;
        }

        command.addAll(splitCommandLine(commandLine));

        File workingDirectory = new File(System.getProperty("user.dir", "."));
        Thread relaunch = new Thread(() -> {
            try {
                Thread.sleep(2000L);
                new ProcessBuilder(command)
                        .directory(workingDirectory)
                        .inheritIO()
                        .start();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (IOException ignored) {
            }
        }, "troll-commands-restart");
        relaunch.setDaemon(true);
        relaunch.start();

        server.halt(false);
    }

    private static String javaExecutable() {
        String javaHome = System.getProperty("java.home");
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe" : "java";
        return javaHome + File.separator + "bin" + File.separator + executable;
    }

    private static List<String> splitCommandLine(String commandLine) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;

        for (int i = 0; i < commandLine.length(); i++) {
            char c = commandLine.charAt(i);
            if ((c == '\'' || c == '"')) {
                if (quoted && c == quote) {
                    quoted = false;
                } else if (!quoted) {
                    quoted = true;
                    quote = c;
                } else {
                    current.append(c);
                }
            } else if (Character.isWhitespace(c) && !quoted) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) result.add(current.toString());
        return result;
    }
}
