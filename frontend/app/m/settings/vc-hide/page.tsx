"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MToggle, MTopBar } from "@/components/m/parts";
import { ApiError, credentials } from "@/lib/api";
import { readHiddenCerts, toggleHiddenCert } from "@/lib/m/data";

type Item = { title: string; id: number };

export default function MobileSettingsVcHidePage() {
  const router = useRouter();
  const [hidden, setHidden] = useState<string[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setHidden(readHiddenCerts());
    let cancelled = false;
    (async () => {
      try {
        const list = await credentials.list();
        if (cancelled) return;
        setItems(
          list.credentials.map((c) => ({
            title: c.credentialTypeCode ?? `VC #${c.credentialId}`,
            id: c.credentialId,
          })),
        );
      } catch (e) {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/m/login");
          return;
        }
        setError(
          e instanceof ApiError
            ? `VC 목록 조회 실패: ${e.message}`
            : "VC 목록 조회 중 오류가 발생했습니다.",
        );
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [router]);

  const toggle = (title: string) => {
    setHidden(toggleHiddenCert(title));
  };

  return (
    <section className="view wash">
      <MTopBar title="VC 숨김 및 관리" back="/m/settings" />
      <div className="scroll content">
        <p className="subcopy mt-8">
          더 이상 사용하지 않는 증명서를 숨길 수 있습니다. 숨긴 증명서는 홈 화면에 나타나지 않습니다.
        </p>
        {error ? <p className="m-error mt-16">{error}</p> : null}
        {loading ? <p className="m-loading mt-16">불러오는 중…</p> : null}
        {!loading && !error && items.length === 0 ? (
          <p
            className="subcopy"
            style={{ textAlign: "center", padding: "32px 0" }}
          >
            발급된 증명서가 없습니다.
          </p>
        ) : null}

        {items.length > 0 ? (
          <div className="settings-group mt-16">
            <div className="sg-card">
              {items.map((c) => {
                const isHidden = hidden.includes(c.title);
                return (
                  <div key={c.id} className="sg-item">
                    <div className={`sg-icon ${isHidden ? "gray" : "blue"}`}>
                      <MIcon.cert />
                    </div>
                    <div className="sg-text">{c.title}</div>
                    <div className="sg-right">
                      <MToggle
                        active={!isHidden}
                        onClick={() => toggle(c.title)}
                      />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        ) : null}
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary"
          onClick={() => router.push("/m/settings")}
        >
          저장하기
        </button>
      </div>
    </section>
  );
}
