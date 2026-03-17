# Camel FTP

A Spring Boot application using Apache Camel to perform SFTP (and local) file transfers with an **optional token file pattern**. When configured, files are only picked up when a corresponding token file is present at the source, and a token file is generated at the destination after each successful transfer. When the token file extension is not configured, files are transferred directly without any token file logic.

## Prerequisites

- Java 17 or later
- Maven 3.8+

## Building the Application

```bash
mvn clean package
```

## Running the Application

```bash
java -jar target/camel-ftp-0.0.1-SNAPSHOT.jar
```

Or with Maven:

```bash
mvn spring-boot:run
```

## Configuration

All settings are in `src/main/resources/application.yml`. Properties can also be overridden via environment variables or command-line arguments (e.g., `--sftp.host=myserver.com`).

### Transfer Mode

| Property | Values | Default | Description |
|---|---|---|---|
| `transfer.mode` | `local`, `sftp` | `local` | `local` uses the filesystem for testing; `sftp` connects to a remote SFTP server. |

### File Transfer Settings

| Property | Default | Description |
|---|---|---|
| `sftp.source.path` | `/source` | Source directory to poll for files. |
| `sftp.destination.path` | `/destination` | Destination directory to write files to. |
| `sftp.file.pattern` | `*.csv` | Ant-style glob pattern to filter which files to pick up (e.g., `*.csv`, `*.txt`, `*.xml`). |
| `sftp.token.file.extension` | *(none)* | **Optional.** Extension appended to the data file name to form the token file name (e.g., `.done` makes `data.csv` → `data.csv.done`). If empty or not set, the token file pattern is disabled — files are transferred without requiring a source token and no token is created at the destination. |

### SFTP Connection (used when `transfer.mode=sftp`)

| Property | Default | Description |
|---|---|---|
| `sftp.host` | `localhost` | SFTP server hostname. |
| `sftp.port` | `22` | SFTP server port. |
| `sftp.username` | `user` | SFTP username. |
| `sftp.password` | `password` | SFTP password (used when private key is not set). |

### SSH Key Authentication (optional)

If a private key file is configured, it takes precedence over password authentication.

| Property | Default | Description |
|---|---|---|
| `sftp.private-key-file` | *(none)* | Absolute path to the SSH private key file (e.g., `/home/user/.ssh/id_rsa`). |
| `sftp.private-key-passphrase` | *(none)* | Passphrase for the private key, if the key is passphrase-protected. |

### Local Mode Directories (used when `transfer.mode=local`)

| Property | Default | Description |
|---|---|---|
| `local.source.path` | `${user.dir}/data/source` | Local source directory to poll. |
| `local.destination.path` | `${user.dir}/data/destination` | Local destination directory to write files to. |

## Token File Pattern (Optional)

The token file pattern is **optional** and controlled by the `sftp.token.file.extension` property.

### When token file extension is configured (e.g., `.done`)

**Source side** — A file is only picked up when its corresponding token file exists. For example, to transfer `report.csv`, the source directory must contain both:
```
report.csv
report.csv.done
```

**Destination side** — After the data file is fully written to the destination, a token file is created to signal completion:
```
report.csv          ← data file
report.csv.done     ← token file (created after write completes)
```

### When token file extension is not configured

Files are picked up from the source directory as soon as they match the file pattern — no token file is required at the source, and no token file is created at the destination.

### General behavior

**Idempotent consumer** — Files that have already been transferred are tracked and will not be transferred again, even if their token files remain in the source directory.

**Empty files** — If a picked-up data file is empty (0 bytes), a warning is logged and the transfer is skipped.

## Testing Locally

1. Build the application:
   ```bash
   mvn clean package
   ```

2. Ensure `transfer.mode` is set to `local` in `application.yml` (this is the default).

3. Create the source directory and place a data file:
   ```bash
   mkdir -p data/source data/destination
   echo "id,name,value" > data/source/sample.csv
   ```
   If the token file extension is configured (e.g., `.done`), also create the token file:
   ```bash
   echo "" > data/source/sample.csv.done
   ```

4. Start the application:
   ```bash
   java -jar target/camel-ftp-0.0.1-SNAPSHOT.jar
   ```
   To run without the token file pattern, override the extension to empty:
   ```bash
   java -jar target/camel-ftp-0.0.1-SNAPSHOT.jar --sftp.token.file.extension=""
   ```

5. **With token enabled**, the logs will show:
   ```
   INFO  ... Picked up file from local source: sample.csv
   INFO  ... File written to local destination: sample.csv
   INFO  ... Token file will be created: sample.csv.done
   INFO  ... Token file created at local destination: sample.csv.done
   ```
   The destination directory will contain both `sample.csv` and `sample.csv.done`.

   **With token disabled**, the logs will show:
   ```
   INFO  ... Picked up file from local source: sample.csv
   INFO  ... File written to local destination: sample.csv
   ```
   The destination directory will contain only `sample.csv`.

## Project Structure

```
src/main/java/com/example/camelftp/
├── CamelFtpApplication.java              # Spring Boot entry point
├── config/
│   └── SftpProperties.java               # Typed configuration properties
├── processor/
│   └── TokenFileProcessor.java           # Creates token file after transfer
└── route/
    ├── LocalFileTransferRoute.java        # File-based route for local testing
    └── SftpFileTransferRoute.java         # SFTP-based route for production
```
