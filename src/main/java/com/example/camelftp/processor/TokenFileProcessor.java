package com.example.camelftp.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Processor that creates a token (done) file header for the exchange.
 * The token file name is derived from the transferred file name
 * (e.g., data.csv -> data.csv.done).
 */
@Component
public class TokenFileProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(TokenFileProcessor.class);

    @Value("${sftp.token.file.extension:}")
    private String tokenExtension;

    @Override
    public void process(Exchange exchange) throws Exception {
        String originalFileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
        String tokenFileName = originalFileName + tokenExtension;
        exchange.getIn().setHeader(Exchange.FILE_NAME, tokenFileName);
        // Token file contains empty content — it is just a signal file
        exchange.getIn().setBody("");
        LOG.info("Token file will be created: {}", tokenFileName);
    }
}

