# Metallurgy upstream fork

- Active modern development lives in `modern/` on `codex/modern-ports`.
  The root Java/Gradle project is the preserved Minecraft 1.12 upstream.
- Read `modern/docs/GITHUB-FORK.md`, `modern/SOURCE-PROVENANCE.md` and the
  relevant system document before changing an implementation.
- Preserve GPL-3.0 and third-party notices, original pixel art, stable IDs and
  loader/version boundaries. Use the Minecraft skill when available.
- Commit logical changes with their documentation. Bump the appropriate mod
  version for changed game content, and record full source commit and SHA-256
  when delivering a binary. Keep old immutable packages/tags distinct.
- No game launch, test, install, enabled-mod change or launcher edit is implied
  by source/Git work. Never let an assistant-launched game take desktop focus.
- Keep generated binaries, caches, raw logs, worlds, user profiles, secrets and
  dependency implementation JARs out of Git. Gradle wrapper JARs are required
  build inputs. Keep disposable work outside the checkout and clean it up.
- Compilation, packaging, installation and confirmed runtime are separate
  states. Imported historical evidence does not validate a later source change.
- Do not overwrite unrelated work, rewrite existing history, push to upstream,
  open upstream PRs or create binary releases without the user's request.
