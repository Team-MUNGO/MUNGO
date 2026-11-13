import { ApiError, hash, newToken } from './assistant.js';

/** Only hashes of invitation/session credentials are stored. All counters are transactional. */
export class AssistantStore {
  constructor(db) { this.db = db; }
  async bootstrapAttempt(ipHash, now) {
    const ref = this.db.collection('assistantBootstrap').doc(`${ipHash}-${Math.floor(now/3600000)}`);
    await this.db.runTransaction(async tx => {
      const data=(await tx.get(ref)).data();
      if ((data?.count ?? 0) >= 10) throw new ApiError(429,'RATE_LIMITED','연결 시도가 많아요. 잠시 후 다시 시도해 주세요.');
      tx.set(ref,{count:(data?.count ?? 0)+1,expiresAt:new Date(now+7200000)});
    });
  }
  async exchange(invitation, now) {
    if (typeof invitation !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(invitation)) throw new ApiError(401,'INVALID_INVITATION','연결 코드를 확인해 주세요.');
    const token=newToken(); const expiresAt=now+3600000;
    const invitationRef=this.db.collection('assistantInvitations').doc(hash(invitation));
    const sessionRef=this.db.collection('assistantSessions').doc(hash(token));
    await this.db.runTransaction(async tx => {
      const data=(await tx.get(invitationRef)).data();
      if (!data || data.used || data.expiresAt.toMillis() <= now) throw new ApiError(401,'INVALID_INVITATION','연결 코드가 만료되었거나 사용되었어요.');
      tx.update(invitationRef,{used:true});
      tx.set(sessionRef,{deviceId:data.deviceId,expiresAt:new Date(expiresAt),revoked:false});
    });
    return {token,expiresAt};
  }
  async reserve(token, now) {
    const sessionRef=this.db.collection('assistantSessions').doc(hash(token));
    await this.db.runTransaction(async tx => {
      const session=(await tx.get(sessionRef)).data();
      if (!session || session.revoked || session.expiresAt.toMillis() <= now) throw new ApiError(401,'UNAUTHENTICATED','AI 연결이 만료되었어요. 정차 후 다시 연결해 주세요.');
      await this.consume(tx,[
        [`device-${hash(session.deviceId)}-${Math.floor(now/60000)}`,5],
        [`devicehour-${hash(session.deviceId)}-${Math.floor(now/3600000)}`,60]
      ],now);
    });
  }
  /** Firebase Auth users (anonymous judge installs): per-user, per-hashed-IP and shared project budgets. */
  async reserveUser(uid, ipHash, now) {
    if (typeof uid !== 'string' || !uid) throw new ApiError(401,'UNAUTHENTICATED','AI 연결이 만료되었어요. 다시 연결해 주세요.');
    await this.db.runTransaction(async tx => this.consume(tx,[
      [`user-${hash(uid)}-${Math.floor(now/60000)}`,5],
      [`userhour-${hash(uid)}-${Math.floor(now/3600000)}`,60],
      [`iphour-${ipHash}-${Math.floor(now/3600000)}`,120]
    ],now));
  }
  /** Separate daily cap on billable Maps searches (Essentials SKU), independent of Gemini request budgets. */
  async reserveMaps(now, max=300) {
    const ref=this.db.collection('assistantLimits').doc(`maps-${Math.floor(now/86400000)}`);
    await this.db.runTransaction(async tx => {
      const count=(await tx.get(ref)).data()?.count ?? 0;
      if (count >= max) throw new ApiError(429,'MAPS_LIMITED','오늘 장소 검색 한도에 도달했어요.');
      tx.set(ref,{count:count+1,expiresAt:new Date(now+172800000)});
    });
  }
  /** Separate daily cap on billable reverse-geocoding calls (Korean addresses, up to 5 per search). */
  async reserveGeocode(now, count, max=1500) {
    const ref=this.db.collection('assistantLimits').doc(`geocode-${Math.floor(now/86400000)}`);
    await this.db.runTransaction(async tx => {
      const used=(await tx.get(ref)).data()?.count ?? 0;
      if (used+count > max) throw new ApiError(429,'GEOCODE_LIMITED','오늘 주소 확인 한도에 도달했어요.');
      tx.set(ref,{count:used+count,expiresAt:new Date(now+172800000)});
    });
  }
  async consume(tx, limits, now) {
    const refs=[...limits,[`project-${Math.floor(now/86400000)}`,500]].map(([id,max])=>[this.db.collection('assistantLimits').doc(id),max]);
    const data=await Promise.all(refs.map(([ref])=>tx.get(ref)));
    if (data.some((item,i)=>(item.data()?.count ?? 0) >= refs[i][1])) throw new ApiError(429,'RATE_LIMITED','요청이 많아요. 잠시 후 다시 질문해 주세요.');
    refs.forEach(([ref],i)=>tx.set(ref,{count:(data[i].data()?.count ?? 0)+1,expiresAt:new Date(now+172800000)}));
  }
}
