"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { kycManualReviewPath } from "@/lib/navigation/admin-routes";

function AdminReviewDetailRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const id = searchParams.get("id");
    router.replace(id ? kycManualReviewPath(id) : "/kyc");
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
