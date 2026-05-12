"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/supplement-history/ClientPage";

function KycSupplementHistoryRoute() {
  const { params } = useQueryIdParams("/kyc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function KycSupplementHistoryPage() {
  return (
    <Suspense fallback={null}>
      <KycSupplementHistoryRoute />
    </Suspense>
  );
}
