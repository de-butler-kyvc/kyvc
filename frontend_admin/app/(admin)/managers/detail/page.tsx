"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function ManagerDetailRoute() {
  const { params } = useQueryIdParams("/managers");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function ManagerDetailPage() {
  return (
    <Suspense fallback={null}>
      <ManagerDetailRoute />
    </Suspense>
  );
}
