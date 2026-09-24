# Release Instructions

This project releases through the GitHub Actions workflow in `.github/workflows/release.yml`.
You do not need to run the Maven release commands manually. Create the correct release branch and push it; the workflow handles the rest.

## Before You Start

Make sure:

- You have permission to push branches and tags.
- The build is green before releasing.
- The root `pom.xml` version ends with `-SNAPSHOT`.
  - Example: `1.15.0-SNAPSHOT`
- The required GitHub secrets are configured:
  - `CI_DEPLOY_USERNAME`, `CI_DEPLOY_PASSWORD`
  - `CI_GPG_PRIVATE_KEY`, `CI_GPG_PASSPHRASE`
  - `GH_TOKEN`
  - `DOCKER_PASSWORD`

Check the current project version:

```bash
./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout
```

The release version is the current Maven version without `-SNAPSHOT`.
The release branch type controls the next development version only.

Example:

```text
Current version: 1.15.0-SNAPSHOT
Release version: 1.15.0
Release tag:     v1.15.0
```

## How to Release

Start from the latest release-ready branch, usually `main`:

```bash
git checkout main
git pull origin main
```

Then create one of the supported release branches below and push it.

## Major Release Example

Use this when releasing breaking changes or a new major version line.

If the current version is `1.15.0-SNAPSHOT`:

- The workflow releases `1.15.0`.
- The workflow prepares the next development version as `2.0.0-SNAPSHOT`.

Create and push the branch:

```bash
git checkout -b release/major
git push origin release/major
```

## Minor Release Example

Use this when releasing normal new features.

If the current version is `1.15.0-SNAPSHOT`:

- The workflow releases `1.15.0`.
- The workflow prepares the next development version as `1.16.0-SNAPSHOT`.

Create and push the branch:

```bash
git checkout -b release/minor
git push origin release/minor
```

## Patch Release Example

Use this when releasing a bug fix for the current minor version.

If the current version is `1.15.0-SNAPSHOT`:

- The workflow releases `1.15.0`.
- The workflow prepares the next development version as `1.15.1-SNAPSHOT`.

Create and push the branch:

```bash
git checkout -b release/patch
git push origin release/patch
```


## What the Workflow Publishes

After the branch is pushed, `.github/workflows/release.yml` will:

- Build and publish Maven artifacts to Maven Central.
- Create the Git tag, for example `v1.15.0`.
- Build the distro ZIP needed for Docker.
- Build and publish the Docker Hub image:
  - `finos/fluxnova-bpm-platform:<release-version>`
  - `finos/fluxnova-bpm-platform:latest`
- Publish Javadocs to GitHub Pages under `javadocs/<major>.<minor>`.
- Publish OpenAPI docs to GitHub Pages under `openapi/<major>.<minor>`.
- Notify `finos/fluxnova-examples` about the new release version.

## After the Release

Verify:

- The GitHub Actions workflow completed successfully.
- Log in to Sonatype to verify artifacts and click the publish button to publish to Maven Central. Note: Ask FINOS admin for Sonatype credentials.
- The Maven artifacts are available in Maven Central.
- The Git tag exists, for example `v1.15.0`.
- The Docker Hub image exists with the release version and `latest` tags.
- Javadocs and OpenAPI docs were published.
- The root `pom.xml` was moved to the expected next `-SNAPSHOT` version.

## Notes

- Use `release/major`, `release/minor`, or `release/patch` exactly as shown above.
- Releases up to `3.0.0` supported `hotfix/*` branches, but the current `.github/workflows/release.yml` version calculation only supports the `release/*` branch names listed above.
- For exact workflow steps, see `.github/workflows/release.yml`.
