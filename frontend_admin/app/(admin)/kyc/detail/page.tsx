"use client";

import { Suspense } from "react";
import { useQueryId } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function AdminKycDetailRoute() {
  const id = useQueryId("/kyc");
  if (!id) return null;

  return <ClientPage id={id} />;
}

export default function AdminKycDetailPage() {
  return (
    <Suspense fallback={null}>
      <AdminKycDetailRoute />
    </Suspense>
  );
}
