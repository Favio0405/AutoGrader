package Containerization;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ContainerCLI {
    private final static String image = "eclipse-temurin:21-jre";
    public static void createAndStart(String name, String testFile) throws IOException, InterruptedException {
        String runnerDir = Paths.get("target/runner").toAbsolutePath().toString();
        String testFileAbs = Paths.get(testFile).toAbsolutePath().toString();
        ExecResult r = execute(Duration.ofSeconds(20),
                "docker", "run", "-d",
                "--name", name,
                "--user",
                "65534:65534",
                "--cpus=1",
                "--memory=512m",
                "--memory-swap=512m",
                "--pids-limit=256",
                "--network=none",
                "--read-only",
                "--cap-drop=ALL",
                "--security-opt", "no-new-privileges",
                "-v", runnerDir + ":/runner:ro",
                "-v", testFileAbs + ":/tests/tests.bin:ro",
                "--tmpfs", "/tmp:rw,nosuid,noexec,size=64m",
                "--tmpfs", "/work:rw,nosuid,size=256m",
                image,
                "sleep", "infinity"
        );

        if (r.exitCode() != 0) {
            throw new IOException("Failed to start container " + name + ":\n" + r.output());
        }
    }

    public static void remove(String name) throws IOException, InterruptedException {
        ExecResult result = execute(Duration.ofSeconds(10), "docker", "rm", "-f", name);
        if (result.exitCode() != 0) throw new IOException("Failed to remove " + name);
    }
    private static ExecResult execute(Duration timeout, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if(!finished) {
            process.destroyForcibly();
            return new ExecResult(124, "(timeout)");
        }

        String out;
        try (InputStream in = process.getInputStream()) {
            out = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        return new ExecResult(process.exitValue(), out);
    }

    private record ExecResult(int exitCode, String output) {}
}
