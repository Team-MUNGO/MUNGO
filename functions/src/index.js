import { onRequest } from 'firebase-functions/v2/https';
import { defineSecret, defineString } from 'firebase-functions/params';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getAuth } from 'firebase-admin/auth';
import { createHmac } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { ApiError, credential, exactKeys, generateAnswer, validateQuestion } from './assistant.js';
import { AssistantStore } from './store.js';
import { assist, validateAssist } from './assist.js';
import { summarize, validateSummary } from './summary.js';
initializeApp();
const geminiKey=defineSecret('GEMINI_API_KEY');
const ipSalt=defineSecret('ASSISTANT_IP_SALT');
const mapsKey=defineSecret('GOOGLE_MAPS_API_KEY');
const model=defineString('GEMINI_MODEL');
const serviceAccount=defineString('ASSISTANT_SERVICE_ACCOUNT');
const store=new AssistantStore(getFirestore());
const auth=getAuth();
const catalog=JSON.parse(readFileSync(new URL('../data/rest-stops.json',import.meta.url),'utf8'));
export const assistantApi=onRequest({region:'asia-northeast3',serviceAccount:serviceAccount,invoker:'public', timeoutSeconds:30, memory:'256MiB',maxInstances:2,concurrency:10,secrets:[geminiKey,ipSalt,mapsKey],cors:false},async(req,res)=>{
  res.set('Cache-Control','no-store');
  res.set('X-Content-Type-Options','nosniff');
  try {
    if (req.method !== 'POST') throw new ApiError(405,'METHOD_NOT_ALLOWED','지원하지 않는 요청이에요.');
    if (!req.is('application/json') || (req.rawBody?.length ?? 0)>8192) throw new ApiError(400,'INVALID_INPUT','요청 형식을 확인해 주세요.');
    const path=req.path.replace(/\/$/,'');
    const now=Date.now();
    const ipHash=()=>createHmac('sha256',ipSalt.value()).update(req.ip ?? req.socket.remoteAddress ?? 'unknown').digest('hex');
    if (path === '/session') {
      // Development/operator path: one-time invitation -> 1h opaque session.
      await store.bootstrapAttempt(ipHash(),now);
      exactKeys(req.body,['invitation']);
      res.json(await store.exchange(req.body.invitation,now));
    } else if (path === '/answer' || path === '/assist' || path === '/summary') {
      const cred=credential(req.get('authorization'));
      const structured=path === '/assist';
      const summary=path === '/summary';
      const input=summary ? validateSummary(req.body) : structured ? validateAssist(req.body) : validateQuestion(req.body);
      if (cred.kind === 'firebase') {
        // Judge/app path: Firebase Auth (anonymous) ID token, signature/audience/expiry verified by Admin SDK.
        let uid;
        try { uid=(await auth.verifyIdToken(cred.token)).uid; } catch { throw new ApiError(401,'UNAUTHENTICATED','AI 연결이 만료되었어요. 다시 연결해 주세요.'); }
        await store.reserveUser(uid,ipHash(),now);
      } else await store.reserve(cred.token,now);
      if (summary) {
        res.json(await summarize(input,{geminiKey:geminiKey.value(),model:model.value()}));
      } else if (structured) {
        res.json(await assist(input,{geminiKey:geminiKey.value(),mapsKey:mapsKey.value(),model:model.value(),reserveMaps:()=>store.reserveMaps(now),reserveGeocode:count=>store.reserveGeocode(now,count)}));
      } else {
        res.json(await generateAnswer(input,{apiKey:geminiKey.value(),model:model.value(),catalog,now}));
      }
    } else throw new ApiError(404,'NOT_FOUND','요청 주소를 확인해 주세요.');
  } catch (error) {
    // Never log questions, coordinates, credentials, upstream URLs or response bodies.
    const publicError=error instanceof ApiError ? error : new ApiError(503,'SERVICE_UNAVAILABLE','연결을 확인하고 다시 시도해 주세요.');
    res.status(publicError.status).json({code:publicError.code,message:publicError.message});
  }
});
