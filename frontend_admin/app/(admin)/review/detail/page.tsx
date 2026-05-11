"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function AdminReviewDetailRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const id = searchParams.get("id");
    router.replace(id ? `/kyc/${encodeURIComponent(id)}/manual-review` : "/kyc");
  }, [router, searchParams]);

  return null;
}

export default function AdminReviewDetailPage() {
  return (
    <Suspense fallback={null}>
      <AdminReviewDetailRedirect />
    </Suspense>
  );
}
