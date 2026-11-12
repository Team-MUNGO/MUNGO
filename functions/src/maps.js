import { ApiError } from './assistant.js';

// Google Maps Platform "Maps Experience agent tools" (formerly Maps Grounding Lite) REST endpoint.
// Same SKU/quota as the MCP search_places tool. pageSize <= 5 and no extended details keeps the
// request on the Essentials SKU; Pro features are deliberately never requested.
export const MAPS_SEARCH_URL = 'https://mapstools.googleapis.com/v1:searchText';
export const MAPS_PAGE_SIZE = 5;
export const MAPS_BIAS_RADIUS_M = 20000;

const PLACE_ID = /^[A-Za-z0-9_-]{10,300}$/;
const finite = (v, min, max) => typeof v === 'number' && Number.isFinite(v) && v >= min && v <= max;

/** Only places returned by Maps with an ID, a name and coordinates survive. Nothing is invented. */
export function verifiedPlaces(json) {
  if (!json || !Array.isArray(json.places)) return [];
  const seen = new Set();
  const out = [];
  for (const p of json.places) {
    const id = typeof p?.id === 'string' ? p.id : '';
    const lat = p?.location?.latitude, lng = p?.location?.longitude;
    const title = typeof p?.attribution?.title === 'string' ? p.attribution.title : '';
    const name = title.replace(/\s*-\s*Google Maps\s*$/i, '').trim();
    if (!PLACE_ID.test(id) || seen.has(id) || !finite(lat, -90, 90) || !finite(lng, -180, 180) || !name || name.length > 100) continue;
    if (typeof p.place === 'string' && p.place !== `places/${id}`) continue;
    const mapsUrl = typeof p.googleMapsLinks?.placeUrl === 'string' && p.googleMapsLinks.placeUrl.startsWith('https://www.google.com/maps/')
      ? p.googleMapsLinks.placeUrl : null;
    seen.add(id);
    // The API returns no postal address on the Essentials SKU: address stays null, never guessed.
    out.push({ placeId:id, name, latitude:lat, longitude:lng, address:null, mapsUrl, canAddWaypoint:true });
  }
  return out;
}

export async function searchPlaces({ query, location, apiKey, fetchImpl = fetch }) {
  if (!apiKey) throw new ApiError(503, 'MAPS_UNAVAILABLE', '장소 검색을 준비하고 있어요.');
  if (typeof query !== 'string' || !query.trim() || query.length > 100) throw new ApiError(400, 'INVALID_INPUT', '검색어를 확인해 주세요.');
  let response;
  try {
    response = await fetchImpl(MAPS_SEARCH_URL, {
      method:'POST', redirect:'error', signal:AbortSignal.timeout(8000),
      headers:{ 'Content-Type':'application/json', 'X-Goog-Api-Key':apiKey },
      body:JSON.stringify({
        textQuery:query.trim(),
        locationBias:{ circle:{ center:{ latitude:location.latitude, longitude:location.longitude }, radiusMeters:MAPS_BIAS_RADIUS_M } },
        languageCode:'ko', regionCode:'KR', pageSize:MAPS_PAGE_SIZE
      })
    });
  } catch {
    throw new ApiError(502, 'MAPS_UNAVAILABLE', '장소를 검색하지 못했어요. 잠시 후 다시 시도해 주세요.');
  }
  if (!response.ok) throw new ApiError(502, 'MAPS_UNAVAILABLE', '장소를 검색하지 못했어요. 잠시 후 다시 시도해 주세요.');
  let json;
  try {
    const raw = await response.text();
    if (raw.length > 200000) throw new Error('oversize');
    json = JSON.parse(raw);
  } catch {
    throw new ApiError(502, 'MAPS_UNAVAILABLE', '장소 검색 결과를 확인하지 못했어요.');
  }
  const summary = typeof json.summary === 'string' ? json.summary.slice(0, 2000) : '';
  return { places:verifiedPlaces(json), summary };
}
