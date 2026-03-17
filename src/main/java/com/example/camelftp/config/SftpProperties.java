package com.example.camelftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    private Source source = new Source();
    private Destination destination = new Destination();
    private FileConfig file = new FileConfig();
    private String host = "localhost";
    private int port = 22;
    private String username = "user";
    private String password = "password";
    private String privateKeyFile;
    private String privateKeyPassphrase;

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
    public FileConfig getFile() { return file; }
    public void setFile(FileConfig file) { this.file = file; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPrivateKeyFile() { return privateKeyFile; }
    public void setPrivateKeyFile(String privateKeyFile) { this.privateKeyFile = privateKeyFile; }
    public String getPrivateKeyPassphrase() { return privateKeyPassphrase; }
    public void setPrivateKeyPassphrase(String privateKeyPassphrase) { this.privateKeyPassphrase = privateKeyPassphrase; }

    public static class Source {
        private String path = "/source";
        private String tokenFileExtension;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getTokenFileExtension() { return tokenFileExtension; }
        public void setTokenFileExtension(String tokenFileExtension) { this.tokenFileExtension = tokenFileExtension; }
    }

    public static class Destination {
        private String path = "/destination";
        private String tokenFileExtension;
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getTokenFileExtension() { return tokenFileExtension; }
        public void setTokenFileExtension(String tokenFileExtension) { this.tokenFileExtension = tokenFileExtension; }
    }

    public static class FileConfig {
        private String pattern = "*.csv";
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
    }
}

