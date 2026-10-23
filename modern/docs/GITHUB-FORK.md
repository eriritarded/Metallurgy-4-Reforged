# Upstream fork and Claude source synchronization

## Reviewed 2026-10-23

The source was already preserved in the Terraria collection's Git history; that
repository has no GitHub remote. The authenticated GitHub account is
`eriritarded`. A real public fork was created from
`Davoleo/Metallurgy-4-Reforged` at upstream master
`ef94cb94cfc08ccebe8a0df307ce6b390dcd4704`:

<https://github.com/eriritarded/Metallurgy-4-Reforged>

The maintained standalone clone is
`H:/AI_MC_Modding/Metallurgy-4-Reforged`, with `origin` pointing to that fork and
`upstream` pointing to Davoleo's repository. The branch `codex/modern-ports`
preserves upstream history and carries the three-target project under `modern/`.
The original legacy root is retained. A later local publication receipt records
the exact committed snapshot and pushed branch identity; creating the fork alone
does not establish that the source has been uploaded.

## Changes since the gauntlet tool update

Claude's source commit `799d371ef033e28874bb5a4b1698d1460d62499d` adds optional
Hephaestus 1.0.17 / Hephaestus Tools 1.0.8 integration on NeoForge 26.1.2:
35 tool materials, 14 added alloys, melting/casting, Thermite fuel, native recipe
precedence, configured stats and the existing Tinkers configuration switches.
See [the exact adaptation](HEPHAESTUS.md). Claude's
`ce58d84b4ee66c18afef19cfa77638ef410a14ef` records its subsequent installation.
The iron pickaxe/shovel gauntlet changes remain in all three maintained targets.

On 2026-10-23, the installed
`Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.17.jar` and retained
`Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.17-hephaestus.jar` both hash to
`a2955f2165cc5591ff5034112c5695ff2651ef38c317f73247d4eb41118f9ddc`.
This is a different package from the earlier gauntlet-only port.17, whose hash is
`a8acf156c9acdd6b5a762cbbf4d785e524fac4d4e2594318c7a9a398b5f63f42`.
The old package/tag is preserved. No retroactive clean-build or runtime claim is
made for Claude's binary. Future changed game content must use a new version.

## Publication and future source work

`tools/export_github_source.py` exports a committed, clean project snapshot into
the initially empty fork's `modern/` directory. It preserves all runtime sources,
resources, build inputs, generators and maintained verification sources. It
omits local forensic receipts and machine instructions, anonymizes home paths in
historical helpers/documentation and records the exact export transformation.
The public fork includes fresh portable instructions instead. No unrelated
Terraria project, user saves, downloaded mod JARs, launcher state, credentials or
release binary is part of this publication.

Continue public development on `codex/modern-ports` in that standalone clone.
The collection copy and its existing history remain intact; they are not a
second GitHub remote or an automatically synchronized working tree. Do not edit
both copies independently without reconciling them. The initial source export
does not publish the collection repository's other projects or history.

No new build, test, game launch, installation, loader edit or upstream pull
request is part of this source synchronization. Compilation/gameplay limits in
the existing records remain. Source publication does not assert full gameplay
parity or validate previously untested integrations.
