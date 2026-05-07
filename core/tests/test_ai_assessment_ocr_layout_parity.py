import json

from app.ai_assessment.enums import ApplicantRole, AssessmentStatus, DocumentType, HolderType, LegalEntityType
from app.ai_assessment.schemas import DocumentMetadata, KycApplication
from app.ai_assessment.service import AssessmentService


class FailingLlmProvider:
    provider_name = "failing_llm"

    def extract(self, document):
        raise RuntimeError("forced LLM failure")


def _application(
    app_id="app-poc-parity",
    *,
    legal_entity_type=LegalEntityType.STOCK_COMPANY,
    applicant_role=ApplicantRole.REPRESENTATIVE,
    applicant_name=None,
):
    return KycApplication(
        kycApplicationId=app_id,
        legalEntityType=legal_entity_type,
        applicantRole=applicant_role,
        applicantName=applicant_name,
        businessRegistrationNumber="123-45-67890",
        corporateRegistrationNumber="110111-1234567",
    )


def _document(document_id, document_type, lines):
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId="app-poc-parity",
        originalFileName=f"{document_id}.json",
        mimeType="application/json",
        declaredDocumentType=document_type,
        predictedDocumentType=document_type,
        classificationConfidence=1.0,
        extracted={
            "layout": {
                "pages": [{"page": 1}],
                "evidence": [{"evidenceId": f"{document_id}_ev_{index}", "text": text, "page": 1} for index, text in enumerate(lines, start=1)],
            }
        },
    )


def _document_with_tables(document_id, document_type, lines, rows):
    return DocumentMetadata(
        documentId=document_id,
        kycApplicationId="app-poc-parity",
        originalFileName=f"{document_id}.json",
        mimeType="application/json",
        declaredDocumentType=document_type,
        predictedDocumentType=document_type,
        classificationConfidence=1.0,
        extracted={
            "layout": {
                "pages": [{"page": 1}],
                "evidence": [{"evidenceId": f"{document_id}_ev_{index}", "text": text, "page": 1} for index, text in enumerate(lines, start=1)],
                "tables": [{"rows": rows}],
            }
        },
    )


def _assess(application, documents):
    return AssessmentService(extraction_provider=FailingLlmProvider()).assess(application, documents)


BUSINESS_LINES = [
    "사업자등록증",
    "상호: 주식회사 케이와이씨",
    "사업자등록번호: 123-45-67890",
    "대표자: 김대표",
    "사업장 소재지: 서울특별시 중구 세종대로 1",
    "업태: 서비스",
    "종목: 소프트웨어 개발",
    "개업연월일: 2020-01-01",
    "발급일: 2026-01-10",
]

CORPORATE_LINES = [
    "등기사항전부증명서",
    "상호: 주식회사 케이와이씨",
    "법인등록번호: 110111-1234567",
    "본점: 서울특별시 중구 세종대로 1",
    "대표이사: 김대표",
    "임원: 김대표, 박이사",
    "목적: 소프트웨어 개발업",
    "발급일: 2026-01-11",
]

SHAREHOLDER_LINES = [
    "주주명부",
    "법인명: 주식회사 케이와이씨",
    "기준일: 2026-01-01",
    "총주식수: 100,000",
    "주주명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율",
    "김민수 | 개인 | 1980-01-01 | KR | 30,000 | 30%",
    "이영희 | 개인 | 1979-02-03 | KR | 40,000 | 40%",
    "박철수 | 개인 | 1982-04-05 | KR | 30,000 | 30%",
]


def test_poc_stock_company_fixture_parity_with_llm_failure_fallback():
    assessment = _assess(
        _application(),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("shareholder", DocumentType.SHAREHOLDER_REGISTRY, SHAREHOLDER_LINES),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    business = next(result.extracted for result in assessment.documentResults if result.documentId == "business")
    corporate = next(result.extracted for result in assessment.documentResults if result.documentId == "corporate")
    shareholder = next(result.extracted for result in assessment.documentResults if result.documentId == "shareholder")
    assert next(result.predictedDocumentType for result in assessment.documentResults if result.documentId == "business") == DocumentType.BUSINESS_REGISTRATION
    assert next(result.predictedDocumentType for result in assessment.documentResults if result.documentId == "corporate") == DocumentType.CORPORATE_REGISTRY
    assert next(result.predictedDocumentType for result in assessment.documentResults if result.documentId == "shareholder") == DocumentType.SHAREHOLDER_REGISTRY
    assert business["legalName"]["normalized"] == "케이와이씨"
    assert business["businessRegistrationNumber"]["normalized"] == "1234567890"
    assert business["representativeName"]["normalized"] == "김대표"
    assert business["businessType"]["raw"] == "서비스"
    assert business["businessItem"]["raw"] == "소프트웨어 개발"
    assert business["openingDate"]["normalized"] == "2020-01-01"
    assert corporate["corporateRegistrationNumber"]["normalized"] == "1101111234567"
    assert corporate["directors"]["raw"] == "김대표"
    assert shareholder["legalName"]["normalized"] == "케이와이씨"
    assert shareholder["totalShares"]["normalized"] == 100000
    assert [(item["name"], item["ownershipPercent"]) for item in shareholder["shareholders"]] == [
        ("김민수", 30.0),
        ("이영희", 40.0),
        ("박철수", 30.0),
    ]
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"김민수", "이영희", "박철수"}
    assert all(log.providerName == "ocr_layout_extractor" for log in assessment.providerUsageLogs if log.providerCategory == "EXTRACTOR")


def test_poc_stock_change_statement_uses_shareholder_registry_parity_path():
    assessment = _assess(
        _application("app-stock-change"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("stock_change", DocumentType.STOCK_CHANGE_STATEMENT, SHAREHOLDER_LINES),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    stock_change = next(result.extracted for result in assessment.documentResults if result.documentId == "stock_change")
    assert stock_change["shareholders"][1]["name"] == "이영희"
    assert stock_change["shareholders"][1]["ownershipPercent"] == 40.0


def test_poc_shareholder_ocr_like_rows_infer_holder_type_and_total_from_table_evidence():
    rows = [
        ["주주명", "주민등록번호", "유형", "1주의 금액", "주식수", "납입금액"],
        ["김철수", "020101-0484340", "보통주", "5,000 원", "1,000주", "5,000,000원"],
        ["이영희", "980505-1056789", "보통주", "5,000 원", "400주", "2,000,000원"],
        ["합 계", "2,000주", "10,000,000원"],
    ]

    assessment = _assess(
        _application("app-ocr-like"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document_with_tables("shareholder", DocumentType.SHAREHOLDER_REGISTRY, ["법인명: 주식회사 케이와이씨"], rows),
        ],
    )

    shareholder = next(result.extracted for result in assessment.documentResults if result.documentId == "shareholder")
    assert shareholder["totalShares"]["normalized"] == 2000
    assert shareholder["shareholders"][0]["holderType"] == HolderType.INDIVIDUAL
    assert shareholder["shareholders"][0]["birthDate"] == "020101"
    assert shareholder["shareholders"][0]["shares"] == 1000
    assert shareholder["shareholders"][0]["ownershipPercent"] is None
    assert assessment.status == AssessmentStatus.MANUAL_REVIEW_REQUIRED
    assert any(issue.code == "OWNERSHIP_PERCENT_TOTAL_INVALID" for issue in assessment.manualReviewReasons)


def test_poc_corporate_shareholder_recursive_fixture_parity_with_llm_failure_fallback():
    corporate_shareholder_lines = [
        "주주명부",
        "법인명: 주식회사 케이와이씨",
        "총주식수: 100000",
        "주주명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율",
        "ABC홀딩스 주식회사 | 법인 |  | KR | 30,000 | 30%",
        "이영희 | 개인 | 1979-02-03 | KR | 40,000 | 40%",
        "박철수 | 개인 | 1982-04-05 | KR | 30,000 | 30%",
    ]
    lower_tier_lines = [
        "주주명부",
        "법인명: ABC홀딩스 주식회사",
        "총주식수: 100000",
        "주주명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율",
        "최종민 | 개인 | 1981-03-01 | KR | 80,000 | 80%",
        "소액주주 | 개인 | 1991-09-09 | KR | 20,000 | 20%",
    ]

    assessment = _assess(
        _application("app-recursive"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("shareholder", DocumentType.SHAREHOLDER_REGISTRY, corporate_shareholder_lines),
            _document("abc_holding", DocumentType.SHAREHOLDER_REGISTRY, lower_tier_lines),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"최종민", "이영희", "박철수"}
    assert next(owner for owner in assessment.beneficialOwnership.owners if owner.name == "최종민").ownershipPercent == 24.0


def test_poc_investor_member_and_organization_document_parity():
    investor_lines = [
        "출자자명부",
        "법인명: 유한회사 케이와이씨",
        "총주식수: 100,000",
        "투자자명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율 | 영문명",
        "김투자 | 개인 | 1981-03-01 | KR | 70,000 | 70% | ",
        "박투자 | 개인 | 1984-05-01 | KR | 30,000 | 30% | ",
    ]
    articles_lines = [
        "정관",
        "법인명: 유한회사 케이와이씨",
        "대표자: 김대표",
        "대표자 생년월일: 1970-01-01",
        "대표자 국적: KR",
        "설립목적: 소프트웨어 기반 신원확인 서비스 제공",
        "실소유자명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율 | 영문명",
        "김정관 | 개인 | 1983-01-01 | KR | 100,000 | 100% | ",
    ]
    investor_assessment = _assess(
        _application("app-investor", legal_entity_type=LegalEntityType.LIMITED_COMPANY),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("investor", DocumentType.INVESTOR_REGISTRY, investor_lines),
        ],
    )
    articles_assessment = _assess(
        _application("app-articles", legal_entity_type=LegalEntityType.LIMITED_COMPANY),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("articles", DocumentType.ARTICLES_OF_ASSOCIATION, articles_lines),
        ],
    )

    assert investor_assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in investor_assessment.beneficialOwnership.owners} == {"김투자", "박투자"}
    assert articles_assessment.status == AssessmentStatus.NORMAL
    assert articles_assessment.extractedFields["purposeVerification"]["satisfied"] is True
    assert articles_assessment.beneficialOwnership.owners[0].name == "김정관"


def test_poc_operating_rules_fixture_parity_for_cooperative_scope():
    operating_rules_lines = [
        "운영규정",
        "단체명: 케이와이씨 협동조합",
        "대표자: 김대표",
        "설립목적: 조합원의 공동 사업과 신원확인 서비스 운영",
        "실소유자명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율 | 영문명",
        "조합원A | 개인 | 1985-01-01 | KR | 100,000 | 100% | ",
    ]

    assessment = _assess(
        _application("app-operating-rules", legal_entity_type=LegalEntityType.COOPERATIVE),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("operating_rules", DocumentType.OPERATING_RULES, operating_rules_lines),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert assessment.extractedFields["purposeVerification"]["satisfied"] is True
    assert assessment.beneficialOwnership.owners[0].name == "조합원A"


def test_poc_official_letter_purpose_is_not_accepted_for_purpose_verification():
    official_letter_lines = [
        "공문",
        "단체명: 사단법인 케이와이씨",
        "대표자: 김대표",
        "설립목적: 공익 목적의 디지털 신뢰 인프라 지원",
        "내용: 위 목적을 확인 요청합니다.",
    ]
    member_lines = [
        "사원명부",
        "법인명: 사단법인 케이와이씨",
        "총주식수: 100,000",
        "사원명 | 구분 | 생년월일 | 국적 | 주식수 | 지분율 | 영문명",
        "정사원 | 개인 | 1982-07-01 | KR | 100,000 | 100% | ",
    ]

    assessment = _assess(
        _application("app-purpose", legal_entity_type=LegalEntityType.INCORPORATED_ASSOCIATION),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("member", DocumentType.MEMBER_REGISTRY, member_lines),
            _document("letter", DocumentType.OFFICIAL_LETTER, official_letter_lines),
        ],
    )

    letter = next(result.extracted for result in assessment.documentResults if result.documentId == "letter")
    assert letter["purposeVerification"]["establishmentPurpose"]["raw"] == "공익 목적의 디지털 신뢰 인프라 지원"
    assert letter["purposeVerification"]["acceptableForPurposeVerification"] is False
    assert letter["purposeVerification"]["purposeVerificationSatisfied"] is False
    assert assessment.status == AssessmentStatus.SUPPLEMENT_REQUIRED
    assert any(issue.code == "PURPOSE_VERIFICATION_DOCUMENT_MISSING" for issue in assessment.supplementRequests)


def test_poc_poa_and_seal_certificate_parity_with_llm_failure_fallback():
    poa_lines = [
        "위임장",
        "위임자: 김대표",
        "수임자: 박대리",
        "대상 법인: 주식회사 케이와이씨",
        "위임범위: KYC 신청, 고객확인, 서류 제출, VC 수령, 인증서 수령",
        "유효기간 시작: 2026-01-01",
        "유효기간 종료: 2999-12-31",
        "작성일: 2026-01-02",
        "인감 식별값: SEAL-12345",
        "서명 및 날인 완료",
    ]
    seal_lines = [
        "인감증명서",
        "성명: 김대표",
        "법인명: 주식회사 케이와이씨",
        "증명서번호: SC-2026-0001",
        "인감 식별값: SEAL-12345",
        "발급일: 2026-01-02",
    ]

    assessment = _assess(
        _application("app-delegate", applicant_role=ApplicantRole.DELEGATE, applicant_name="박대리"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            _document("shareholder", DocumentType.SHAREHOLDER_REGISTRY, SHAREHOLDER_LINES),
            _document("poa", DocumentType.POWER_OF_ATTORNEY, poa_lines),
            _document("seal", DocumentType.SEAL_CERTIFICATE, seal_lines),
        ],
    )

    poa = next(result.extracted for result in assessment.documentResults if result.documentId == "poa")
    seal = next(result.extracted for result in assessment.documentResults if result.documentId == "seal")
    assert poa["delegatorName"]["normalized"] == "김대표"
    assert poa["delegateName"]["normalized"] == "박대리"
    assert poa["canApplyKyc"]["normalized"] is True
    assert poa["canSubmitDocuments"]["normalized"] is True
    assert poa["canReceiveVc"]["normalized"] is True
    assert poa["hasSignatureOrSeal"]["normalized"] is True
    assert poa["sealImpressionId"]["normalized"] == "12345"
    assert seal["subjectName"]["normalized"] == "김대표"
    assert seal["corporateName"]["normalized"] == "케이와이씨"
    assert seal["certificateNumber"]["normalized"] == "20260001"
    assert seal["sealImpressionId"]["normalized"] == "12345"
    assert assessment.status == AssessmentStatus.NORMAL
    assert assessment.delegation.result == "AUTHORIZED"


def test_poc_layout_json_file_source_is_supported(tmp_path):
    layout_path = tmp_path / "shareholder.json"
    layout_path.write_text(
        json.dumps(
            {
                "pages": [{"page": 1}],
                "evidence": [{"text": text, "page": 1} for text in SHAREHOLDER_LINES],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    assessment = _assess(
        _application("app-json-layout"),
        [
            _document("business", DocumentType.BUSINESS_REGISTRATION, BUSINESS_LINES),
            _document("corporate", DocumentType.CORPORATE_REGISTRY, CORPORATE_LINES),
            DocumentMetadata(
                documentId="shareholder",
                kycApplicationId="app-json-layout",
                originalFileName="shareholder.json",
                mimeType="application/json",
                declaredDocumentType=DocumentType.SHAREHOLDER_REGISTRY,
                predictedDocumentType=DocumentType.SHAREHOLDER_REGISTRY,
                classificationConfidence=1.0,
                storagePath=str(layout_path),
            ),
        ],
    )

    assert assessment.status == AssessmentStatus.NORMAL
    assert {owner.name for owner in assessment.beneficialOwnership.owners} == {"김민수", "이영희", "박철수"}
