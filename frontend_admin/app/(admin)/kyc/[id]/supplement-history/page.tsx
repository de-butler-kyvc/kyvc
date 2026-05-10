import ClientPage from "./ClientPage";

export function generateStaticParams() {
  return [{ id: "placeholder" }];
}

export default function Page({ params }: { params: Promise<{ id: string }> }) {
  return <ClientPage params={params} />;
}