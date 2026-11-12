// End-to-end smoke test of the judge path: Firebase anonymous sign-in -> /answer -> Gemini.
// Usage: POCO_FIREBASE_API_KEY=AIza... POCO_ASSISTANT_URL=https://.../assistantApi node scripts/smoke-anonymous.js
// Prints only HTTP status, error codes and the answer text. Never prints ID/refresh tokens.
const key = process.env.POCO_FIREBASE_API_KEY;
const base = (process.env.POCO_ASSISTANT_URL ?? '').replace(/\/$/, '');
if (!key || !/^https:\/\//.test(base)) throw new Error('Set POCO_FIREBASE_API_KEY and POCO_ASSISTANT_URL.');
const signUp = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${key}`, {
  method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({returnSecureToken:true})
});
const auth = await signUp.json();
if (!signUp.ok) { console.log(`anonymous sign-in failed: ${signUp.status} ${auth.error?.message}`); process.exit(1); }
console.log(`anonymous sign-in: ${signUp.status}`);
const question = process.argv[2] ?? '지금 차 안 온도는 반려견에게 괜찮아?';
const res = await fetch(`${base}/answer`, {
  method:'POST', headers:{'Content-Type':'application/json', Authorization:`Bearer ${auth.idToken}`},
  body:JSON.stringify({question, mode:'status', context:{score:92, elapsedMinutes:35, temperatureC:27.5, minTemperatureC:20, maxTemperatureC:25, windowPercent:0}})
});
const body = await res.json();
console.log(`answer: ${res.status}`, res.ok ? body.answerText : body.code);
process.exit(res.ok ? 0 : 1);
