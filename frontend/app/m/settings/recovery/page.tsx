"use client";

import { useRouter } from "next/navigation";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";

export default function MobileSettingsRecoveryPage() {
  const router = useRouter();
  return (
    <section className="view wash">
      <MTopBar title="기기 변경 및 복구" back="/m/settings" />
      <div className="scroll content">
        <h1 className="headline m-auth-title">
          안전한 지갑 복구를 위한
          <br />
          안내입니다
        </h1>
        <p className="subcopy">
          기기 변경 또는 앱 삭제 후 복구하려면 백업된 복구 문구(Seed Phrase)가 필요합니다.
        </p>

        <div className="settings-group mt-24">
          <div className="sg-card">
            <div
              className="sg-item"
              onClick={() => router.push("/m/settings/seed-backup")}
            >
              <div className="sg-icon green">
                <MIcon.shield />
              </div>
              <div className="sg-text">복구 문구 확인 및 백업</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
            <div
              className="sg-item"
              onClick={() => router.push("/m/settings/seed-test")}
            >
              <div className="sg-icon blue">
                <MIcon.user />
              </div>
              <div className="sg-text">복구 테스트</div>
              <div className="sg-right">
                <MIcon.chevronRight />
              </div>
            </div>
          </div>
        </div>

        <div className="m-info-box info-box mt-24">
          <div className="info-icon">
            <MIcon.help />
          </div>
          <div className="info-text">
            <strong>주의사항</strong>
            <p>
              복구 문구를 타인에게 노출하지 마세요. KYvC는 어떠한 경우에도 복구
              문구를 요구하지 않습니다.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
