import { redirect } from "next/navigation";

export default async function AdminReviewDetailPage({
  searchParams,
}: {
  searchParams?: Promise<{ id?: string }>;
}) {
  const id = (await searchParams)?.id;
  redirect(id ? `/kyc/${encodeURIComponent(id)}/manual-review` : "/kyc");
}
