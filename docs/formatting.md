# Formatting guidelines

Apply these rules to code changes throughout the repository.

- Do not add comments to code.
- Never format generated files or build/cache directories.
- Prefer explicit imports; do not introduce star imports. ktfmt does not expand
  existing star imports, so follow this rule when adding or optimizing imports.

## Kotlin formatting

[The root Gradle configuration](../build.gradle.kts) configures ktfmt with
`kotlinLangStyle()`, a maximum width of 100, and `COMPLETE` trailing comma
management. Use these settings for Kotlin sources and Gradle Kotlin scripts.
[EditorConfig](../.editorconfig) supplies editor settings, including four-space
Kotlin indentation and explicit imports.

Check formatting from the repository root:

```sh
./gradlew ktfmtCheck
```

The configured `check` tasks depend on this check. To apply formatting to the
configured source files and scripts:

```sh
./gradlew ktfmtFormat
```

These tasks exclude build and cache directories through their configured source
inputs. Preserve those exclusions and keep generated files out of formatting
inputs. Review the diff after formatting and keep unrelated formatting changes
out of the task.
