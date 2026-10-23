"""Generate maintained optional-mod data, without Gradle, Java, or game launches.

Tetra 6.x and Silent Gear 3.6/4.2 have different loaders and stat systems.
These are separate adapters, not renamed Tinkers materials. See MODULAR-COMPAT.md.
All outputs are maintained source resources; no scratch files are created.
"""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
TARGETS = ('forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2')
VANILLA_TIERS = ('wood', 'stone', 'iron', 'diamond', 'netherite')

# Native effect IDs/levels. Effects keep the receiving mod's own conditions,
# caps, stacking, durability handling and UI. No base-item-ID effect spoofing.
# The combinations intentionally adapt material themes instead of attaching
# full Metallurgy armor-set powers to a cheap rod or tip upgrade.
PROFILES = {
    'adamantine': ({'crushing': 2, 'unbreaking': 1}, [('adamant', 1), ('hard', 2)]),
    'amordrine': ({'criticalStrike': [8, 1.35], 'intuit': 1}, [('holy', 2), ('renew', 1)]),
    'angmallen': ({'criticalStrike': [10, 1.5], 'intuit': 1}, [('lucky', 1), ('lustrous', 1)]),
    'astral_silver': ({'intuit': 2, 'stabilizing': 15}, [('lustrous', 2), ('refractive', 1)]),
    'atlarus': ({'armorPenetration': 15, 'crushing': 1}, [('mighty', 2), ('sturdy', 1)]),
    'black_steel': ({'howling': [1, 1.2], 'unbreaking': 1}, [('bastion', 1), ('sturdy', 1)]),
    'brass': ({'stabilizing': 5}, [('malleable', 2)]),
    'bronze': ({'unbreaking': 1}, [('sturdy', 1)]),
    'carmot': ({'extraction': 1}, [('greedy', 2), ('lucky', 1)]),
    'celenegil': ({'quickStrike': 2, 'intuit': 2}, [('mighty', 2), ('flexible', 1)]),
    'ceruclase': ({'earthbind': 1}, [('chilled', 2), ('brittle', 1)]),
    'copper': ({'workable': 1}, [('malleable', 1)]),
    'damascus_steel': ({'bleeding': 1, 'intuit': 1}, [('sharp', 1), ('lustrous', 1)]),
    'deep_iron': ({'unbreaking': 1, 'stabilizing': 5}, [('aquatic', 1), ('swift_swim', 1)]),
    'desichalkos': ({'howling': [1, 2], 'armorPenetration': 8}, [('bastion', 2), ('sturdy', 2)]),
    'electrum': ({'quickStrike': 2, 'criticalStrike': [6, 1.5]}, [('mighty', 1), ('magnetic', 1)]),
    'etherium': ({'reaching': 1, 'quickStrike': 2}, [('reach', 2), ('light', 2)]),
    'eximite': ({'enderReverb': [1, 0.03], 'intuit': 2}, [('reach', 1), ('synergistic', 1)]),
    'haderoth': ({'unbreaking': 3, 'stabilizing': 20}, [('renew', 2), ('brittle', 1)]),
    'hepatizon': ({'backstab': 1}, [('kitty_vision', 1), ('sharp', 1)]),
    'ignatius': ({'combusting': [1, 5]}, [('fiery', 1), ('magmatic', 1), ('fireproof', 1)]),
    'inolashite': ({'reaching': 2, 'intuit': 1}, [('reach', 2), ('flexible', 1)]),
    'kalendrite': ({'unbreaking': 1, 'intuit': 1}, [('renew', 1), ('malleable', 1)]),
    'krik': ({'quickStrike': 2, 'reaching': 1}, [('light', 2), ('reach', 1)]),
    'lutetium': ({}, [('malleable', 1), ('light', 1)]),
    'midasium': ({'criticalStrike': [8, 1.5], 'stabilizing': 10}, [('greedy', 2), ('lucky', 2)]),
    'mithril': ({'intuit': 3, 'stabilizing': 20}, [('synergistic', 2), ('sharp', 1)]),
    'orichalcum': ({'armorPenetration': 10, 'unbreaking': 1}, [('renew', 1), ('sharp', 2)]),
    'osmium': ({}, [('bastion', 2), ('sturdy', 2)]),
    'oureclase': ({'crushing': 1}, [('hard', 2), ('sturdy', 1)]),
    'platinum': ({'unbreaking': 3}, [('indestructible', 1)]),
    'prometheum': ({'unbreaking': 1, 'workable': 1}, [('renew', 1), ('organic', 1)]),
    'quicksilver': ({'quickStrike': 3}, [('light', 2), ('mighty', 1)]),
    'sanguinite': ({'bleeding': 2, 'bloodbound': [10, 3]}, [('jagged', 2), ('renew', 1)]),
    'shadow_iron': ({'backstab': 1, 'intuit': 1}, [('kitty_vision', 1), ('lustrous', 1)]),
    'shadow_steel': ({'backstab': 2, 'quickStrike': 1}, [('kitty_vision', 1), ('light', 2)]),
    'silver': ({'armorPenetration': 5}, [('holy', 2)]),
    'steel': ({'unbreaking': 1, 'stabilizing': 5}, [('hard', 1), ('sturdy', 1)]),
    'tartarite': ({'armorPenetration': 20, 'unbreaking': 2, 'intuit': 2}, [('synergistic', 3), ('sturdy', 2)]),
    'vulcanite': ({'combusting': [2, 8], 'crushing': 1}, [('fiery', 2), ('magmatic', 1)]),
    'vyroxeres': ({'bleeding': 1, 'armorPenetration': 8}, [('venom', 2)]),
}


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def tier_id(level):
    return 'minecraft:' + VANILLA_TIERS[level] if level <= 4 else 'metallurgy:level_' + str(level)


def traits(metal, modern, subordinate=False):
    result = []
    for name, level in PROFILES[metal['name']][1]:
        # Platinum's durability behavior belongs on the main material only.
        # A single platinum rod must not make any assembled tool indestructible.
        if name == 'indestructible' and subordinate:
            continue
        entry = {'trait' if modern else 'name': 'silentgear:' + name,
                 'level': max(1, level - 1) if subordinate else level}
        # The native condition counts trait-bearing components. A token amount
        # in a mixed main part must not unlock the full rare-material trait.
        entry['conditions'] = [{'type': 'silentgear:material_ratio',
                                'ratio': 0.75 if name == 'indestructible' else 0.5}]
        result.append(entry)
    return result


def main_stats(metal, modern):
    tool, armor = metal.get('tool_stats'), metal.get('armor_stats')
    result = {}
    if tool:
        result.update(durability=tool['durability'], harvest_speed=tool['efficiency'],
                      enchantment_value=tool['enchantability'],
                      **{'attack_damage' if modern else 'melee_damage': tool['damage']})
    if armor:
        result['armor_durability'] = armor['durability']
        result['armor'] = sum(armor['damage_reduction'])
        for index, form in enumerate(('boots', 'leggings', 'chestplate', 'helmet')):
            result['armor/' + form] = armor['damage_reduction'][index]
        result['armor_toughness'] = armor.get('toughness', 0)
        if not tool:
            result['enchantment_value'] = armor['enchantability']
    # Ranged performance has no original metal statistic. Use a neutral native
    # multiplier instead of pretending melee damage was bow/projectile damage.
    result.update(attack_speed=0, ranged_damage=1, projectile_speed=1,
                  projectile_accuracy=1, rarity=20)
    result['draw_speed' if modern else 'ranged_speed'] = 0
    result['charging_value'] = 1
    if modern:
        result['traits'] = traits(metal, True)
        if tool:
            result['harvest_tier'] = harvest_tier(tool['harvest_level'], metal['name'])
    return result


def harvest_tier(level, name):
    return {'name': name.replace('_', ' ').title(), 'level_hint': str(level),
            'incorrect_blocks_for_tool': 'metallurgy:incorrect_for_level_' + str(level)}


def modifier(value, modern):
    return {'operation': 'ADD', 'value': round(value, 3)} if modern else {'add': round(value, 3)}


def part_stats(metal, modern):
    stats = {'main': main_stats(metal, modern)}
    tool = metal.get('tool_stats')
    if tool:
        # These are native part bonuses, not another complete tool head.
        stats['rod'] = {'durability': modifier(tool['durability'] * 0.15, modern)}
        stats['tip'] = {
            'durability': modifier(tool['durability'] * 0.15, modern),
            'harvest_speed': modifier(tool['efficiency'] * 0.10, modern),
            'attack_damage' if modern else 'melee_damage': modifier(tool['damage'] * 0.15, modern),
        }
        if modern:
            stats['tip']['harvest_tier'] = harvest_tier(tool['harvest_level'], metal['name'])
            stats['rod']['traits'] = traits(metal, True, True)
            stats['tip']['traits'] = traits(metal, True, True)
    # Native gem settings remain available for Silent's Gems materials.
    if modern:
        stats['setting'] = {}
    return stats


def gear_material(metal, target):
    modern, latest = target != TARGETS[0], target == TARGETS[2]
    name, tool = metal['name'], metal.get('tool_stats')
    color = '#FF' + metal['color'].upper()
    # Use only our own ingots, avoiding ambiguous shared silver/copper tags:
    # Silent Gear can otherwise choose an unrelated mod's material definition.
    ingredient = 'metallurgy:' + name + '_ingot' if latest else {'item': 'metallurgy:' + name + '_ingot'}
    if modern:
        display = {'name': {'translate': 'material.metallurgy.' + name + '.name'},
                   'name_prefix': '', 'color': color, 'main_texture_type': 'HIGH_CONTRAST'}
        if latest:
            display['equippable'] = {
                'always_use_tint': True, 'asset_id': 'silentgear:generic_shiny',
                'compound_asset_id': 'silentgear:generic_shiny',
                'equip_sound': 'minecraft:item.armor.equip_generic'}
        return {'type': 'silentgear:simple', 'parent': 'silentgear:empty',
                'crafting': {'ingredient': ingredient, 'categories': ['metal', 'intermediate'],
                             'can_salvage': True, 'part_substitutes': {},
                             'gear_type_blacklist': [] if tool else ['silentgear:tool', 'silentgear:projectile']},
                'display': display,
                'properties': {'silentgear:' + key: value for key, value in part_stats(metal, True).items()}}
    model = {}
    for part, texture in [('main', 'main_generic_hc'), ('rod', 'rod_generic_lc'), ('tip', 'tip_sharp')]:
        if part != 'main' and not tool:
            continue
        model[part + '/all'] = [{'texture': 'silentgear:' + texture, 'color': color}]
    model['main/fragment'] = [{'texture': 'silentgear:metal', 'color': color}]
    if tool:
        model['rod/part'] = [{'texture': 'silentgear:rod', 'color': color}]
        model['tip/part'] = [{'texture': 'silentgear:tip_base', 'color': color},
                             {'texture': 'silentgear:tip', 'color': color}, 'silentgear:tip_shine']
    result = {'type': 'silentgear:standard', 'simple': True,
              'harvest_tier': tier_id(tool['harvest_level']) if tool else 'minecraft:wood',
              'availability': {'tier': tool['harvest_level'] if tool else 0,
                               'categories': ['metal'], 'visible': True, 'can_salvage': True,
                               'gear_blacklist': [] if tool else ['tool', 'projectile']},
              'crafting_items': {'main': ingredient, 'subs': {}},
              'name': {'translate': 'material.metallurgy.' + name + '.name'},
              'stats': part_stats(metal, False), 'model': model,
              'traits': {part: traits(metal, False, part != 'main')
                         for part in part_stats(metal, False)}}
    return result


def tetra_material(metal):
    tool = metal['tool_stats']
    level = tool['harvest_level']
    # Calibrated to native iron: primary5, secondary3.8, tertiary3,
    # magic84. Integrity is an explicit Tetra balance adaptation.
    return {'key': 'metallurgy_' + metal['name'], 'category': 'metal',
            'primary': round(3 + tool['damage'], 3),
            'secondary': round(2.8 + metal['hardness'] / 3, 3),
            'tertiary': round(max(1, min(6, tool['efficiency'] / 2)), 3),
            'durability': tool['durability'],
            'integrityCost': max(2, (level + 2) // 2),
            'integrityGain': min(8, 3 + level),
            'magicCapacity': 6 * tool['enchantability'],
            'toolLevel': tier_id(level), 'toolEfficiency': tool['efficiency'],
            'tints': {'glyph': metal['color'], 'texture': metal['color']},
            'textures': ['metal', 'default'],
            'material': {'items': ['metallurgy:' + metal['name'] + '_ingot']},
            'effects': PROFILES[metal['name']][0],
            'requiredTools': {'hammer_dig': 'minecraft:' + ('gold' if level <= 2 else 'iron' if level <= 4 else 'diamond' if level <= 6 else 'netherite')}}


def main():
    catalog = json.loads((ROOT / 'content/legacy-catalog.json').read_text(encoding='utf-8'))
    metals = [m for m in catalog['materials'] if 'tool_stats' in m or 'armor_stats' in m]
    if set(PROFILES) != {m['name'] for m in metals}:
        raise ValueError('Every eligible material needs an explicit reviewed compatibility profile')
    table = []
    for target in TARGETS:
        base = ROOT / target / 'src/main/resources'
        gear_pack=base/'resourcepacks/silentgear'
        metadata={'description':'Metallurgy native Silent Gear materials'}
        metadata.update({'pack_format':15 if target==TARGETS[0] else 48} if target!=TARGETS[2] else {'min_format':[101,1],'max_format':[101,1]})
        dump(gear_pack/'pack.mcmeta',{'pack':metadata})
        for metal in metals:
            dump(gear_pack / 'data/metallurgy/silentgear_materials' / (metal['name'] + '.json'), gear_material(metal, target))
        if target == TARGETS[0]:
            tetra_pack=base/'resourcepacks/tetra'
            dump(tetra_pack/'pack.mcmeta',{'pack':{'description':'Metallurgy native Tetra materials','pack_format':15}})
            for level in range(5, 8):
                dump(base / 'resourcepacks/core_tiers/data/metallurgy/tags/blocks' / ('needs_level_' + str(level) + '.json'),
                     {'replace': False, 'values': ['metallurgy:' + m['name'] + '_ore'
                                                 for m in catalog['materials'] if m['ore_harvest_level'] == level]})
            dump(tetra_pack / 'data/tetra/tiers/metallurgy.json', ['metallurgy:level_' + str(i) for i in range(5, 8)])
            for metal in metals:
                if 'tool_stats' in metal:
                    dump(tetra_pack / 'data/tetra/materials/metallurgy' / (metal['name'] + '.json'), tetra_material(metal))
        table.append({'target': target, 'silentgearMaterials': len(metals),
                      'tetraMaterials': sum('tool_stats' in m for m in metals) if target == TARGETS[0] else 0,
                      'state': 'source implemented; dependency runtime behavior unverified'})
    # Add translations without replacing the existing original language data.
    lang_path = ROOT / 'common/src/main/resources/assets/metallurgy/lang/en_us.json'
    lang = json.loads(lang_path.read_text(encoding='utf-8'))
    tetra_lang = {}
    for metal in metals:
        name = metal['name']
        title = lang.get('material.metallurgy.' + name + '.name', name.replace('_', ' ').title())
        lang.setdefault('material.metallurgy.' + name + '.name', title)
        if 'tool_stats' in metal:
            tetra_lang['tetra.material.metallurgy_' + name] = title
            tetra_lang['tetra.material.metallurgy_' + name + '.prefix'] = title
    dump(lang_path, lang)
    dump(ROOT / TARGETS[0] / 'src/main/resources/assets/tetra/lang/en_us.json', tetra_lang)
    dump(ROOT / 'docs/modular-compat-coverage.json', {
        'complete': False, 'targets': table,
        'materialIds': ['metallurgy:' + m['name'] for m in metals],
        'excluded': [m['name'] for m in catalog['materials'] if m not in metals],
        'limits': ['Original source-only checkpoint ran no builds or games; see later evidence for resumed compilation.',
                   'No Tetra releases found for the two NeoForge targets; no fabricated port/API adapter.',
                   'Native traits adapt material themes; original full-set powers and Tartarite absorption are not attached to modular parts.',
                   'Silent Gear is the modern tool system formerly in Silent Gems; Gems itself does not have a modern ToolPartRegistry.',
                   'Client tool tinting, workbench construction, repair, trait stacking, salvage, save/reload and mining require future native checks.']})
    rows = ['# Modular material catalog', '',
            'Generated maintained design reference. Source implementation only; no compatibility runtime checks have run.', '',
            'Effects and traits use the receiving mod\'s rules. Numeric base stats come from the source catalog; integrity and subordinate-part bonuses are explicit adaptations.', '',
            '| Material | Harvest level | Tool durability | Tetra innate effects | Silent Gear main traits |',
            '|---|---:|---:|---|---|']
    for metal in metals:
        tool = metal.get('tool_stats', {})
        tetra = ', '.join(k + ' ' + json.dumps(v) for k, v in PROFILES[metal['name']][0].items()) if tool else 'Armor only; no Tetra entry'
        gear = ', '.join(name + ' ' + str(level) for name, level in PROFILES[metal['name']][1])
        rows.append('| ' + ' | '.join([metal['name'].replace('_', ' ').title(), str(tool.get('harvest_level', '—')),
                                       str(tool.get('durability', '—')), tetra, gear]) + ' |')
    (ROOT / 'docs/MODULAR-MATERIALS.md').write_text('\n'.join(rows) + '\n', encoding='utf-8')
    print(json.dumps(table))


if __name__ == '__main__':
    main()
