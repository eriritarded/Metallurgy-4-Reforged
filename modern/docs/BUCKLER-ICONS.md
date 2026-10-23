# Buckler inventory size

The user clarified that the three buckler inventory icons should be slightly larger. `zinc_buckler`, `vulcanite_buckler` and `orichalcum_buckler` now have GUI scale `[1.08,1.08,1.08]`, an8% uniform increase from their authored implicit1.0.

This applies to the12 root/gadget normal/blocking model variants so native selectors stay consistent across all three targets. `tools/generate_gadget_resources.py::enlarge_buckler_icons` preserves it during regeneration. Original Matpac geometry, UVs, PNGs, hand/ground transforms, shield mechanics, durations, cooldowns and item IDs are retained. The two full-size shields are not resized.

Packaged resources can establish the exact transform; their appearance and edge spacing have not been inspected in a newly launched game. Build/package/install state is recorded with the optional handbook release in `evidence/handbook-release.json`.
