"""Remove unassigned Blockbench export faces; retain every authored textured face and original PNG."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
MODELS = ('block/bimetal_block', 'block/sublimation_chamber', 'block/lit_sublimation_chamber',
          'item/ore_detector', 'item/gadget/ore_detector',
          'item/rubracium_gauntlet', 'item/gadget/rubracium_gauntlet')

def main():
    for name in MODELS:
        path = ROOT / 'common/src/main/resources/assets/metallurgy/models' / (name + '.json')
        data = json.loads(path.read_text(encoding='utf-8'))
        if 'missing' in data.get('textures', {}):
            raise ValueError('Unexpected assigned texture slot: ' + name)
        removed = 0
        for element in data.get('elements', []):
            faces = element.get('faces', {})
            for side in list(faces):
                if faces[side].get('texture') == '#missing':
                    del faces[side]
                    removed += 1
        if removed:
            path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
        print(name + ': removed ' + str(removed) + ' unassigned faces')

if __name__ == '__main__':
    main()
