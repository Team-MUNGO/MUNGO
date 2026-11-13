import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { randomBytes } from 'node:crypto';
import { hash } from '../src/assistant.js';
if (!process.env.GOOGLE_CLOUD_PROJECT) throw new Error('Set GOOGLE_CLOUD_PROJECT explicitly.');
initializeApp({credential:applicationDefault(),projectId:process.env.GOOGLE_CLOUD_PROJECT});
const code=randomBytes(32).toString('base64url');
await getFirestore().collection('assistantInvitations').doc(hash(code)).create({deviceId:randomBytes(16).toString('hex'),used:false,expiresAt:new Date(Date.now()+1800000)});
// This is a secret provisioning output: display only in the operator's terminal, never CI logs.
process.stdout.write(code+'\n');
