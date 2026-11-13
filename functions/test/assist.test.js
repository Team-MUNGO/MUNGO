import test from 'node:test';
import assert from 'node:assert/strict';
import { assist, validateAssist } from '../src/assist.js';
import { GEOCODE_URL, koreanAddress, verifiedPlaces } from '../src/maps.js';
import { AssistantStore } from '../src/store.js';

const gemini = obj => ({ ok:true, text:async () => JSON.stringify({ candidates:[{ finishReason:'STOP', content:{ parts:[{ text:JSON.stringify(obj) }] } }] }) });
const mapsBody = { places:[
  { place:'places/ChIJvSMioIapZDURijUJ9O6phq8', id:'ChIJvSMioIapZDURijUJ9O6phq8', location:{ latitude:37.241832, longitude:127.389932 },
    googleMapsLinks:{ placeUrl:'https://www.google.com/maps/place//data=x' }, attribution:{ title:'덕평휴게소 달려라코코 - Google Maps', url:'https://www.google.com/maps/place//data=x' } },
  { place:'places/ChIJsecondPlace0001', id:'ChIJsecondPlace0001', location:{ latitude:37.1, longitude:127.1 }, attribution:{ title:'이천휴게소 - Google Maps' } }
], summary:'덕평휴게소 달려라코코[0]는 반려견 놀이터가 있어요.' };
const ctx = { comfortScore:88, minutesToRest:18, petSize:'MEDIUM', temperatureStatus:'NORMAL' };
const loc = { latitude:37.24, longitude:127.21, ageMs:1000 };

const geocodeBody = latlng => ({ status:'OK', results:[{ formatted_address:`대한민국 경기도 이천시 마장면 ${latlng}` }] });

function harness(replies, { mapsOk=true, maps=mapsBody, geocode=geocodeBody } = {}) {
  const calls = { gemini:[], maps:[], geocode:[] };
  const fetchImpl = async (url, init) => {
    if (url.startsWith(GEOCODE_URL)) {
      const params = new URL(url).searchParams;
      calls.geocode.push(Object.fromEntries(params));
      assert.equal(params.get('key'), 'maps-key'); assert.equal(params.get('language'), 'ko');
      const body = geocode(params.get('latlng'));
      return body ? { ok:true, text:async () => JSON.stringify(body) } : { ok:false, text:async () => '' };
    }
    if (url.includes('mapstools')) {
      calls.maps.push(JSON.parse(init.body));
      assert.equal(init.headers['X-Goog-Api-Key'], 'maps-key');
      if (!mapsOk) return { ok:false, text:async () => '' };
      return { ok:true, text:async () => JSON.stringify(maps) };
    }
    calls.gemini.push(JSON.parse(init.body));
    assert.equal(init.headers['x-goog-api-key'], 'gem-key');
    return gemini(replies.shift());
  };
  return { calls, opts:{ geminiKey:'gem-key', mapsKey:'maps-key', model:'gemini-test', fetchImpl } };
}
const q = (question, extra={}) => validateAssist({ type:'USER_QUESTION', requestId:'req-1', eventId:null, question, context:ctx, ...extra });

test('#18/#20 general question: Gemini answers from context, Maps never called', async () => {
  const { calls, opts } = harness([{ intent:'ANSWER', answerText:'컴포트 점수는 88점이에요.', searchQuery:'' }]);
  const r = await assist(q('우리 강아지 괜찮아?'), opts);
  assert.equal(r.status, 'OK'); assert.equal(r.displayText, '컴포트 점수는 88점이에요.'); assert.equal(r.ttsText, r.displayText);
  assert.deepEqual(r.places, []); assert.equal(calls.maps.length, 0); assert.equal(calls.gemini.length, 1);
  assert.equal(JSON.parse(calls.gemini[0].contents[0].parts[0].text).context.comfortScore, 88);
});

test('#19 place intent is chosen by Gemini (no keyword matching); location requested before any Maps call', async () => {
  const { calls, opts } = harness([{ intent:'PLACE_SEARCH', answerText:'', searchQuery:'반려견 동반 카페' }]);
  const r = await assist(q('멍멍이랑 잠깐 들를 데 없을까'), opts);
  assert.equal(r.status, 'LOCATION_REQUIRED'); assert.equal(r.searchQuery, '반려견 동반 카페'); assert.equal(calls.maps.length, 0);
});

test('#21 follow-up with location: places come only from Maps; Gemini picks an ID and never sees coordinates', async () => {
  const { calls, opts } = harness([{ message:'이천휴게소에서 쉬어 가세요.', recommendedPlaceId:'ChIJsecondPlace0001' }]);
  let reserved = 0;
  const r = await assist(q('쉴 곳 찾아줘', { searchQuery:'반려견 동반 휴게소', location:loc }), { ...opts, reserveMaps:async () => { reserved++; } });
  assert.equal(r.status, 'OK'); assert.equal(reserved, 1); assert.equal(calls.gemini.length, 1);
  assert.equal(r.recommendedPlaceId, 'ChIJsecondPlace0001');
  assert.deepEqual(r.places.map(p => [p.placeId, p.name, p.latitude, p.longitude, p.address, p.canAddWaypoint]),
    [['ChIJvSMioIapZDURijUJ9O6phq8', '덕평휴게소 달려라코코', 37.241832, 127.389932, '경기도 이천시 마장면 37.241832,127.389932', true],
      ['ChIJsecondPlace0001', '이천휴게소', 37.1, 127.1, '경기도 이천시 마장면 37.1,127.1', true]]);
  assert.equal(calls.geocode.length, 2); assert(r.places.every(p => p.originalName === undefined));
  assert.equal(calls.maps[0].pageSize, 5); assert.equal(calls.maps[0].includeExtendedDetails, undefined);
  assert.equal(calls.maps[0].locationBias.circle.center.latitude, 37.24);
  assert(!JSON.stringify(calls.gemini[0]).includes('127.21') && !JSON.stringify(calls.gemini[0]).includes('37.24'));
  assert.deepEqual(r.sources, { text:'gemini', places:'google_maps' });
});

test('#21 invented place ID / ETA claims / missing name fall back to verified template', async () => {
  for (const reply of [{ message:'가짜휴게소로 가세요.', recommendedPlaceId:'ChIJinvented00000' }, { message:'덕평휴게소 달려라코코까지 15분 뒤 도착해요.', recommendedPlaceId:'ChIJvSMioIapZDURijUJ9O6phq8' }]) {
    const { opts } = harness([reply]);
    const r = await assist(validateAssist({ type:'REST_RECOMMENDATION', requestId:'r', eventId:'rest-cycle-1', context:ctx, location:loc }), opts);
    assert.equal(r.recommendedPlaceId, 'ChIJvSMioIapZDURijUJ9O6phq8');
    assert.equal(r.displayText, `덕평휴게소 달려라코코까지 약 ${r.places[0].approxMinutes}분 거리예요. 진입 방향과 운영 여부는 내비게이션에서 확인해 주세요.`);
    assert.equal(r.eventId, 'rest-cycle-1');
  }
});

test('#21 verifiedPlaces drops entries without ID/name/coords or with mismatched resource name', () => {
  const out = verifiedPlaces({ places:[
    { id:'short', location:{ latitude:1, longitude:1 }, attribution:{ title:'A - Google Maps' } },
    { id:'ChIJvalidButNoCoords', attribution:{ title:'B - Google Maps' } },
    { id:'ChIJvalidButNoName00', location:{ latitude:1, longitude:1 } },
    { place:'places/other', id:'ChIJmismatchedName00', location:{ latitude:1, longitude:1 }, attribution:{ title:'C' } },
    { id:'ChIJokPlace000000000', location:{ latitude:91, longitude:1 }, attribution:{ title:'D' } }
  ] });
  assert.deepEqual(out, []);
});

test('#22 Maps failure is an explicit error; empty results are NO_RESULTS without a phrasing call', async () => {
  const failing = harness([], { mapsOk:false });
  await assert.rejects(assist(q('x', { searchQuery:'휴게소', location:loc }), failing.opts), { code:'MAPS_UNAVAILABLE' });
  const empty = harness([], { maps:{ places:[] } });
  const r = await assist(q('x', { searchQuery:'휴게소', location:loc }), empty.opts);
  assert.equal(r.status, 'NO_RESULTS'); assert.deepEqual(r.places, []); assert.equal(empty.calls.gemini.length, 0);
});

test('#23 rest recommendation without location asks for it; LARGE dog adjusts the server-side query', async () => {
  const { calls, opts } = harness([{ message:'덕평휴게소 달려라코코에서 쉬어요.', recommendedPlaceId:'ChIJvSMioIapZDURijUJ9O6phq8' }]);
  const none = await assist(validateAssist({ type:'REST_RECOMMENDATION', requestId:'r', eventId:'c1', context:ctx }), opts);
  assert.equal(none.status, 'LOCATION_REQUIRED'); assert.equal(calls.gemini.length, 0);
  await assist(validateAssist({ type:'REST_RECOMMENDATION', requestId:'r', eventId:'c1', context:{ ...ctx, petSize:'LARGE' }, location:loc }), opts);
  assert.equal(calls.maps[0].textQuery, '대형견 동반 가능한 휴게소');
});

test('validation: unknown fields, bad types, stale location rejected; absent context dropped', () => {
  assert.throws(() => validateAssist({ type:'X', requestId:'a' }), { code:'INVALID_INPUT' });
  assert.throws(() => validateAssist({ type:'USER_QUESTION', requestId:'a', question:'q', audio:'x' }), { code:'INVALID_INPUT' });
  assert.throws(() => validateAssist({ type:'USER_QUESTION', requestId:'a', question:'q', context:{ gps:1 } }), { code:'INVALID_INPUT' });
  assert.throws(() => validateAssist({ type:'USER_QUESTION', requestId:'a', question:'q', location:{ ...loc, ageMs:60000 } }), { code:'LOCATION_UNAVAILABLE' });
  assert.throws(() => validateAssist({ type:'REST_RECOMMENDATION', requestId:'a', question:'q' }), { code:'INVALID_INPUT' });
  const v = validateAssist({ type:'USER_QUESTION', requestId:'a', question:' q ', context:{ temperatureC:null, comfortScore:90 } });
  assert.deepEqual(v.context, { comfortScore:90 }); assert.equal(v.question, 'q');
});

test('#30 Maps daily cap is enforced independently', async () => {
  const data = new Map();
  const db = { collection:() => ({ doc:id => ({ key:id }) }), runTransaction:async fn => fn({ get:async r => ({ data:() => data.get(r.key) }), set:(r, v) => data.set(r.key, v) }) };
  const store = new AssistantStore(db);
  await store.reserveMaps(0, 2); await store.reserveMaps(0, 2);
  await assert.rejects(store.reserveMaps(0, 2), { code:'MAPS_LIMITED' });
});

test('ANSWER time claims are allowed only when they match values the app sent', async () => {
  const { unsupportedTimeClaim } = await import('../src/assist.js');
  assert.equal(unsupportedTimeClaim('서울역까지 40분 걸려요.', { routeRemainingMinutes:40 }), false);
  assert.equal(unsupportedTimeClaim('서울역까지 1시간 5분 걸려요.', { routeRemainingMinutes:65 }), false);
  assert.equal(unsupportedTimeClaim('25분 후에 쉬어 가요.', { minutesToRest:25 }), false);
  assert.equal(unsupportedTimeClaim('30분 후 도착해요.', { routeRemainingMinutes:40 }), true);
  assert.equal(unsupportedTimeClaim('40분 걸려요.', {}), true);
  assert.equal(unsupportedTimeClaim('도착 예정 시간은 알 수 없어요.', {}), true);
  assert.equal(unsupportedTimeClaim('다음 휴식까지 25분 남았어요.', {}), false);
});

test('petName is accepted and validated', () => {
  const base = { type:'USER_QUESTION', requestId:'r1', question:'괜찮아?' };
  assert.equal(validateAssist({ ...base, context:{ petName:'현댕이' } }).context.petName, '현댕이');
  assert.throws(() => validateAssist({ ...base, context:{ petName:'x'.repeat(21) } }));
});

const englishMaps = { places:[
  { place:'places/ChIJenglishPlace0001', id:'ChIJenglishPlace0001', location:{ latitude:37.3, longitude:127.3 }, attribution:{ title:'Happy Dog Park - Google Maps' } },
  { place:'places/ChIJsecondPlace0001', id:'ChIJsecondPlace0001', location:{ latitude:37.1, longitude:127.1 }, attribution:{ title:'이천휴게소 - Google Maps' } }
], summary:'' };
const rest = () => validateAssist({ type:'REST_RECOMMENDATION', requestId:'r', eventId:'c1', context:ctx, location:loc });

test('English Maps name becomes a Korean display name; Maps name and coordinates are kept', async () => {
  const { opts } = harness([{ message:'해피 도그 파크에서 쉬어 가요.', recommendedPlaceId:'ChIJenglishPlace0001',
    koreanNames:[{ placeId:'ChIJenglishPlace0001', name:'해피 도그 파크' }, { placeId:'ChIJsecondPlace0001', name:'바꾸면 안 됨' }] }], { maps:englishMaps });
  const r = await assist(rest(), opts);
  const [eng, kor] = r.places;
  assert.equal(eng.name, '해피 도그 파크'); assert.equal(eng.originalName, 'Happy Dog Park');
  assert.equal(eng.latitude, 37.3); assert.equal(eng.longitude, 127.3); assert.equal(eng.address, '경기도 이천시 마장면 37.3,127.3');
  // Already-Korean names are never replaced by the model.
  assert.equal(kor.name, '이천휴게소'); assert.equal(kor.originalName, undefined);
  assert.equal(r.displayText, '해피 도그 파크에서 쉬어 가요.');
});

test('Korean name proposals without Hangul or for unknown places are ignored', async () => {
  const { opts } = harness([{ message:'Happy Dog Park에서 쉬어 가요.', recommendedPlaceId:'ChIJenglishPlace0001',
    koreanNames:[{ placeId:'ChIJenglishPlace0001', name:'Happy Dog Park' }, { placeId:'ChIJinvented00000', name:'가짜' }] }], { maps:englishMaps });
  const r = await assist(rest(), opts);
  assert.equal(r.places[0].name, 'Happy Dog Park'); assert.equal(r.places[0].originalName, undefined);
});

test('geocode failure or quota leaves the address null without failing the recommendation', async () => {
  const failing = harness([{ message:'이천휴게소에서 쉬어요.', recommendedPlaceId:'ChIJsecondPlace0001', koreanNames:[] }], { geocode:() => null });
  const r = await assist(rest(), failing.opts);
  assert.equal(r.status, 'OK'); assert(r.places.every(p => p.address === null));
  const limited = harness([{ message:'이천휴게소에서 쉬어요.', recommendedPlaceId:'ChIJsecondPlace0001', koreanNames:[] }]);
  const q2 = await assist(rest(), { ...limited.opts, reserveGeocode:async () => { throw new Error('limit'); } });
  assert(q2.places.every(p => p.address === null)); assert.equal(limited.calls.geocode.length, 0);
});

test('koreanAddress strips the country and rejects non-Korean or failed results', () => {
  assert.equal(koreanAddress({ status:'OK', results:[{ formatted_address:'대한민국 서울특별시 중구 세종대로 110' }] }), '서울특별시 중구 세종대로 110');
  assert.equal(koreanAddress({ status:'OK', results:[{ formatted_address:'110 Sejong-daero, Seoul' }] }), null);
  assert.equal(koreanAddress({ status:'ZERO_RESULTS', results:[] }), null);
});

test('geocode daily cap counts every address lookup', async () => {
  const data = new Map();
  const db = { collection:() => ({ doc:id => ({ key:id }) }), runTransaction:async fn => fn({ get:async r => ({ data:() => data.get(r.key) }), set:(r, v) => data.set(r.key, v) }) };
  const store = new AssistantStore(db);
  await store.reserveGeocode(0, 5, 8);
  await assert.rejects(store.reserveGeocode(0, 5, 8), { code:'GEOCODE_LIMITED' });
});

test('rest recommendation searches rest areas AND dog-friendly cafes/parks, merged nearest first with approx minutes', async () => {
  const { calls, opts } = harness([{ message:'이천휴게소는 약 4분 거리예요. dd와 잠시 쉬어 가요.', recommendedPlaceId:'ChIJsecondPlace0001', koreanNames:[] }]);
  let reserved = 0;
  const near = { latitude:37.12, longitude:127.12, ageMs:1000 };
  const r = await assist(validateAssist({ type:'REST_RECOMMENDATION', requestId:'r', eventId:'c1', context:ctx, location:near }), { ...opts, reserveMaps:async () => { reserved++; } });
  assert.deepEqual(calls.maps.map(m => m.textQuery), ['반려견 동반 가능한 휴게소', '반려견 동반 가능한 카페 공원']);
  assert.equal(reserved, 2);
  assert.deepEqual(r.places.map(p => p.placeId), ['ChIJsecondPlace0001', 'ChIJvSMioIapZDURijUJ9O6phq8']); // deduped, nearest first
  const [first] = r.places;
  assert(first.distanceMeters > 0 && Number.isInteger(first.approxMinutes));
  // Gemini sees approx minutes but never coordinates
  const sent = JSON.stringify(calls.gemini[0]);
  assert(sent.includes('approxMinutes') && !sent.includes('37.12'));
  const expected = `이천휴게소는 약 ${first.approxMinutes}분 거리예요. dd와 잠시 쉬어 가요.`;
  assert.equal(first.approxMinutes, 4, `fixture distance gives ${first.approxMinutes}`);
  assert.equal(r.displayText, expected);
});

test('placeTimeOk: only the chosen place approx minutes, never arrival times', async () => {
  const { placeTimeOk, restSearchQueries } = await import('../src/assist.js');
  assert.equal(placeTimeOk('약 12분 거리예요.', 12), true);
  assert.equal(placeTimeOk('쉬어 가요.', 12), true);
  assert.equal(placeTimeOk('약 20분 거리예요.', 12), false);
  assert.equal(placeTimeOk('12분 뒤 도착 예정이에요.', 12), false);
  assert.deepEqual(restSearchQueries('LARGE'), ['대형견 동반 가능한 휴게소', '대형견 동반 가능한 카페 공원']);
});

test('one of the two rest searches failing still recommends from the other', async () => {
  let n = 0;
  const base = harness([{ message:'덕평휴게소 달려라코코에서 쉬어요.', recommendedPlaceId:'ChIJvSMioIapZDURijUJ9O6phq8', koreanNames:[] }]);
  const fetchImpl = async (url, init) => url.includes('mapstools') && n++ === 1 ? { ok:false, text:async () => '' } : base.opts.fetchImpl(url, init);
  const r = await assist(rest(), { ...base.opts, fetchImpl });
  assert.equal(r.status, 'OK'); assert.equal(r.places.length, 2);
});
