import test from 'node:test';
import assert from 'node:assert/strict';
import { summarize, validateSummary } from '../src/summary.js';

const gemini = obj => ({ ok:true, status:200, text:async () => JSON.stringify({ candidates:[{ finishReason:'STOP', content:{ parts:[{ text:JSON.stringify(obj) }] } }] }) });
const trip = {
  petName:'현댕이', durationMinutes:154, finalScore:88, minScore:80, restCount:1, restMinutes:16,
  counts:{ rapidBraking:2, sharpTurn:1, speedBump:2, rapidAcceleration:0, windowWarning:1, temperatureAlert:0 },
  events:[{ minute:100, kind:'RAPID_BRAKING', scoreDelta:-7 }, { minute:63, kind:'RECOVERY', scoreDelta:1 }],
  motionSigns:['YAWN']
};
const body = (t = trip) => ({ requestId:'trip-1', trip:t });
const opts = reply => {
  const calls = [];
  return { calls, opts:{ geminiKey:'k', model:'gemini-test', fetchImpl:async (url, init) => { calls.push(JSON.parse(init.body)); return gemini(reply); } } };
};

test('summary: sends only the trip record and returns the one-liner', async () => {
  const { calls, opts:o } = opts({ oneLine:'출발 100분쯤 급정거 말고는 대부분 평온했어요. 휴식 뒤 점수가 다시 올랐어요.' });
  const r = await summarize(validateSummary(body()), o);
  assert.equal(r.text, '출발 100분쯤 급정거 말고는 대부분 평온했어요. 휴식 뒤 점수가 다시 올랐어요.');
  assert.equal(r.requestId, 'trip-1');
  const payload = JSON.parse(calls[0].contents[0].parts[0].text);
  assert.deepEqual(Object.keys(payload), ['trip']);
  assert.equal(payload.trip.previousScore, undefined);
});

test('summary: invented clock times are rejected', async () => {
  const { opts:o } = opts({ oneLine:'오후 2시 급정거 말고는 평온했어요.' });
  await assert.rejects(summarize(validateSummary(body()), o), e => e.code === 'AI_UNAVAILABLE');
});

test('summary: unknown fields, bad counts and unknown signs are refused', () => {
  assert.throws(() => validateSummary({ ...body(), extra:1 }), e => e.code === 'INVALID_INPUT');
  assert.throws(() => validateSummary(body({ ...trip, latitude:37 })), e => e.code === 'INVALID_INPUT');
  assert.throws(() => validateSummary(body({ ...trip, counts:{ ...trip.counts, rapidBraking:-1 } })), e => e.code === 'INVALID_INPUT');
  assert.throws(() => validateSummary(body({ ...trip, motionSigns:['SICK'] })), e => e.code === 'INVALID_INPUT');
  assert.throws(() => validateSummary(body({ ...trip, events:Array(41).fill({ minute:1, kind:'REST' }) })), e => e.code === 'INVALID_INPUT');
});
