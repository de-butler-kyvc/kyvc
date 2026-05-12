"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ai-result/ClientPage";

function KycAiResultRoute() {
  const { params } = useQueryIdParams("/kyc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function KycAiResultPage() {
  return (
    <Suspense fallback={null}>
      <KycAiResultRoute />
    </Suspense>
  );
}
