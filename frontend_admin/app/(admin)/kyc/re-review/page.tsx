"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/re-review/ClientPage";

function KycReReviewRoute() {
  const { params } = useQueryIdParams("/kyc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function KycReReviewPage() {
  return (
    <Suspense fallback={null}>
      <KycReReviewRoute />
    </Suspense>
  );
}
