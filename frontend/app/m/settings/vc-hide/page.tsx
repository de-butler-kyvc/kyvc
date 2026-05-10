"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MToggle, MTopBar } from "@/components/m/parts";
import { MOCK_CERTS, readHiddenCerts, toggleHiddenCert } from "@/lib/m/data";

export default function MobileSettingsVcHidePage() {
  const router = useRouter();
  const [hidden, setHidden] = useState<string[]>([]);

  useEffect(() => {
    setHidden(readHiddenCerts());
  }, []);

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
        <div className="settings-group mt-16">
          <div className="sg-card">
            {MOCK_CERTS.map((c) => {
              const isHidden = hidden.includes(c.title);
              return (
                <div key={c.title} className="sg-item">
                  <div className={`sg-icon ${isHidden ? "gray" : "blue"}`}>
                    <MIcon.cert />
                  </div>
                  <div className="sg-text">
                    {c.title}
                    {c.title.includes("사업자") ? " (만료 임박)" : ""}
                  </div>
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
