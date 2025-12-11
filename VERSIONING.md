# Versioning with Maven Release Plugin

This project uses [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/) for automatic version management based on Git tags.

## Version Format

- **Development versions**: `0.1.0-SNAPSHOT` (current)
- **Tagged release**: `0.1.0` (based on git tag `v0.1.0`)
- **Next development**: `0.2.0-SNAPSHOT` (after release)

## How It Works

The Maven Release Plugin automatically:
- Updates version numbers in all build files (pom.xml, build.gradle, build.sbt)
- Creates Git tags for releases
- Updates to next development version after release
- Generates release notes and changelogs

## Release Process

### Automated Release (Recommended)

1. Ensure all changes are committed and pushed:
   ```bash
   git add .
   git commit -m "Prepare for release"
   git push origin main
   ```

2. Run the release prepare command:
   ```bash
   mvn release:prepare
   ```
   This will:
   - Ask for release version (e.g., `0.1.0`)
   - Ask for next development version (e.g., `0.2.0-SNAPSHOT`)
   - Update all version files
   - Create Git tag

3. Perform the release:
   ```bash
   mvn release:perform
   ```
   This will deploy artifacts to repository

### Manual Release Steps

If you need more control, you can do it manually:

1. Create and push a version tag:
   ```bash
   git tag -a v0.1.0 -m "Release version 0.1.0"
   git push origin v0.1.0
   ```

2. Update versions manually in all build files:
   - pom.xml: `<version>0.1.0</version>`
   - build.gradle: `version = '0.1.0'`
   - build.sbt: `version := "0.1.0"`

## Checking Current Version

The current version is defined in:
- `pom.xml` (primary)
- `build.gradle`
- `build.sbt`

Run `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` to check current version.

## Configuration

The release plugin is configured in `pom.xml` with:
- SCM information pointing to the correct repository
- Release profile that includes javadoc and sources
- Automatic version management across submodules

## Notes

- Always use SNAPSHOT versions during development
- Release versions should not have -SNAPSHOT suffix
- The plugin automatically handles version increments
- Make sure your Git repository is clean before running release commands
