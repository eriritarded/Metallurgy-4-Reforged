# Spartan Weaponry

Implemented and compiled2026-10-16. Forge1.20.1 uses native Spartan Weaponry3.2.1/API12. Primary source revision: `ObliviousSpartan/SpartanWeaponry@e45342c53cd2c6fb185d149019fd326e4c1ac696`. The official Modrinth version is `4U6gwadt` (artifact SHA1 `187977cf09972df5773c92b8af5fd342a9d726cb`); it is a compile-only dependency. Original source used Spartan1.5.3/API5 or6. No corresponding official1.21.1/26.1.2 branch was found; those targets do not fabricate the API or register unusable optional weapons.

## Registration and mechanics

`compat/spartan/SpartanIntegration` is entered only with `spartanweaponry` loaded and `GeneralConfig.spartanWeaponryIntegration`. It creates the original39 tool materials and23 weapon forms:897 optional items. The original `form_material` IDs are retained, including `hammer`, `mace`, `staff`, `throwing_axe` and `crossbow`; the corresponding native API types are Battle Hammer, Flanged Mace, Quarterstaff, Tomahawk and Heavy Crossbow. API12 also supports a Scythe, but the original addon did not add that form.

Each material receives the original durability, efficiency, damage, enchantability and RGB from the restart-configured `Materials` definitions, and a native common ingot repair tag. Native API-created items own their archetypes, weapon properties, durability, projectiles, loading, weapon traits, oil behavior, rendering predicates and reload processing. Explicit empty material-trait tags remain valid native extension points for higher-priority packs. No new material powers are invented. The old addon items do not implement Metallurgy's `IMetalItem`/`IToolEffect`, so they did not receive the base armor/tool/weapon effects or Tartarite absorption; that separation is retained.

The integration has its own creative tab. Client registration only adds the old layered material palette; native item constructors already register their own action/loading predicates. Dedicated-server code does not enter the client helper. An optional loader dependency orders initialization after Spartan Weaponry and checks the reviewed3.2.x API range. Disabling the startup integration omits its registry entries, matching the original optional registration policy; remove stored optional items first or restore the setting/dependency if a world contains them.

## Recipes

All897 original recipes are retained individually in `content/spartan-legacy-recipes.json`, including source path and SHA256. `tools/generate_spartan_resources.py` writes native shaped recipes with the same patterns, counts and ingot requirements. Old material metadata0/1 becomes native Handle/Pole; wood sticks, string, planks, logs and vanilla bows use current item/tag ingredients. Every optional recipe checks dependency presence and the integration configuration before decoding its result.

## Pixel art and models

The original addon depended on Spartan's layered custom sprites. The port retains90 byte-identical licensed source PNGs, including the inherited wooden Parrying Dagger layer. It does not substitute a tinted full iron weapon or generate replacement artwork. Source archive identity, Apache2.0 license reference and every retained PNG hash are recorded in `spartan-art-baseline.json`; credits and the license are packaged. No dependency classes are bundled.

Sixty-nine native3.2.1 pose/model definitions are adapted to those layers. Their exact source JSON and Git blobs are recorded in `content/spartan-native-models.json`. Current native parents, action predicates, coating textures/loader, throwing/blocking poses and Longbow/Heavy Crossbow loading states are retained. Only visible sprite layers and the namespace of our model references change. Fixed handles stay untinted, and layers1/2 use the original material RGB. The source crossbow firing state intentionally reuses its loaded sprite.

## Evidence limits

Source/API and recipe/art inventories have been inspected. All three target compilers passed with exact staged/maintained source manifests in `evidence/optional-tools-compile-checkpoint.json`. No recipe decoding, weapon registration, reload, projectile behavior, oil rendering, client poses or gameplay has been observed. No game has been launched; no production archive is installed.
