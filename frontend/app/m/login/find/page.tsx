"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { MIcon } from "@/components/m/icons";
import { MTopBar } from "@/components/m/parts";
import { bridge, isBridgeAvailable } from "@/lib/m/android-bridge";

type Tab = "business" | "email";

export default function MobileFindAccountPage() {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("business");
  const [value, setValue] = useState("");
  const [done, setDone] = useState(false);

  // 계정 찾기 = 이메일 인증으로 잠금 해제도 겸함.
  // 성공 시 Android 측 5회 실패 카운터를 초기화.
  const onLoginAfterFound = async () => {
    if (isBridgeAvailable()) {
      try {
        await bridge.completeEmailVerification();
      } catch {
        /* 무시 — 로그인 화면에서 재시도 가능 */
      }
    }
    router.push("/m/login");
  };

  if (done) {
    return (
      <section className="view find-view">
        <MTopBar title="계정 찾기" back="/m/login" />
        <div className="content find-result scroll">
          <div className="result-check">
            <MIcon.check />
          </div>
          <h1>계정을 찾았습니다</h1>
          <p>
            입력하신 정보와 일치하는
            <br />
            계정 정보를 확인하세요.
          </p>
          <dl className="account-card">
            <div>
              <dt>법인명</dt>
              <dd>(주)테크노바</dd>
            </div>
            <div>
              <dt>사업자번호</dt>
              <dd>123-45-***90</dd>
            </div>
            <div>
              <dt>이메일</dt>
              <dd>hong***@techno.co.kr</dd>
            </div>
            <div>
              <dt>가입일</dt>
              <dd>2024.03.15</dd>
            </div>
          </dl>
        </div>
        <div className="bottom-action">
          <button
            type="button"
            className="primary dark"
            onClick={onLoginAfterFound}
          >
            로그인 하기
          </button>
          <button
            type="button"
            className="secondary"
            onClick={() => setDone(false)}
          >
            돌아가기
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="view find-view">
      <MTopBar title="계정 찾기" back="/m/login" />
      <div className="content find-content scroll">
        <h1 className="find-title">
          계정 정보를
          <br />
          확인해드릴게요
        </h1>
        <p className="find-desc">가입 시 입력한 정보로 계정을 찾을 수 있습니다.</p>

        <div className="find-tabs">
          <button
            type="button"
            className={tab === "business" ? "active" : ""}
            onClick={() => setTab("business")}
          >
            사업자번호
          </button>
          <button
            type="button"
            className={tab === "email" ? "active" : ""}
            onClick={() => setTab("email")}
          >
            이메일
          </button>
        </div>

        <label className="find-label">
          {tab === "email" ? <MIcon.mail /> : <MIcon.building />}
          {tab === "email" ? " 이메일 주소 입력" : " 사업자번호 입력"}
        </label>
        <div className="find-input">
          <input
            inputMode={tab === "email" ? "email" : "numeric"}
            placeholder={tab === "email" ? "example@company.com" : "000-00-00000"}
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
        </div>
        <p className="find-helper">
          {tab === "email"
            ? "가입 시 등록한 이메일 주소를 입력해주세요."
            : "사업자번호 10자리를 입력해주세요."}
        </p>

        <div className="find-note">
          <p>· 법인 KYvC 계정은 사업자번호 기준으로 생성됩니다.</p>
          <p>· 계정 찾기 후 비밀번호 재설정이 가능합니다.</p>
          <p>· 추가 문의는 고객센터로 연락해주세요.</p>
        </div>
      </div>
      <div className="bottom-action">
        <button
          type="button"
          className="primary dark"
          onClick={() => setDone(true)}
        >
          계정 찾기
        </button>
      </div>
    </section>
  );
}
