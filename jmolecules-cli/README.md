# jMolecules CLI (`jm`)

Command-line interface for jMolecules - Domain-Driven Design and architecture building blocks for Java projects.

## Installation

### macOS

#### Homebrew

```bash
brew install xmolecules/jm
```

Bash completion is installed automatically and works in both bash and zsh.

### Linux

Download the appropriate binary for your architecture from [GitHub Releases](https://github.com/xmolecules/jmolecules-integrations/releases):

- **x86_64**: `jm-VERSION-linux-x86_64.tar.gz`
- **ARM64**: `jm-VERSION-linux-aarch64.tar.gz`

```bash
# Extract archive
tar -xzf jm-VERSION-linux-x86_64.tar.gz

# Move binary to PATH
sudo mv bin/jm /usr/local/bin/
```

The bash completion file is included in `completion/jm.completion` and works in both bash and zsh.

### Windows

#### Scoop

```powershell
scoop bucket add xmolecules https://github.com/xmolecules/scoop-bucket
scoop install jm
```

#### Manual Installation

Download `jm-VERSION-windows-x86_64.zip` from [GitHub Releases](https://github.com/xmolecules/jmolecules-integrations/releases):

1. Extract the archive
2. Add the `bin` directory to your PATH

### Manual Download

Download pre-built binaries for all platforms from the [Releases page](https://github.com/xmolecules/jmolecules-integrations/releases).

## Usage

```bash
# Initialize jMolecules in your project
jm init

# Add a DDD aggregate
jm add-aggregate <name> --package <package>

# Add a Spring Modulith module
jm add-module --name <name>

# View configuration
jm config

# Show help
jm --help
```

## Building from Source

Requires:
- JDK 17+
- GraalVM (for native image compilation)

```bash
# Build JAR
./mvnw clean package

# Build native binary
./mvnw -Pdist clean package

# Binary output: target/jm (or target/jm.exe on Windows)
```

## License

Apache License 2.0
