package com.yomahub.liteflow.agent.tool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/** JDK-only subprocess fixture for deterministic shell lifecycle tests. */
public final class ShellProcessFixture {

    private ShellProcessFixture() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "flood" -> flood(Integer.parseInt(args[1]));
            case "tree" -> tree(Path.of(args[1]), Path.of(args[2]));
            case "child" -> new CountDownLatch(1).await();
            default -> throw new IllegalArgumentException("unknown fixture mode: " + args[0]);
        }
    }

    private static void flood(int bytes) {
        byte[] chunk = new byte[8192];
        java.util.Arrays.fill(chunk, (byte) 'x');
        int remaining = bytes;
        while (remaining > 0) {
            int count = Math.min(chunk.length, remaining);
            System.out.write(chunk, 0, count);
            remaining -= count;
        }
        System.out.flush();
    }

    private static void tree(Path parentPid, Path childPid) throws Exception {
        Files.writeString(parentPid, Long.toString(ProcessHandle.current().pid()));
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process child = new ProcessBuilder(
                        java,
                        "-cp",
                        System.getProperty("java.class.path"),
                        ShellProcessFixture.class.getName(),
                        "child")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        Files.writeString(childPid, Long.toString(child.pid()));
        new CountDownLatch(1).await();
    }
}
