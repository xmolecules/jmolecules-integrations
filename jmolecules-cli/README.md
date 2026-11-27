# jMolecules CLI (`jm`)

Command-line interface for [jMolecules](https://github.com/xmolecules/jmolecules) – Domain-Driven Design and architecture building blocks for Java projects.

`jm` sets up jMolecules in an existing Maven project and generates DDD building blocks for you:

- **`jm init`** adds the jMolecules BOM to your build and inspects your project (base package, Lombok, Spring Data, Spring Modulith).
- **`jm add-module`** creates a new [Spring Modulith](https://spring.io/projects/spring-modulith) application module.
- **`jm add-aggregate`** creates an aggregate root including its identifier and, if you use Spring Data, a repository and a creation event.
- **`jm config`** shows the configuration `jm` uses for code generation.

## Installation

### macOS

#### Homebrew

```bash
brew install xmolecules/tap/jm
```

Bash completion is installed automatically and works in both bash and zsh.

If you installed a pre-release before, run `brew update` first to refresh your local copy of the tap.

### Linux

Download `jm-VERSION-linux-x86_64.zip` from [GitHub Releases](https://github.com/xmolecules/jmolecules-integrations/releases):

```bash
# Extract archive
unzip jm-VERSION-linux-x86_64.zip -d jm

# Move binary to PATH
sudo mv jm/bin/jm /usr/local/bin/
```

The bash completion file is included in `completion/jm.completion` and works in both bash and zsh. Completing module names for `--module` requires [`jq`](https://jqlang.org).

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

## Quick start

```bash
cd my-project               # a Spring Boot project built with Maven
jm init                     # set up jMolecules
jm add-module orders        # create a Spring Modulith module
jm add-aggregate Order -m orders
```

For a Spring Boot application in `com.example.shop` that uses Spring Data and Spring Modulith, this results in:

```
src/main/java/com/example/shop
├── ShopApplication.java
└── orders
    ├── package-info.java                    # @ApplicationModule
    ├── Orders.java                          # placeholder @Component
    ├── Order.java                           # aggregate root, identifier and creation event
    └── OrderRepository.java                 # Spring Data repository
src/test/java/com/example/shop/orders
├── OrdersIntegrationTest.java               # @ApplicationModuleTest
├── OrderUnitTest.java
└── OrderRepositoryIntegrationTests.java     # @ApplicationModuleTest
```

## Requirements

- A Maven project, i.e. a `pom.xml` in the directory you run `jm` in. Gradle is not supported yet.
- The [Maven Wrapper](https://maven.apache.org/wrapper/) (`mvnw`) in the project directory or one of its parent directories. `jm` runs all Maven invocations through it.
- A single class annotated with `@SpringBootApplication` so that `jm init` can detect your base package.
- For Spring Modulith support: a `java` executable on your `PATH`, as `jm` runs Spring Modulith's module exporter to learn about your application modules.

## Commands

Run `jm --help` for an overview of all commands and `jm --version` to see the installed version.

### `jm init`

Prepares a project for jMolecules and writes the configuration used by the other commands. Run it once in your project's root directory, and again whenever you change the dependencies relevant to code generation.

`jm init`

1. Resolves your project's classpath, effective POM and dependencies via the Maven Wrapper and caches them in `.jm/`.
2. Adds the jMolecules BOM to your `pom.xml`, unless jMolecules artifacts are already managed there. The version is declared as `jmolecules.version` property and defaults to the latest release on Maven Central.
3. Detects your base package from the class annotated with `@SpringBootApplication`.
4. Enables the generator features matching your dependencies: Lombok, Spring Data (`spring-data-commons`) and Spring Modulith (`spring-modulith-core`).
5. If Spring Modulith is used, computes the application module metadata that `jm add-aggregate --module` and the shell completion use.
6. Writes everything to `jmolecules.config`.

```
$ jm init
☕️️ Calculating classpath (./.jm/classpath).
☕️️ Computing effective POM (./.jm/effective-pom.xml).
📦 Adding jMolecules BOM (2025.0.2) to dependencyManagement.
☕️️ Updating Maven dependencies to (./.jm/dependencies).
🍃 No base package configured. Detecting…
🍃 Configuring base package to 'com.example.shop'.
📦 Calculating application module metadata.
⚗️  jMolecules configuration written to jmolecules.config.
```

The changes to your `pom.xml` look like this:

```xml
<properties>
	<jmolecules.version>2025.0.2</jmolecules.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>org.jmolecules</groupId>
			<artifactId>jmolecules-bom</artifactId>
			<version>${jmolecules.version}</version>
			<type>pom</type>
			<scope>import</scope>
		</dependency>
	</dependencies>
</dependencyManagement>
```

| Option | Description |
|---|---|
| `-f`, `--force` | Detect the base package again, even if one is already configured. |
| `-o`, `--offline` | Run Maven in offline mode and use the latest jMolecules BOM version available in your local repository. |
| `--version=<version>` | Use the given jMolecules BOM version instead of the latest release. |

> [!NOTE]
> `jm init` only adds the BOM, not the jMolecules libraries themselves. The code generated by `jm add-aggregate` uses the jMolecules DDD types, so add the corresponding dependency – its version is managed by the BOM:
>
> ```xml
> <dependency>
> 	<groupId>org.jmolecules</groupId>
> 	<artifactId>jmolecules-ddd</artifactId>
> </dependency>
> ```

### `jm add-module <name>`

Creates a new Spring Modulith application module named `<name>` as direct sub-package of your base package and requires Spring Modulith to be used in your project.

```
$ jm add-module orders
```

creates the following files in `com.example.shop.orders`:

| File | Content |
|---|---|
| `package-info.java` | Declares the package as `@ApplicationModule`. |
| `Orders.java` | A placeholder Spring `@Component` to start from. |
| `OrdersIntegrationTest.java` (test) | An integration test for the module annotated with `@ApplicationModuleTest`. |

Afterwards, `jm` compiles your project and refreshes the application module metadata, so that the new module is immediately available to `jm add-aggregate --module` and the shell completion.

### `jm add-aggregate <name>`

Creates an aggregate root named `<name>`, which must be a valid Java type name, together with supporting types.

| Option | Description |
|---|---|
| `-p`, `--package=<package>` | The package to create the aggregate in. Required unless Spring Modulith is enabled. |
| `-m`, `--module=<module>` | The Spring Modulith application module to create the aggregate in. The aggregate is placed in the module's base package. Requires `spring-modulith.enabled=true`. |

```
$ jm add-aggregate Order --module orders
☕️ ️Writing source file ./src/main/java/com/example/shop/orders/Order.java.
☕️ ️Writing source file ./src/test/java/com/example/shop/orders/OrderUnitTest.java.
☕️ ️Writing source file ./src/main/java/com/example/shop/orders/OrderRepository.java.
☕️ ️Writing source file ./src/test/java/com/example/shop/orders/OrderRepositoryIntegrationTests.java.
```

What gets generated depends on your configuration:

| File | Generated | Content |
|---|---|---|
| `Order.java` | always | The aggregate root implementing jMolecules' `AggregateRoot`, with a nested `OrderIdentifier` record wrapping a `UUID`. |
| `OrderUnitTest.java` (test) | always | An empty unit test class. |
| `OrderRepository.java` | Spring Data | A `CrudRepository<Order, Order.OrderIdentifier>`. |
| `OrderRepositoryIntegrationTests.java` (test) | Spring Data | An integration test with the repository injected. Annotated with `@ApplicationModuleTest` if Spring Modulith is enabled, `@SpringBootTest` otherwise. Uses Lombok's `@RequiredArgsConstructor` if Lombok is enabled. |

With Spring Data enabled, the aggregate also extends `AbstractAggregateRoot` and registers an `OrderCreated` event on creation. The event implements jMolecules' `DomainEvent` if `jmolecules-events` is on your classpath:

```java
class Order extends AbstractAggregateRoot<Order> implements AggregateRoot<Order, Order.OrderIdentifier> {

	private final OrderIdentifier id;

	Order() {
		this.id = new OrderIdentifier(UUID.randomUUID());
		registerEvent(new OrderCreated(id));
	}

	@Override
	public OrderIdentifier getId() {
		return id;
	}

	public record OrderIdentifier(UUID id) implements Identifier {

		@Override
		public String toString() {
			return id.toString();
		}
	}

	public record OrderCreated(OrderIdentifier id) implements DomainEvent {}
}
```

### `jm config`

Prints the configuration currently used for code generation.

```
$ jm config
{spring-data.enabled=true, base-package=com.example.shop, spring-modulith.enabled=true}
```

## Configuration

`jm init` writes its findings to `jmolecules.config` in your project's root directory. Commit it alongside your code, and edit it to override what was detected:

| Property | Description |
|---|---|
| `base-package` | The base package of your application. New modules are created as sub-packages of it. |
| `lombok.enabled` | Whether generated code uses Lombok. |
| `spring-data.enabled` | Whether to generate Spring Data repositories and domain events for aggregates. |
| `spring-modulith.enabled` | Whether Spring Modulith is used. Enables `jm add-aggregate --module` and generates `@ApplicationModuleTest` integration tests. |

The `.jm/` directory contains machine-specific data cached by `jm` (resolved classpath, effective POM, dependencies, application module metadata). Add it to your `.gitignore`; `jm init` recreates it.

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

When switching to the Homebrew formula later, remove these files first or run `brew link --overwrite jm` after installing it, as Homebrew refuses to overwrite files it doesn't own.

## License

Apache License 2.0
