"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { TextField } from "@/components/ui/text-field";
import { ApiError, auth, session } from "@/lib/api";

type LoginForm = { email: string; password: string };

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<LoginForm>({ defaultValues: { email: "", password: "" } });

  const onSubmit = handleSubmit(async ({ email, password }) => {
    setError(null);
    try {
      const res = await auth.login(email, password);
      session.set(res.accessToken, res.refreshToken);
      router.push("/corporate");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "로그인에 실패했습니다.");
    }
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 px-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>KYvC 로그인</CardTitle>
          <CardDescription>법인 사용자 / 금융사 / 모바일 Wallet 공용 로그인</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="grid gap-4" noValidate>
            <TextField
              label="이메일"
              type="email"
              required
              error={errors.email?.message}
              {...register("email", {
                required: "이메일을 입력해 주세요",
                pattern: { value: /\S+@\S+\.\S+/, message: "이메일 형식이 올바르지 않습니다" }
              })}
            />
            <TextField
              label="비밀번호"
              type="password"
              required
              error={errors.password?.message}
              {...register("password", { required: "비밀번호를 입력해 주세요" })}
            />
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "로그인 중..." : "로그인"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
