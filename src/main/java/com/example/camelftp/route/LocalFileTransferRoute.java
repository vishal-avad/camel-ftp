package com.example.camelftp.route;

import com.example.camelftp.processor.TokenFileProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Camel route for local file transfer — used for testing without an SFTP server.
 *
 * Uses the Camel file component to transfer files between local directories,
 * following the same token file pattern as the SFTP route:
 * 1. Polls the local source directory for files matching the configured pattern.
 * 2. Only picks up a file when its corresponding token file is present at the source.
 * 3. Uses an idempotent consumer to prevent re-transferring already-transferred files.
 * 4. Writes the data file to the local destination directory.
 * 5. After the data file is fully written, generates a token file at the destination.
 */
@Component
@ConditionalOnProperty(name = "transfer.mode", havingValue = "local", matchIfMissing = true)
public class LocalFileTransferRoute extends RouteBuilder {

    @Value("${local.source.path}")
    private String localSourcePath;

    @Value("${local.destination.path}")
    private String localDestinationPath;

    @Value("${sftp.token.file.extension}")
    private String tokenExtension;

    @Value("${sftp.file.pattern}")
    private String filePattern;

    private final TokenFileProcessor tokenFileProcessor;

    public LocalFileTransferRoute(TokenFileProcessor tokenFileProcessor) {
        this.tokenFileProcessor = tokenFileProcessor;
    }

    @Override
    public void configure() throws Exception {

        // Source: poll local directory for files with a matching token (done) file
        String sourceUri = String.format(
            "file://%s"
                + "?antInclude=%s"
                + "&doneFileName=${file:name}%s"
                + "&idempotent=true"
                + "&noop=true"
                + "&readLock=changed"
                + "&delay=5000",
            localSourcePath,
            filePattern,
            tokenExtension
        );

        // Destination: write data file using temp file to ensure atomicity
        String destUri = String.format(
            "file://%s?tempFileName=${file:name}.tmp",
            localDestinationPath
        );

        // Destination: write token file
        String destTokenUri = String.format(
            "file://%s",
            localDestinationPath
        );

        from(sourceUri)
            .routeId("local-file-transfer")
            .log(LoggingLevel.INFO, "Picked up file from local source: ${header.CamelFileName}")
            .to(destUri)
            .log(LoggingLevel.INFO, "File written to local destination: ${header.CamelFileName}")
            .process(tokenFileProcessor)
            .to(destTokenUri)
            .log(LoggingLevel.INFO, "Token file created at local destination: ${header.CamelFileName}");
    }
}

