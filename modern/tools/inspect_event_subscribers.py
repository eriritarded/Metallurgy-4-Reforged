"""Inspect event-listener classfile metadata without loading or executing game classes."""
import argparse
import hashlib
import json
import re
import struct
import zipfile
from pathlib import Path


def class_info(data):
    offset = 8

    def take(count):
        nonlocal offset
        result = data[offset:offset + count]
        offset += count
        return result

    def u2():
        return struct.unpack('>H', take(2))[0]

    pool = [None] * u2()
    index = 1
    while index < len(pool):
        tag = take(1)[0]
        if tag == 1:
            pool[index] = take(u2()).decode('utf-8', errors='replace')
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            take(4)
        elif tag in (5, 6):
            take(8)
            index += 1
        elif tag == 15:
            take(3)
        else:
            raise ValueError(f'Unknown classfile constant tag: {tag}')
        index += 1
    flags, own_class, parent = u2(), u2(), u2()
    take(u2() * 2)

    def annotation():
        kind = pool[u2()]
        for _ in range(u2()):
            u2()
            value()
        return kind

    def value():
        kind = chr(take(1)[0])
        if kind in 'BCDFIJSZsc':
            u2()
        elif kind == 'e':
            take(4)
        elif kind == '@':
            annotation()
        elif kind == '[':
            for _ in range(u2()):
                value()
        else:
            raise ValueError(f'Unknown annotation value: {kind}')

    def members():
        result = []
        for _ in range(u2()):
            member_flags, name, descriptor = u2(), pool[u2()], pool[u2()]
            annotations = []
            for _ in range(u2()):
                attr_name = pool[u2()]
                length = struct.unpack('>I', take(4))[0]
                end = offset + length
                if attr_name in ('RuntimeVisibleAnnotations', 'RuntimeInvisibleAnnotations'):
                    annotations.extend(annotation() for _ in range(u2()))
                else:
                    take(length)
                if offset != end:
                    raise ValueError('Malformed classfile attribute')
            result.append((name, descriptor, annotations, member_flags))
        return result

    members()  # fields
    methods = members()
    return {'name': pool[pool[own_class]], 'flags': flags, 'methods': methods}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--artifact', type=Path, required=True)
    parser.add_argument('--classpath', type=Path, action='append', default=[])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    classes, owners = {}, {}
    for path in [args.artifact, *args.classpath]:
        with zipfile.ZipFile(path) as archive:
            for name in archive.namelist():
                if name.endswith('.class') and name not in classes:
                    classes[name] = archive.read(name)
                    owners[name] = path.name
    rows = []
    for entry, owner in owners.items():
        if owner != args.artifact.name:
            continue
        own = class_info(classes[entry])
        for method, descriptor, annotations, _ in own['methods']:
            if not any(x.endswith('/SubscribeEvent;') for x in annotations):
                continue
            parameter = re.fullmatch(r'\(L([^;]+);\)V', descriptor)
            if not parameter:
                raise ValueError(f'Unexpected listener descriptor: {descriptor}')
            event = parameter[1]
            event_bytes = classes.get(event + '.class')
            info = class_info(event_bytes) if event_bytes is not None else None
            rows.append({'class': own['name'], 'method': method, 'event': event,
                         'eventClassFound': info is not None,
                         'eventIsAbstract': bool(info['flags'] & 0x400) if info else None,
                         'eventIsInterface': bool(info['flags'] & 0x200) if info else None})
    with args.artifact.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    failures = [row for row in rows if not row['eventClassFound'] or row['eventIsAbstract'] or row['eventIsInterface']]
    report = {'method': 'Static classfile metadata only; no classes loaded and no game/test execution',
              'artifact': str(args.artifact), 'sha256': digest,
              'classpath': [str(p) for p in args.classpath],
              'listeners': rows, 'listenerCount': len(rows), 'invalidOrUnresolved': failures}
    args.output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: report[key] for key in ['sha256', 'listenerCount', 'invalidOrUnresolved']}, indent=2))


if __name__ == '__main__':
    main()
