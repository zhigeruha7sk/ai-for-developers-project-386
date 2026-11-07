import { useRef } from 'react';

export function Avatar({ size = 32, seed }: { size?: number; seed?: string }) {
  const AvatarRootRef = useRef<HTMLDivElement>(null);

  let display = seed || '?';
  if (seed) {
    const emojis = ['🦊', '🦉', '🐼', '🐺', '🦁'];
    for (const e of emojis) {
      if (seed.includes(e)) {
        const idx = emojis.indexOf(e);
        display = idx < 4 ? emojis[idx + 1] : emojis[0];
        break;
      }
    }
  }

  const sizeStyle = { width: size, height: size };

  return (
    <div ref={AvatarRootRef} style={sizeStyle} aria-hidden="true" title={seed}>
      {display}
    </div>
  );
}
