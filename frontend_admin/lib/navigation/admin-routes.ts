function encodedId(id: string | number) {
  return encodeURIComponent(String(id));
}

export function kycDetailPath(id: string | number) {
  return `/kyc/detail?id=${encodedId(id)}`;
}

export function kycAiResultPath(id: string | number) {
  return `/kyc/ai-result?id=${encodedId(id)}`;
}

export function kycManualReviewPath(id: string | number) {
  return `/kyc/manual-review?id=${encodedId(id)}`;
}

export function kycReReviewPath(id: string | number) {
  return `/kyc/re-review?id=${encodedId(id)}`;
}

export function kycReviewHistoryPath(id: string | number) {
  return `/kyc/review-history?id=${encodedId(id)}`;
}

export function kycSupplementHistoryPath(id: string | number) {
  return `/kyc/supplement-history?id=${encodedId(id)}`;
}

export function kycSupplementRequestPath(id: string | number) {
  return `/kyc/supplement-request?id=${encodedId(id)}`;
}

export function issuerDetailPath(id: string | number) {
  return `/issuer/detail?id=${encodedId(id)}`;
}

export function managerDetailPath(id: string | number) {
  return `/managers/detail?id=${encodedId(id)}`;
}

export function userDetailPath(id: string | number) {
  return `/users/detail?id=${encodedId(id)}`;
}

export function vcDetailPath(id: string | number) {
  return `/vc/detail?id=${encodedId(id)}`;
}

export function vcReissuePath(id: string | number) {
  return `/vc/reissue?id=${encodedId(id)}`;
}

export function vcRevokePath(id: string | number) {
  return `/vc/revoke?id=${encodedId(id)}`;
}

export function verifierDetailPath(id: string | number) {
  return `/verifier/detail?id=${encodedId(id)}`;
}
