"use client";

import { useEffect, useMemo } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export function useQueryId(fallbackPath: string) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const id = searchParams.get("id");

  useEffect(() => {
    if (!id) router.replace(fallbackPath);
  }, [fallbackPath, id, router]);

  return id;
}

export function useQueryIdParams(fallbackPath: string) {
  const id = useQueryId(fallbackPath);
  const params = useMemo(() => (id ? Promise.resolve({ id }) : null), [id]);

  return { id, params };
}
