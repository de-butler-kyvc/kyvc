"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function AdminKycDetailRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const id = searchParams.get("id");
    router.replace(id ? `/kyc/${encodeURIComponent(id)}` : "/kyc");
  }, [router, searchParams]);

  return null;
}

export default function AdminKycDetailPage() {
  return (
    <Suspense fallback={null}>
      <AdminKycDetailRedirect />
    </Suspense>
  );
}
