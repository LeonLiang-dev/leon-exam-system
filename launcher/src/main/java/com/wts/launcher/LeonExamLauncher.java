package com.wts.launcher;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public final class LeonExamLauncher {

    private static final String APP_NAME = "LeonExam";
    private static final int SERVER_PORT = 8080;
    private static final int DB_PORT = 3307;
    private static final Duration START_TIMEOUT = Duration.ofSeconds(90);
    private static final String APP_JAR_NAME = "wts-app-2.0.0-SNAPSHOT.jar";

    private final JFrame frame = new JFrame("Leon在线考试系统");
    private final JLabel statusLabel = new JLabel("准备启动");
    private final JLabel teacherUrlLabel = new JLabel("-");
    private final JLabel studentUrlLabel = new JLabel("-");
    private final JTextArea logArea = new JTextArea(18, 86);
    private final JButton startButton = new JButton("启动");
    private final JButton stopButton = new JButton("停止");
    private final JButton openButton = new JButton("打开教师端");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final Path appDir;
    private final Path dataRoot;
    private Process databaseProcess;
    private Process serverProcess;

    private LeonExamLauncher() {
        this.appDir = resolveAppDir();
        this.dataRoot = resolveDataRoot();
        initUi();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServices));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep default Swing look and feel.
            }
            LeonExamLauncher launcher = new LeonExamLauncher();
            launcher.frame.setVisible(true);
            launcher.startAsync();
        });
    }

    private void initUi() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                setStatus("正在停止服务...");
                stopServices();
                executor.shutdownNow();
                frame.dispose();
                System.exit(0);
            }
        });

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);

        JPanel infoPanel = new JPanel(new GridLayout(3, 2, 8, 6));
        infoPanel.add(new JLabel("状态"));
        infoPanel.add(statusLabel);
        infoPanel.add(new JLabel("教师端"));
        infoPanel.add(teacherUrlLabel);
        infoPanel.add(new JLabel("学生访问地址"));
        infoPanel.add(studentUrlLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(openButton);

        startButton.addActionListener(e -> startAsync());
        stopButton.addActionListener(e -> executor.submit(this::stopServices));
        openButton.addActionListener(e -> openBrowser("http://127.0.0.1:" + SERVER_PORT));
        stopButton.setEnabled(false);
        openButton.setEnabled(false);

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(infoPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(new JScrollPane(logArea), BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private void startAsync() {
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        executor.submit(() -> {
            try {
                startServices();
                setStatus("运行中");
                setOpenEnabled(true);
            } catch (Exception e) {
                appendLog("启动失败: " + e.getMessage());
                setStatus("启动失败");
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });
    }

    private void startServices() throws Exception {
        appendLog("安装目录: " + appDir);
        appendLog("数据目录: " + dataRoot);
        Files.createDirectories(dataRoot);
        Files.createDirectories(dataRoot.resolve("logs"));
        Files.createDirectories(dataRoot.resolve("uploads"));

        Path appJar = appDir.resolve("app").resolve(APP_JAR_NAME);
        Path mariaDbDir = appDir.resolve("mariadb");
        Path sqlInit = appDir.resolve("sql").resolve("init").resolve("wts.v1.4.1.sql");
        Path sqlMigrationsDir = appDir.resolve("sql").resolve("migrations");
        requireFile(appJar, "后端 JAR");
        requireFile(sqlInit, "初始化 SQL");
        requireFile(mariaDbDir.resolve("bin").resolve(executable("mysqld")), "MariaDB mysqld");
        requireFile(mariaDbDir.resolve("bin").resolve(executable("mysql")), "MariaDB mysql client");

        initDatabaseFiles(mariaDbDir);
        startDatabase(mariaDbDir);
        waitForDatabase(mariaDbDir);
        initializeSchemaIfNeeded(mariaDbDir, sqlInit, sqlMigrationsDir);

        Path configFile = writeApplicationConfig();
        startServer(appJar, configFile);
        waitForServer();

        String teacherUrl = "http://127.0.0.1:" + SERVER_PORT;
        String studentUrl = detectLanAddress()
                .map(ip -> "http://" + ip + ":" + SERVER_PORT)
                .orElse("未检测到局域网 IPv4 地址");
        setUrls(teacherUrl, studentUrl);
        appendLog("教师端地址: " + teacherUrl);
        appendLog("学生访问地址: " + studentUrl);
        appendLog("若学生无法访问，请在 Windows 防火墙中放行 TCP " + SERVER_PORT + " 端口。");
        openBrowser(teacherUrl);
    }

    private void initDatabaseFiles(Path mariaDbDir) throws IOException, InterruptedException {
        Path mysqlData = dataRoot.resolve("mysql");
        if (isInitializedDatabaseDirectory(mysqlData)) {
            appendLog("检测到已有数据库数据目录，跳过初始化。");
            return;
        }
        appendLog("首次运行，正在初始化 MariaDB 数据目录...");

        Path installDb = mariaDbDir.resolve("bin").resolve(executable("mariadb-install-db"));
        Path legacyInstallDb = mariaDbDir.resolve("bin").resolve(executable("mysql_install_db"));
        Path mysqld = mariaDbDir.resolve("bin").resolve(executable("mysqld"));
        List<List<String>> commands = new ArrayList<>();
        if (Files.exists(installDb)) {
            commands.add(List.of(installDb.toString(), "--datadir=" + mysqlData));
        }
        if (Files.exists(legacyInstallDb)) {
            commands.add(List.of(legacyInstallDb.toString(), "--datadir=" + mysqlData));
        }
        commands.add(List.of(mysqld.toString(),
                "--no-defaults",
                "--initialize-insecure",
                "--basedir=" + mariaDbDir,
                "--datadir=" + mysqlData));
        initializeDatabaseDirectory(mariaDbDir, mysqlData, commands);
    }

    private void initializeDatabaseDirectory(Path mariaDbDir, Path mysqlData, List<List<String>> commands)
            throws IOException, InterruptedException {
        Exception lastFailure = null;
        for (List<String> command : commands) {
            prepareEmptyDataDirectory(mysqlData);
            appendLog("尝试 MariaDB 初始化方式: " + commandLabel(command.get(0)));
            try {
                runCommand(command, mariaDbDir, null);
                appendLog("MariaDB 数据目录初始化完成。");
                return;
            } catch (IllegalStateException | IOException e) {
                lastFailure = e;
                appendLog("该初始化方式失败，准备尝试下一种: " + e.getMessage());
            }
        }
        if (lastFailure instanceof IOException ioException) {
            throw ioException;
        }
        throw new IllegalStateException("MariaDB 数据目录初始化失败", lastFailure);
    }

    private void prepareEmptyDataDirectory(Path mysqlData) throws IOException {
        if (Files.exists(mysqlData)) {
            if (!isDirectoryEmpty(mysqlData)) {
                appendLog("检测到未完成的 MariaDB 数据目录，正在清理后重试。");
            }
            try (Stream<Path> paths = Files.walk(mysqlData)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.equals(mysqlData)) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
        Files.createDirectories(mysqlData);
    }

    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        }
    }

    private static boolean isInitializedDatabaseDirectory(Path mysqlData) {
        Path mysqlSchema = mysqlData.resolve("mysql");
        return Files.exists(mysqlData.resolve("ibdata1"))
                || Files.exists(mysqlSchema.resolve("global_priv.frm"))
                || Files.exists(mysqlSchema.resolve("global_priv.MAD"))
                || Files.exists(mysqlSchema.resolve("user.frm"));
    }

    private void startDatabase(Path mariaDbDir) throws IOException {
        if (databaseProcess != null && databaseProcess.isAlive()) {
            return;
        }
        appendLog("正在启动 MariaDB...");
        Path mysqlData = dataRoot.resolve("mysql");
        Path mysqlLog = dataRoot.resolve("logs").resolve("mariadb.log");
        List<String> command = List.of(
                mariaDbDir.resolve("bin").resolve(executable("mysqld")).toString(),
                "--no-defaults",
                "--basedir=" + mariaDbDir,
                "--datadir=" + mysqlData,
                "--port=" + DB_PORT,
                "--bind-address=127.0.0.1",
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_general_ci",
                "--log-error=" + mysqlLog
        );
        databaseProcess = new ProcessBuilder(command)
                .directory(mariaDbDir.toFile())
                .redirectErrorStream(true)
                .start();
        pipeLogs(databaseProcess, "mariadb");
    }

    private void waitForDatabase(Path mariaDbDir) throws Exception {
        long deadline = System.currentTimeMillis() + START_TIMEOUT.toMillis();
        Path mysqlAdmin = mariaDbDir.resolve("bin").resolve(executable("mysqladmin"));
        requireFile(mysqlAdmin, "MariaDB mysqladmin");
        while (System.currentTimeMillis() < deadline) {
            int code = runCommandNoThrow(List.of(mysqlAdmin.toString(),
                    "--protocol=tcp",
                    "--host=127.0.0.1",
                    "--port=" + DB_PORT,
                    "--user=root",
                    "ping"), mariaDbDir);
            if (code == 0) {
                appendLog("MariaDB 已启动。");
                return;
            }
            sleep(1000);
        }
        throw new IllegalStateException("MariaDB 启动超时");
    }

    private void initializeSchemaIfNeeded(Path mariaDbDir, Path sqlInit, Path sqlMigrationsDir)
            throws IOException, InterruptedException {
        Path marker = dataRoot.resolve(".db-initialized");
        appendLog("正在创建数据库 wts...");
        runMysql(mariaDbDir, null, List.of("-e",
                "CREATE DATABASE IF NOT EXISTS wts DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"), null);

        if (Files.exists(marker)) {
            appendLog("数据库已初始化，检查增量迁移。");
        } else {
            appendLog("正在导入初始化 SQL，可能需要数分钟...");
            runMysql(mariaDbDir, "wts", List.of("--default-character-set=utf8mb4"), sqlInit);
            Files.writeString(marker, "initialized", StandardCharsets.UTF_8);
            appendLog("数据库基础结构初始化完成。");
        }
        runPendingMigrations(mariaDbDir, sqlMigrationsDir);
        appendLog("数据库初始化完成。");
    }

    private void runPendingMigrations(Path mariaDbDir, Path sqlMigrationsDir)
            throws IOException, InterruptedException {
        if (!Files.isDirectory(sqlMigrationsDir)) {
            appendLog("未发现增量迁移目录，跳过。");
            return;
        }

        List<Path> migrations;
        try (Stream<Path> paths = Files.list(sqlMigrationsDir)) {
            migrations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        if (migrations.isEmpty()) {
            appendLog("未发现增量迁移 SQL。");
            return;
        }

        Path markerDir = dataRoot.resolve(".migrations");
        Files.createDirectories(markerDir);
        boolean executed = false;
        for (Path migration : migrations) {
            String fileName = migration.getFileName().toString();
            Path migrationMarker = markerDir.resolve(fileName + ".applied");
            if (Files.exists(migrationMarker)) {
                continue;
            }
            appendLog("正在执行增量迁移 SQL: " + fileName);
            runMysql(mariaDbDir, "wts", List.of("--default-character-set=utf8mb4"), migration);
            Files.writeString(migrationMarker, "applied", StandardCharsets.UTF_8);
            executed = true;
        }

        if (!executed) {
            appendLog("没有待执行的增量迁移。");
        }
    }

    private Path writeApplicationConfig() throws IOException {
        Path configDir = dataRoot.resolve("config");
        Files.createDirectories(configDir);
        Path configFile = configDir.resolve("application.yml");
        if (Files.exists(configFile)) {
            appendLog("使用已有配置文件: " + configFile);
            return configFile;
        }
        String uploads = dataRoot.resolve("uploads").toString().replace("\\", "/");
        String yaml = """
                server:
                  port: 8080

                spring:
                  datasource:
                    url: jdbc:mysql://127.0.0.1:3307/wts?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
                    username: root
                    password: ""

                file:
                  upload-dir: %s

                app:
                  access:
                    lan:
                      enabled: true
                      auto-same-subnet: true
                      allowed-cidrs: ""
                """.formatted(uploads);
        Files.writeString(configFile, yaml, StandardCharsets.UTF_8);
        appendLog("已创建默认配置文件: " + configFile);
        return configFile;
    }

    private void startServer(Path appJar, Path configFile) throws IOException {
        if (serverProcess != null && serverProcess.isAlive()) {
            return;
        }
        appendLog("正在启动后端服务...");
        Path javaExe = resolveJavaExecutable();
        appendLog("Java 运行时: " + javaExe);
        List<String> command = List.of(
                javaExe.toString(),
                "-jar",
                appJar.toString(),
                "--spring.profiles.active=prod",
                "--spring.config.additional-location=" + configFile.toUri()
        );
        serverProcess = new ProcessBuilder(command)
                .directory(dataRoot.toFile())
                .redirectErrorStream(true)
                .start();
        pipeLogs(serverProcess, "server");
    }

    private Path resolveJavaExecutable() {
        List<Path> candidates = new ArrayList<>();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isBlank()) {
            candidates.add(Paths.get(javaHome, "bin", executable("java")));
        }

        Path installRoot = appDir.getParent();
        if (installRoot != null) {
            candidates.add(installRoot.resolve("runtime").resolve("bin").resolve(executable("java")));
        }
        candidates.add(appDir.resolve("jre").resolve("bin").resolve(executable("java")));

        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return Paths.get(executable("java"));
    }

    private void waitForServer() throws Exception {
        long deadline = System.currentTimeMillis() + START_TIMEOUT.toMillis();
        URL url = URI.create("http://127.0.0.1:" + SERVER_PORT + "/api/v1/health").toURL();
        while (System.currentTimeMillis() < deadline) {
            try {
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 500) {
                    appendLog("后端服务已启动。");
                    return;
                }
            } catch (IOException ignored) {
                // Retry until timeout.
            }
            sleep(1000);
        }
        throw new IllegalStateException("后端服务启动超时");
    }

    private void stopServices() {
        setOpenEnabled(false);
        stopServer();
        stopDatabase();
        setStatus("已停止");
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            appendLog("正在停止后端服务...");
            serverProcess.destroy();
            try {
                if (!serverProcess.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
                    serverProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void stopDatabase() {
        Path mariaDbDir = appDir.resolve("mariadb");
        Path mysqlAdmin = mariaDbDir.resolve("bin").resolve(executable("mysqladmin"));
        if (Files.exists(mysqlAdmin)) {
            try {
                runCommandNoThrow(List.of(mysqlAdmin.toString(),
                        "--protocol=tcp",
                        "--host=127.0.0.1",
                        "--port=" + DB_PORT,
                        "--user=root",
                        "shutdown"), mariaDbDir);
            } catch (Exception ignored) {
                // Fall through to process destroy.
            }
        }
        if (databaseProcess != null && databaseProcess.isAlive()) {
            appendLog("正在停止 MariaDB...");
            databaseProcess.destroy();
            try {
                if (!databaseProcess.waitFor(8, java.util.concurrent.TimeUnit.SECONDS)) {
                    databaseProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runMysql(Path mariaDbDir, String database, List<String> extraArgs, Path inputFile)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(mariaDbDir.resolve("bin").resolve(executable("mysql")).toString());
        command.add("--protocol=tcp");
        command.add("--host=127.0.0.1");
        command.add("--port=" + DB_PORT);
        command.add("--user=root");
        command.addAll(extraArgs);
        if (database != null) {
            command.add(database);
        }
        runCommand(command, mariaDbDir, inputFile);
    }

    private void runCommand(List<String> command, Path workDir, Path inputFile) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        if (inputFile != null) {
            builder.redirectInput(inputFile.toFile());
        }
        Process process = builder.start();
        pipeLogs(process, commandLabel(command.get(0)));
        int code = process.waitFor();
        if (code != 0) {
            throw new IllegalStateException("命令执行失败(" + code + "): " + String.join(" ", command));
        }
    }

    private int runCommandNoThrow(List<String> command, Path workDir) {
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor();
        } catch (Exception e) {
            return -1;
        }
    }

    private void pipeLogs(Process process, String label) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog("[" + label + "] " + line);
                }
            } catch (IOException ignored) {
                // Process stream closed.
            }
        }, label + "-log-reader");
        thread.setDaemon(true);
        thread.start();
    }

    private void openBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception e) {
            appendLog("无法自动打开浏览器，请手动访问: " + url);
        }
    }

    private Optional<String> detectLanAddress() {
        List<String> candidates = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return Optional.empty();
            }
            for (NetworkInterface networkInterface : Collections.list(interfaces)) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        candidates.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            appendLog("检测局域网 IP 失败: " + e.getMessage());
        }
        candidates.sort(Comparator.comparingInt(this::addressPriority));
        return candidates.stream().findFirst();
    }

    private int addressPriority(String ip) {
        if (ip.startsWith("172.")) {
            return 0;
        }
        if (ip.startsWith("192.168.")) {
            return 1;
        }
        if (ip.startsWith("10.")) {
            return 2;
        }
        return 3;
    }

    private static Path resolveAppDir() {
        String override = System.getProperty("leon.exam.appDir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        try {
            Path location = Paths.get(LeonExamLauncher.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.getParent().toAbsolutePath().normalize();
            }
            return location.toAbsolutePath().normalize();
        } catch (Exception e) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }

    private static Path resolveDataRoot() {
        String programData = System.getenv("PROGRAMDATA");
        if (programData != null && !programData.isBlank()) {
            return Paths.get(programData, APP_NAME);
        }
        return Paths.get(System.getProperty("user.home"), "AppData", "Local", APP_NAME);
    }

    private static String executable(String name) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win") && !name.endsWith(".exe")) {
            return name + ".exe";
        }
        return name;
    }

    private static String commandLabel(String command) {
        String fileName = Paths.get(command).getFileName().toString();
        int dot = fileName.indexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static void requireFile(Path path, String label) {
        if (!Files.exists(path)) {
            throw new IllegalStateException(label + " 不存在: " + path);
        }
    }

    private void setStatus(String value) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(value));
    }

    private void setUrls(String teacherUrl, String studentUrl) {
        SwingUtilities.invokeLater(() -> {
            teacherUrlLabel.setText(teacherUrl);
            studentUrlLabel.setText(studentUrl);
        });
    }

    private void setOpenEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> openButton.setEnabled(enabled));
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
