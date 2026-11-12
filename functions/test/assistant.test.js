import test from 'node:test';
import assert from 'node:assert/strict';
import { credential, validateQuestion, nearestStops, generateAnswer, bearer, hash, newToken } from '../src/assistant.js';
const now=Date.parse('2026-11-12T00:00:00Z');
const input={question:'지금 온도는 어때요?',mode:'status',context:{score:87,elapsedMinutes:15,temperatureC:20}};
// Explicit fixtures, never shipped as real places.
const catalog={sourceUrl:'https://example.org/test-only',license:'TEST FIXTURE',updatedAt:'2026-11-01',stops:[{id:'fixture-a',name:'테스트 전용 장소',latitude:37,longitude:127,facilities:['테스트 시설']}]};
function fake(answer,finishReason='STOP') { return async()=>({ok:true,text:async()=>JSON.stringify({candidates:[{finishReason,content:{parts:[{text:JSON.stringify(answer)}]}}]})}); }
test('text validation rejects unknown/audio/actions and invalid sensors',()=>{
  assert.deepEqual(validateQuestion(input),input);
  for (const bad of [{...input,audio:'x'},{...input,action:'closeWindow'},{...input,question:'x'.repeat(501)},{...input,context:{score:101}},{...input,context:{temperatureC:NaN}},{...input,location:{latitude:37,longitude:127,ageMs:0}}]) assert.throws(()=>validateQuestion(bad));
});
test('nearby requires recent valid coordinates',()=>{
  const near={...input,mode:'nearby',location:{latitude:37,longitude:127,ageMs:30_000}};
  assert.equal(validateQuestion(near).mode,'nearby');
  assert.throws(()=>validateQuestion({...near,location:{...near.location,ageMs:30001}}));
  assert.throws(()=>validateQuestion({...near,location:{...near.location,latitude:91}}));
});
test('no catalog or stale catalog yields unavailable, not invented places',()=>{
  assert.throws(()=>nearestStops({stops:[]},{latitude:37,longitude:127},now),{code:'CATALOG_UNAVAILABLE'});
  assert.throws(()=>nearestStops({...catalog,updatedAt:'2024-01-01'},{latitude:37,longitude:127},now));
  assert.equal(nearestStops(catalog,{latitude:37,longitude:127},now)[0].distanceMeters,0);
  assert.equal(nearestStops(catalog,{latitude:0,longitude:0},now).length,0);
});
test('Gemini receives text context, no GPS/audio, credentials in header only',async()=>{
  let request;
  const answer=await generateAnswer(input,{apiKey:'test-secret',model:'test-model',now,fetchImpl:async(url,options)=>{
    request={url,...options}; return fake({answerText:'현재 실내 온도는 20도예요.',selectedStopId:''})();
  }});
  assert.match(answer.answerText,/20/);
  assert(!request.url.includes('test-secret'));
  assert.equal(request.headers['x-goog-api-key'],'test-secret');
  assert(!request.body.includes('inlineData'));
  assert(!request.body.includes('latitude'));
});
test('nearby chooses catalog ID; ignores invented model claims and never sends GPS',async()=>{
  let body;
  const result=await generateAnswer({...input,mode:'nearby',location:{latitude:37,longitude:127,ageMs:0}},{apiKey:'x',model:'test-model',catalog,now,fetchImpl:async(_url,options)=>{body=options.body;return fake({answerText:'강아지 수영장이 운영 중이고 5분 걸려요',selectedStopId:'fixture-a'})();}});
  assert.equal(result.selectedStopId,'fixture-a');
  assert(!result.answerText.includes('수영장'));
  assert(!result.answerText.includes('5분'));
  assert(!body.includes('latitude'));
});
test('unknown POI, blocked/truncated/malformed replies and upstream errors fail safely',async()=>{
  for (const fetchImpl of [fake({answerText:'응답',selectedStopId:'invented'}),fake({answerText:'응답',selectedStopId:''},'MAX_TOKENS'),async()=>{throw Error('test');},async()=>({ok:true,text:async()=>'invalid'})]) {
    await assert.rejects(generateAnswer(input,{apiKey:'x',model:'test-model',fetchImpl,now}),{code:'AI_UNAVAILABLE'});
  }
});
test('empty verified vicinity returns no results without a Gemini call',async()=>{
  const r=await generateAnswer({...input,mode:'nearby',location:{latitude:0,longitude:0,ageMs:0}},{catalog,now,fetchImpl:()=>assert.fail('No upstream call')});
  assert.equal(r.stops.length,0);
});
test('opaque tokens, hash and bearer validation',()=>{
  const token=newToken(); assert.equal(token.length,43); assert.equal(bearer(`Bearer ${token}`),token);
  assert.equal(hash(token).length,64); assert.notEqual(hash(token),token);
  assert.throws(()=>bearer('Bearer short')); assert.throws(()=>bearer('secret'));
});
test('credential distinguishes Firebase ID token (JWT) from dev session token',()=>{
  const session=newToken(); assert.deepEqual(credential(`Bearer ${session}`),{kind:'session',token:session});
  assert.deepEqual(credential('Bearer aaa.bbb.ccc'),{kind:'firebase',token:'aaa.bbb.ccc'});
  assert.throws(()=>credential(`Bearer a.b.${'c'.repeat(5000)}`),{code:'UNAUTHENTICATED'});
  assert.throws(()=>credential('Bearer a.b'),{code:'UNAUTHENTICATED'});
  assert.throws(()=>credential(undefined),{code:'UNAUTHENTICATED'});
});
