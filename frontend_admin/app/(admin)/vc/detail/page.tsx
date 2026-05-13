"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function VcDetailRoute() {
  const { params } = useQueryIdParams("/vc");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function VcDetailPage() {
  return (
    <Suspense fallback={null}>
      <VcDetailRoute />
    </Suspense>
  );
}
