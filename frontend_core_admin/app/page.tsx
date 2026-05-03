const environment = process.env.NEXT_PUBLIC_KYVC_ENV ?? "local";
const appName = "front-core-admin";

export default function Home() {
  return (
    <main className="container">
      <h1>KYvC Front Core Admin</h1>
      <p>environment: {environment}</p>
      <p>app: {appName}</p>
    </main>
  );
}
