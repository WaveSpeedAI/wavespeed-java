# Versioning with Maven Release

This project uses Maven release management with Git tags for version control, similar to Python setuptools_scm and JavaScript npm version approaches.

## How it works

- Version numbers are manually managed in `pom.xml` and `build.gradle`
- Versions are based on Git tags (e.g., `v1.0.0`)
- No automatic version bumping based on commits
- Releases are triggered by creating and pushing Git tags

## Version Format

- **Semantic versioning**: `MAJOR.MINOR.PATCH` (e.g., `1.0.0`)
- **Git tags**: `vMAJOR.MINOR.PATCH` (e.g., `v1.0.0`)
- **Development versions**: `MAJOR.MINOR.PATCH-SNAPSHOT` (e.g., `1.0.0-SNAPSHOT`)

## Creating a Release

### 1. Development Phase
Developers work normally, committing changes to the repository.

### 2. Version Update (Manual)
When ready to release, update the version in both `pom.xml` and `build.gradle`:

```xml
<!-- pom.xml -->
<version>1.0.0</version>
```

```gradle
// build.gradle
version = '1.0.0'
```

### 3. Create Git Tag
```bash
# Create annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push tag to trigger release
git push origin v1.0.0
```

### 4. Automated Release
GitHub Actions detects the new tag and automatically:
- Builds the project with Maven
- Runs tests
- Publishes to Maven Central
- Creates a GitHub release

### 5. Update to Next Development Version
After release, update versions to next development version:
```xml
<!-- pom.xml -->
<version>1.1.0-SNAPSHOT</version>
```

```gradle
// build.gradle
version = '1.1.0-SNAPSHOT'
```

## Release Workflow

```bash
# 1. Ensure all changes are committed
git add .
git commit -m "feat: add new feature"

# 2. Update version numbers in pom.xml and build.gradle
# Edit version from "1.0.0-SNAPSHOT" to "1.0.0"

# 3. Commit version changes
git add pom.xml build.gradle
git commit -m "chore: release version 1.0.0"

# 4. Create and push tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main
git push origin --tags

# 5. GitHub Actions automatically publishes

# 6. Update to next development version
# Edit version from "1.0.0" to "1.1.0-SNAPSHOT"
git add pom.xml build.gradle
git commit -m "chore: prepare for next development iteration"
```

## Version Types

| Version Type | When to use | Example |
|-------------|-------------|---------|
| `MAJOR` | Breaking changes | `2.0.0` |
| `MINOR` | New features (backwards compatible) | `1.1.0` |
| `PATCH` | Bug fixes | `1.0.1` |
| `SNAPSHOT` | Development version | `1.1.0-SNAPSHOT` |

## Checking Current Version

```bash
# From pom.xml
grep -A 1 "<version>" pom.xml

# From build.gradle
grep "version =" build.gradle

# Check latest Git tag
git describe --tags --abbrev=0
```

## GitHub Actions Workflow

The release process is automated when tags are pushed:

```yaml
name: Release
on:
  push:
    tags:
      - 'v*.*.*'
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
      - run: mvn clean package
      - run: mvn test
      - run: mvn deploy  # Publish to Maven Central
      - run: Create GitHub release
```

## Maven Central Publishing

The project is configured to publish to Maven Central. Required secrets:
- `MAVEN_USERNAME`: Sonatype OSSRH username
- `MAVEN_PASSWORD`: Sonatype OSSRH password
- `MAVEN_GPG_PASSPHRASE`: GPG key passphrase

## Best Practices

### When to Release
- **Patch releases**: Bug fixes, documentation updates, small improvements
- **Minor releases**: New features that are backwards compatible
- **Major releases**: Breaking changes, API modifications

### Version Planning
- Plan version bumps during development
- Test thoroughly before releasing
- Consider deprecation warnings for breaking changes
- Keep development versions as SNAPSHOT

### Commit Messages
While not strictly enforced, descriptive commit messages are recommended:

```bash
feat: add support for custom timeout options
fix: resolve issue with prediction polling
docs: update API reference documentation
refactor: simplify error handling logic
```

## Comparison with Other Projects

| Aspect | Java (Maven) | Python (setuptools_scm) | JavaScript (npm version) |
|--------|-------------|----------------------|-------------------------|
| Version source | pom.xml/build.gradle | Git tags | package.json |
| Version format | 1.2.3 | 1.2.3.dev4+g1234567 | 1.2.3 |
| Trigger | Git tag | Git tag | npm version + Git tag |
| Automation | GitHub Actions | Build-time | GitHub Actions |
| Development versions | SNAPSHOT | Automatic dev versions | No dev versions |

This approach provides consistent manual version control across all three projects while leveraging each ecosystem's native tooling.