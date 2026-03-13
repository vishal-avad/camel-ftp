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
        String tokenExt = props.getToken().getFile().getExtension();
        String filePattern = props.getFile().getPattern();

        // Source SFTP endpoint: poll for files that have a matching token (done) file
        String sourceUri = String.format(
            "sftp://%s@%s:%d%s"
                + "?password=%s"
                + "&antInclude=%s"
                + "&doneFileName=${file:name}%s"
                + "&idempotent=true"
                + "&noop=true"
                + "&readLock=changed"
                + "&delay=5000",
            props.getUsername(),
            props.getHost(),
            props.getPort(),
            props.getSource().getPath(),
            props.getPassword(),
            filePattern,
            tokenExt
        );

        // Destination SFTP endpoint for data files
        String destUri = String.format(
            "sftp://%s@%s:%d%s"
                + "?password=%s"
                + "&tempFileName=${file:name}.tmp",
            props.getUsername(),
            props.getHost(),
            props.getPort(),
            props.getDestination().getPath(),
            props.getPassword()
        );

        // Destination SFTP endpoint for token files
        String destTokenUri = String.format(
            "sftp://%s@%s:%d%s"
                + "?password=%s",
            props.getUsername(),
            props.getHost(),
            props.getPort(),
            props.getDestination().getPath(),
            props.getPassword()
        );

        from(sourceUri)
            .routeId("sftp-file-transfer")
            .log(LoggingLevel.INFO, "Picked up file from source: ${header.CamelFileName}")
            .to(destUri)
            .log(LoggingLevel.INFO, "File written to destination: ${header.CamelFileName}")
            .process(tokenFileProcessor)
            .to(destTokenUri)
            .log(LoggingLevel.INFO, "Token file created at destination: ${header.CamelFileName}");
    }
}

