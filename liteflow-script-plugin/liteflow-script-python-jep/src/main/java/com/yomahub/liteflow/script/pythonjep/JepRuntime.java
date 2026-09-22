package com.yomahub.liteflow.script.pythonjep;

import jep.Interpreter;
import jep.JepConfig;
import jep.MainInterpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Windows 下通过 pip 自动定位 JEP 原生运行时。
 */
public final class JepRuntime {

    private static final String WINDOWS = "Windows";
    private static final int DISCOVERY_TIMEOUT_SECONDS = 10;
    private static boolean initialized;

    private JepRuntime() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Interpreter createInterpreter(JepConfig config) {
        initialize();
        return config.createSubInterpreter();
    }

    private static synchronized void initialize() {
        if (initialized) {
            return;
        }
        if (System.getProperty("os.name").startsWith(WINDOWS)) {
            initializeWindows();
        }
        initialized = true;
    }

    private static void initializeWindows() {
        try {
            Process process = new ProcessBuilder("pip", "show", "jep").start();
            if (!process.waitFor(DISCOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException();
            }

            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                throw new IllegalStateException();
            }
            String location = output.lines()
                    .filter(line -> line.startsWith("Location:"))
                    .map(line -> line.substring("Location:".length()).strip())
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
            Path jepLibrary = Path.of(location, "jep", "jep.dll").toAbsolutePath();
            if (!Files.isRegularFile(jepLibrary)) {
                throw new IllegalStateException();
            }
            MainInterpreter.setJepLibraryPath(jepLibrary.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw initializationException(e);
        } catch (IOException | RuntimeException | LinkageError e) {
            throw initializationException(e);
        }
    }

    private static IllegalStateException initializationException(Throwable cause) {
        return new IllegalStateException("python环境初始化异常", cause);
    }
}
