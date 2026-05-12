"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/supplement-request/ClientPage";

function KycSupplementRequestRoute() {
  const { params } = useQueryIdParams("/kyc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function KycSupplementRequestPage() {
  return (
    <Suspense fallback={null}>
      <KycSupplementRequestRoute />
    </Suspense>
  );
}
