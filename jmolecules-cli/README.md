# jMolecules CLI (`jm`)

Command-line interface for jMolecules - Domain-Driven Design and architecture building blocks for Java projects.

## Installation

### macOS

#### Homebrew

```bash
brew install xmolecules/tap/jm
```

Bash completion is installed automatically and works in both bash and zsh.

### Linux

Download `jm-VERSION-linux-x86_64.zip` from [GitHub Releases](https://github.com/xmolecules/jmolecules-integrations/releases):

```bash
# Extract archive
unzip jm-VERSION-linux-x86_64.zip -d jm

# Move binary to PATH
sudo mv jm/bin/jm /usr/local/bin/
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
../mvnw clean package

# Build native binary
../mvnw -Pdist clean package

# Binary output: target/jm (or target/jm.exe on Windows)
```

### Local install for testing

For end-to-end testing of a locally built `jm`, install the binary directly. Homebrew's API-mode design does not currently support installing a local-only tap formula without cloning `homebrew-core`, so the `target/tap` assembly is reserved for jreleaser's published-tap pipeline.

```bash
# Link the binary onto PATH
ln -sf $(pwd)/target/jm /opt/homebrew/bin/jm

# Install bash completion (works for bash and zsh)
cp target/tap/completion/jm.completion $(brew --prefix)/etc/bash_completion.d/jm
```

## License

Apache License 2.0
