"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/review-history/ClientPage";

function KycReviewHistoryRoute() {
  const { params } = useQueryIdParams("/kyc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function KycReviewHistoryPage() {
  return (
    <Suspense fallback={null}>
      <KycReviewHistoryRoute />
    </Suspense>
  );
}
