"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode
} from "react";

import {
  auth,
  corporate,
  type CorporateProfile,
  type SessionResponse
} from "@/lib/api";
import { clearKyvcLocalStorage, syncKyvcSessionUser } from "@/lib/kyc-flow";

type SessionState = {
  session: SessionResponse | null;
  profile: CorporateProfile | null;
  loading: boolean;
  refreshSession: () => Promise<SessionResponse | null>;
  refreshProfile: () => Promise<CorporateProfile | null>;
};

const SessionCtx = createContext<SessionState | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [profile, setProfile] = useState<CorporateProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshSession = useCallback(async () => {
    try {
      const s = await auth.session();
      if (s?.authenticated) {
        syncKyvcSessionUser(s.userId);
      } else {
        clearKyvcLocalStorage();
      }
      setSession(s);
      return s;
    } catch {
      clearKyvcLocalStorage();
      setSession(null);
      return null;
    }
  }, []);

  const refreshProfile = useCallback(async () => {
    try {
      const p = await corporate.me();
      setProfile(p);
      return p;
    } catch {
      setProfile(null);
      return null;
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const s = await refreshSession();
      if (cancelled) return;
      if (s?.authenticated && s.corporateRegistered) {
        await refreshProfile();
      } else {
        setProfile(null);
      }
      if (!cancelled) setLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [refreshSession, refreshProfile]);

  return (
    <SessionCtx.Provider
      value={{ session, profile, loading, refreshSession, refreshProfile }}
    >
      {children}
    </SessionCtx.Provider>
  );
}

export function useSession() {
  const ctx = useContext(SessionCtx);
  if (!ctx) throw new Error("useSession must be used inside <SessionProvider>");
  return ctx;
}

/** 현재 로그인된 법인 사용자 정보(corporate.me) 캐시 */
export function useCorporateProfile() {
  const { profile, loading, refreshProfile } = useSession();
  return { profile, loading, refresh: refreshProfile };
}
