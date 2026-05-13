"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/reissue/ClientPage";

function VcReissueRoute() {
  const { params } = useQueryIdParams("/vc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function VcReissuePage() {
  return (
    <Suspense fallback={null}>
      <VcReissueRoute />
    </Suspense>
  );
}
