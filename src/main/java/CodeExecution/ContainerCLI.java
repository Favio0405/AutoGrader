package CodeExecution;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class ContainerCLI {
    public static void createAndStart(String name, String image) throws IOException, InterruptedException {
        ExecResult r = execute(Duration.ofSeconds(20),
                "docker", "run", "-d",
                "--name", name,
                "--cpus=1",
                "--memory=512m",
                "--memory-swap=512m",
                "--pids-limit=256",
                "--network=none",
                "--read-only",
                "--cap-drop=ALL",
                "--security-opt", "no-new-privileges",
                "--tmpfs", "/tmp:rw,nosuid,noexec,size=64m",
                "--tmpfs", "/work:rw,nosuid,size=256m",
                image,
                "bash", "-lc", "sleep infinity"
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
