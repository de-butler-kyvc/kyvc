"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function IssuerDetailRoute() {
  const { params } = useQueryIdParams("/issuer");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function IssuerDetailPage() {
  return (
    <Suspense fallback={null}>
      <IssuerDetailRoute />
    </Suspense>
  );
}
