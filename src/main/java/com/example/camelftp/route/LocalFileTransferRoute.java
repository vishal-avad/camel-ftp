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

    @Value("${sftp.source.token-file-extension:}")
    private String sourceTokenExtension;

    @Value("${sftp.destination.token-file-extension:}")
    private String destTokenExtension;

    @Value("${sftp.file.pattern}")
    private String filePattern;

    private final TokenFileProcessor tokenFileProcessor;

    public LocalFileTransferRoute(TokenFileProcessor tokenFileProcessor) {
        this.tokenFileProcessor = tokenFileProcessor;
    }

    @Override
    public void configure() throws Exception {
        boolean sourceTokenEnabled = sourceTokenExtension != null && !sourceTokenExtension.isBlank();
        boolean destTokenEnabled = destTokenExtension != null && !destTokenExtension.isBlank();

        // Source: poll local directory for files, optionally requiring a token (done) file
        StringBuilder sourceUriBuilder = new StringBuilder();
        sourceUriBuilder.append(String.format("file://%s?antInclude=%s", localSourcePath, filePattern));
        if (sourceTokenEnabled) {
            sourceUriBuilder.append(String.format("&doneFileName=${file:name}%s", sourceTokenExtension));
        }
        sourceUriBuilder.append("&idempotent=true&noop=true&readLock=changed&readLockMinLength=0&delay=5000");
        String sourceUri = sourceUriBuilder.toString();

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

        var route = from(sourceUri)
            .routeId("local-file-transfer")
            .log(LoggingLevel.INFO, "Picked up file from local source: ${header.CamelFileName}")
            .choice()
                .when(simple("${header.CamelFileLength} == 0"))
                    .log(LoggingLevel.WARN, "File is empty, skipping transfer: ${header.CamelFileName}")
                .otherwise()
                    .to(destUri)
                    .log(LoggingLevel.INFO, "File written to local destination: ${header.CamelFileName}");

        if (destTokenEnabled) {
            route
                    .process(tokenFileProcessor)
                    .to(destTokenUri)
                    .log(LoggingLevel.INFO, "Token file created at local destination: ${header.CamelFileName}");
        }

        route.end();
    }
}

