"""Native arm-space clearance/poses over the unchanged source gadget pixel art."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]

def generate():
    root=ROOT/'common/src/main/resources/assets/metallurgy/models/item'
    for folder in ['', 'gadget/']:
        path=root/(folder+'rubracium_gauntlet.json')
        value=json.loads(path.read_text(encoding='utf-8'))
        # Native hand-layer transform yields arm-local center (-1,4,0).
        # 5.2x12.96x5.2 clears both the 4px arm and its 0.25px skin sleeve.
        for side,sign in [('righthand',1),('lefthand',-1)]:
            value['display']['thirdperson_'+side]={'rotation':[90,0,0],'translation':[0,-2,6],'scale':[sign*1.3,1.08,1.3]}
            # Solve native idle renderPlayerArm (including its 0.1rad arm roll)
            # against the regular held-item origin; native code mirrors Y/Z for left.
            value['display']['firstperson_'+side]={'rotation':[81.694,60.2207,47.9266],'translation':[0.9107,-1.9315,-1.4667],'scale':[sign*1.3,1.08,1.3]}
        for element in value['elements']:
            if element.get('name')=='plate':element['from'][1]=1.95
            if element.get('name') in ['finger0','finger1','finger2']:element['to'][1]=2.05
        path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')

        path=root/(folder+'ore_detector.json');value=json.loads(path.read_text(encoding='utf-8'))
        cuff=next(e for e in value['elements'] if e.get('name')=='sleeve')
        if cuff['from']==[6.1,12.35,9.5]:
            # Move the housing/handle/LEDs together 0.8 model units away from
            # the forearm. Moving rotation pivots too preserves each part's pose.
            for element in value['elements']:
                if element is cuff:continue
                for key in ['from','to']:element[key][2]=round(element[key][2]-0.8,5)
                if 'rotation' in element:element['rotation']['origin'][2]=round(element['rotation']['origin'][2]-0.8,5)
            cuff['from']=[5.6,12.35,8.85];cuff['to']=[10.9,17.35,14.15]
        elif cuff['from']!=[5.6,12.35,8.85] or cuff['to']!=[10.9,17.35,14.15]:
            raise ValueError('Unreviewed detector cuff geometry: '+str(path))
        path.write_text(json.dumps(value,indent=2)+'\n',encoding='utf-8')
    print('Updated two gauntlet and two detector model variants; original PNGs retained.')

if __name__=='__main__':generate()
