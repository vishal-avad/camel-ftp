package com.example.camelftp.route;

import com.example.camelftp.config.SftpProperties;
import com.example.camelftp.processor.TokenFileProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Camel route for SFTP file transfer with token file pattern.
 *
 * Behavior:
 * 1. Polls the source SFTP directory for files matching the configured pattern.
 * 2. Only picks up a file when its corresponding token file (e.g., data.csv.done) is present.
 * 3. Uses an idempotent consumer to prevent re-transferring already-transferred files.
 * 4. Writes the data file to the destination SFTP directory.
 * 5. After the data file is fully written, generates a token file at the destination.
 */
@Component
@ConditionalOnProperty(name = "transfer.mode", havingValue = "sftp")
public class SftpFileTransferRoute extends RouteBuilder {

    private final SftpProperties props;
    private final TokenFileProcessor tokenFileProcessor;

    public SftpFileTransferRoute(SftpProperties props, TokenFileProcessor tokenFileProcessor) {
        this.props = props;
        this.tokenFileProcessor = tokenFileProcessor;
    }

    @Override
    public void configure() throws Exception {
        String sourceTokenExt = props.getSource().getTokenFileExtension();
        boolean sourceTokenEnabled = sourceTokenExt != null && !sourceTokenExt.isBlank();
        String destTokenExt = props.getDestination().getTokenFileExtension();
        boolean destTokenEnabled = destTokenExt != null && !destTokenExt.isBlank();
        String filePattern = props.getFile().getPattern();
        String authParams = buildAuthParams();
        String sourcePath = toAbsoluteSftpPath(props.getSource().getPath());
        String destPath = toAbsoluteSftpPath(props.getDestination().getPath());

        // Source SFTP endpoint: poll for files, optionally requiring a token (done) file
        StringBuilder sourceUriBuilder = new StringBuilder();
        sourceUriBuilder.append(String.format(
            "sftp://%s@%s:%d%s?%s&antInclude=%s",
            props.getUsername(), props.getHost(), props.getPort(),
            sourcePath, authParams, filePattern
        ));
        if (sourceTokenEnabled) {
            sourceUriBuilder.append(String.format("&doneFileName=${file:name}%s", sourceTokenExt));
        }
        sourceUriBuilder.append("&idempotent=true&noop=true&readLock=changed&readLockMinLength=0&delay=5000");
        String sourceUri = sourceUriBuilder.toString();

        // Destination SFTP endpoint for data files
        String destUri = String.format(
            "sftp://%s@%s:%d%s?%s&tempFileName=${file:name}.tmp",
            props.getUsername(), props.getHost(), props.getPort(),
            destPath, authParams
        );

        // Destination SFTP endpoint for token files
        String destTokenUri = String.format(
            "sftp://%s@%s:%d%s?%s",
            props.getUsername(), props.getHost(), props.getPort(),
            destPath, authParams
        );

        var route = from(sourceUri)
            .routeId("sftp-file-transfer")
            .log(LoggingLevel.INFO, "Picked up file from source: ${header.CamelFileName}")
            .choice()
                .when(simple("${header.CamelFileLength} == 0"))
                    .log(LoggingLevel.WARN, "File is empty, skipping transfer: ${header.CamelFileName}")
                .otherwise()
                    .to(destUri)
                    .log(LoggingLevel.INFO, "File written to destination: ${header.CamelFileName}");

        if (destTokenEnabled) {
            route
                    .process(tokenFileProcessor)
                    .to(destTokenUri)
                    .log(LoggingLevel.INFO, "Token file created at destination: ${header.CamelFileName}");
        }

        route.end();
    }

    /**
     * Builds the authentication query parameters for the SFTP URI.
     * If a private key file is configured, uses SSH key-based authentication.
     * Otherwise, falls back to password-based authentication.
     */
    /**
     * Converts a configured path to an absolute SFTP URI path.
     * In Camel's SFTP URI, a single slash after the host (e.g., sftp://host:22/path)
     * is treated as relative to the user's home directory. To use an absolute path
     * on the remote server, a double slash is required (e.g., sftp://host:22//path).
     * This method ensures that paths starting with '/' are prefixed with an extra '/'
     * so they are interpreted as absolute.
     */
    private String toAbsoluteSftpPath(String path) {
        if (path != null && path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    private String buildAuthParams() {
        String privateKeyFile = props.getPrivateKeyFile();
        if (privateKeyFile != null && !privateKeyFile.isBlank()) {
            StringBuilder params = new StringBuilder();
            params.append("privateKeyFile=").append(privateKeyFile);
            params.append("&useUserKnownHostsFile=false");
            String passphrase = props.getPrivateKeyPassphrase();
            if (passphrase != null && !passphrase.isBlank()) {
                params.append("&privateKeyPassphrase=").append(passphrase);
            }
            return params.toString();
        }
        return "password=" + props.getPassword();
    }
}

