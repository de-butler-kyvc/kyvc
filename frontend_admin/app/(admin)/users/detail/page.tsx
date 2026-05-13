"use client";

import { Suspense } from "react";
import { useQueryIdParams } from "@/lib/navigation/use-query-id";
import ClientPage from "../[id]/ClientPage";

function UserDetailRoute() {
  const { params } = useQueryIdParams("/users");
  if (!params) return null;

  return <ClientPage params={params} />;
}

export default function UserDetailPage() {
  return (
    <Suspense fallback={null}>
      <UserDetailRoute />
    </Suspense>
  );
}
