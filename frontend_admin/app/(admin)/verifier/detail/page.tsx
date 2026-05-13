"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function VerifierDetailRoute() {
  const { params } = useQueryIdParams("/verifier");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function VerifierDetailPage() {
  return (
    <Suspense fallback={null}>
      <VerifierDetailRoute />
    </Suspense>
  );
}
