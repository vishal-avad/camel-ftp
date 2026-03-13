package com.example.camelftp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sftp")
public class SftpProperties {

    private Source source = new Source();
    private Destination destination = new Destination();
    private Token token = new Token();
    private FileConfig file = new FileConfig();
    private String host = "localhost";
    private int port = 22;
    private String username = "user";
    private String password = "password";

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public Destination getDestination() { return destination; }
    public void setDestination(Destination destination) { this.destination = destination; }
    public Token getToken() { return token; }
    public void setToken(Token token) { this.token = token; }
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

    public static class Source {
        private String path = "/source";
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class Destination {
        private String path = "/destination";
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class Token {
        private FileExtension file = new FileExtension();
        public FileExtension getFile() { return file; }
        public void setFile(FileExtension file) { this.file = file; }

        public static class FileExtension {
            private String extension = ".done";
            public String getExtension() { return extension; }
            public void setExtension(String extension) { this.extension = extension; }
        }
    }

    public static class FileConfig {
        private String pattern = "*.csv";
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
    }
}

