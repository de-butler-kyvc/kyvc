const environment = process.env.NEXT_PUBLIC_KYVC_ENV ?? "local";
const appName = "front";

export default function Home() {
  return (
    <main className="container">
      <h1>KYvC Front</h1>
      <p>environment: {environment}</p>
      <p>app: {appName}</p>
    </main>
  );
}
