const environment = process.env.NEXT_PUBLIC_KYVC_ENV ?? "local";
const appName = "front-admin";

import { redirect } from "next/navigation";

export default function RootPage() {
  redirect("/login");
}