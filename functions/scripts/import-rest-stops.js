// Import an operator-verified JSON catalog. Never fabricate POI IDs or pet facilities.
import { readFileSync, writeFileSync } from 'node:fs';
import { nearestStops } from '../src/assistant.js';
const path=process.argv[2];
if (!path) throw new Error('Usage: node scripts/import-rest-stops.js /path/to/verified-catalog.json');
const catalog=JSON.parse(readFileSync(path,'utf8'));
nearestStops(catalog,{latitude:37,longitude:127},Date.now());
if (!catalog.stops.length) throw new Error('Catalog must contain verified stops.');
const ids=new Set();
for (const s of catalog.stops) {
  if (typeof s.id !== 'string' || !s.id || s.id.length>100 || ids.has(s.id) ||
      typeof s.name !== 'string' || !s.name.trim() || s.name.length>100 ||
      !Number.isFinite(s.latitude) || s.latitude < -90 || s.latitude > 90 ||
      !Number.isFinite(s.longitude) || s.longitude < -180 || s.longitude > 180 ||
      !Array.isArray(s.facilities) || s.facilities.some(f=>typeof f !== 'string' || f.length>100)) throw new Error('Invalid or duplicate stop.');
  if (s.navigation && ![s.navigation.id,s.navigation.subId,s.navigation.provider].every(v=>typeof v === 'string' && v.length>0 && v.length<=100)) throw new Error('Invalid verified navigation POI mapping.');
  ids.add(s.id);
}
writeFileSync(new URL('../data/rest-stops.json',import.meta.url),JSON.stringify(catalog,null,2)+'\n');
process.stdout.write(`Imported ${ids.size} verified stops.\n`);
