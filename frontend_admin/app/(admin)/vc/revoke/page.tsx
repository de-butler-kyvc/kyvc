"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/revoke/ClientPage";

function VcRevokeRoute() {
  const { params } = useQueryIdParams("/vc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function VcRevokePage() {
  return (
    <Suspense fallback={null}>
      <VcRevokeRoute />
    </Suspense>
  );
}
