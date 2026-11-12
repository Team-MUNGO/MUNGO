import test from 'node:test';
import assert from 'node:assert/strict';
import { AssistantStore } from '../src/store.js';
import { hash, newToken } from '../src/assistant.js';
// Transaction test double: serial execution, staged writes and commit only on success.
function memoryDb() {
  const data=new Map(); let tail=Promise.resolve();
  const db={data,collection:name=>({doc:id=>({key:`${name}/${id}`})}),runTransaction:fn=>{
    const result=tail.then(async()=>{
      const writes=[];
      const value=await fn({get:async ref=>({data:()=>data.get(ref.key)}),set:(ref,value)=>writes.push([ref.key,value]),update:(ref,value)=>writes.push([ref.key,{...data.get(ref.key),...value}])});
      writes.forEach(([key,value])=>{if(value.expiresAt instanceof Date) {const ms=value.expiresAt.getTime();value={...value,expiresAt:{toMillis:()=>ms}};}data.set(key,value);});
      return value;
    });tail=result.catch(()=>{});return result;
  }}; return db;
}
const now=Date.parse('2026-11-12T00:00:00Z');
function invite(db,code,expiresAt=now+1000) {db.data.set(`assistantInvitations/${hash(code)}`,{deviceId:'fixture-device',used:false,expiresAt:{toMillis:()=>expiresAt}});}
test('invitation exchanges exactly once even for concurrent attempts; secret not stored',async()=>{
  const db=memoryDb(),store=new AssistantStore(db),code=newToken(); invite(db,code);
  const results=await Promise.allSettled([store.exchange(code,now),store.exchange(code,now)]);
  assert.equal(results.filter(r=>r.status==='fulfilled').length,1);
  const session=results.find(r=>r.status==='fulfilled').value;
  assert.equal(session.expiresAt,now+3600000);
  assert(db.data.has(`assistantSessions/${hash(session.token)}`));
  assert(!JSON.stringify([...db.data]).includes(session.token));
});
test('expired/missing invitations and expired/revoked sessions rejected',async()=>{
  const db=memoryDb(),store=new AssistantStore(db),code=newToken();invite(db,code,now);
  await assert.rejects(store.exchange(code,now),{code:'INVALID_INVITATION'});
  await assert.rejects(store.exchange([code],now),{code:'INVALID_INVITATION'});
  await assert.rejects(store.reserve(newToken(),now),{code:'UNAUTHENTICATED'});
  for (const revoked of [true,false]) {
    const token=newToken();db.data.set(`assistantSessions/${hash(token)}`,{deviceId:'fixture',revoked,expiresAt:{toMillis:()=>revoked?now+1000:now}});
    await assert.rejects(store.reserve(token,now),{code:'UNAUTHENTICATED'});
  }
});
test('device minute budget atomically permits five requests, rejects sixth; next minute permits',async()=>{
  const db=memoryDb(),store=new AssistantStore(db),code=newToken();invite(db,code);const {token}=await store.exchange(code,now);
  const results=await Promise.allSettled(Array.from({length:8},()=>store.reserve(token,now)));
  assert.equal(results.filter(r=>r.status==='fulfilled').length,5);
  assert.equal(results.filter(r=>r.status==='rejected'&&r.reason.code==='RATE_LIMITED').length,3);
  await store.reserve(token,now+60000);
});
test('hour and global budgets cannot be bypassed with new sessions',async()=>{
  for (const key of [`devicehour-${hash('fixture-device')}-${Math.floor(now/3600000)}`,`project-${Math.floor(now/86400000)}`]) {
    const db=memoryDb(),store=new AssistantStore(db),code=newToken();invite(db,code);const {token}=await store.exchange(code,now);
    db.data.set(`assistantLimits/${key}`,{count:key.startsWith('devicehour')?60:500});
    await assert.rejects(store.reserve(token,now),{code:'RATE_LIMITED'});
  }
});
test('bootstrap attempts bounded per hashed IP/hour',async()=>{
  const store=new AssistantStore(memoryDb());
  for(let i=0;i<10;i++) await store.bootstrapAttempt('test-ip-hash',now);
  await assert.rejects(store.bootstrapAttempt('test-ip-hash',now),{code:'RATE_LIMITED'});
  await store.bootstrapAttempt('test-ip-hash',now+3600000);
});
test('Firebase user budget: 5/min per uid, per-IP hour cap and shared project cap',async()=>{
  const db=memoryDb(),store=new AssistantStore(db);
  const results=await Promise.allSettled(Array.from({length:7},()=>store.reserveUser('uid-a','ip',now)));
  assert.equal(results.filter(r=>r.status==='fulfilled').length,5);
  await store.reserveUser('uid-b','ip',now);
  assert(![...db.data.keys()].some(k=>k.includes('uid-a')),'raw uid must not be stored');
  db.data.set(`assistantLimits/iphour-ip2-${Math.floor(now/3600000)}`,{count:120});
  await assert.rejects(store.reserveUser('uid-c','ip2',now),{code:'RATE_LIMITED'});
  db.data.set(`assistantLimits/project-${Math.floor(now/86400000)}`,{count:500});
  await assert.rejects(store.reserveUser('uid-d','ip3',now),{code:'RATE_LIMITED'});
  await assert.rejects(store.reserveUser('','ip3',now),{code:'UNAUTHENTICATED'});
});
