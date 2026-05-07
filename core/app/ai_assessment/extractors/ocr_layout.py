import re
from dataclasses import dataclass
from typing import Iterable

from app.ai_assessment.engines.normalizer import Normalizer
from app.ai_assessment.enums import DocumentType, HolderType
from app.ai_assessment.schemas import (
    BusinessRegistrationExtraction,
    CorporateRegistryExtraction,
    ExtractedValue,
    OrganizationDocumentExtraction,
    PersonExtraction,
    PowerOfAttorneyExtraction,
    PurposeVerificationExtraction,
    SealCertificateExtraction,
    Shareholder,
    ShareholderRegistryExtraction,
)


@dataclass(frozen=True)
class LayoutLine:
    evidence_id: str
    text: str
    confidence: float = 0.85


class OcrLayoutDeterministicExtractor:
    OWNERSHIP_TYPES = {
        DocumentType.SHAREHOLDER_REGISTRY,
        DocumentType.STOCK_CHANGE_STATEMENT,
        DocumentType.INVESTOR_REGISTRY,
        DocumentType.MEMBER_REGISTRY,
        DocumentType.BENEFICIAL_OWNER_PROOF_DOCUMENT,
    }
    ORGANIZATION_TYPES = {
        DocumentType.ARTICLES_OF_ASSOCIATION,
        DocumentType.OPERATING_RULES,
        DocumentType.REGULATIONS,
        DocumentType.MEETING_MINUTES,
        DocumentType.OFFICIAL_LETTER,
        DocumentType.PURPOSE_PROOF_DOCUMENT,
        DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE,
        DocumentType.INVESTMENT_REGISTRATION_CERTIFICATE,
        DocumentType.BOARD_REGISTRY,
    }

    def __init__(self, normalizer: Normalizer | None = None) -> None:
        self.normalizer = normalizer or Normalizer()

    def extract(self, document_type: DocumentType, text: str | None):
        layout = self._layout(text)
        if not layout:
            return None
        if document_type == DocumentType.BUSINESS_REGISTRATION:
            return self._business_registration(layout)
        if document_type == DocumentType.CORPORATE_REGISTRY:
            return self._corporate_registry(layout)
        if document_type in self.OWNERSHIP_TYPES:
            return self._shareholder_registry(layout)
        if document_type == DocumentType.POWER_OF_ATTORNEY:
            return self._power_of_attorney(layout)
        if document_type == DocumentType.SEAL_CERTIFICATE:
            return self._seal_certificate(layout)
        if document_type in self.ORGANIZATION_TYPES:
            return self._organization_document(document_type, layout)
        return None

    def _business_registration(self, layout: list[LayoutLine]) -> BusinessRegistrationExtraction:
        result = BusinessRegistrationExtraction()
        result.legalName = self._company_value(layout, ["상호", "법인명", "회사명", "Company", "Legal name"])
        result.businessRegistrationNumber = self._digits_value(layout, ["사업자등록번호", "Business registration number", "BRN"])
        result.representativeName = self._person_value(layout, ["대표자", "대표자명", "Representative"])
        result.representative = self._representative(result.representativeName, layout)
        result.businessAddress = self._line_value(layout, ["사업장 소재지", "사업장주소", "주소", "Business address"])
        result.issueDate = self._date_value(layout, ["발급일", "교부일", "Issue date"])
        result.sealImpressionId = self._seal_value(layout)
        return result

    def _corporate_registry(self, layout: list[LayoutLine]) -> CorporateRegistryExtraction:
        result = CorporateRegistryExtraction()
        result.legalName = self._company_value(layout, ["상호", "법인명", "회사명", "Company", "Legal name"])
        result.corporateRegistrationNumber = self._digits_value(layout, ["법인등록번호", "Corporate registration number", "CRN"])
        result.representativeName = self._person_value(layout, ["대표자", "대표자명", "Representative"])
        result.representative = self._representative(result.representativeName, layout)
        result.headOfficeAddress = self._line_value(layout, ["본점", "본점소재지", "주소", "Head office address"])
        result.purpose = self._line_value(layout, ["목적", "사업목적", "Purpose"])
        result.issueDate = self._date_value(layout, ["발급일", "교부일", "Issue date"])
        result.sealImpressionId = self._seal_value(layout)
        return result

    def _shareholder_registry(self, layout: list[LayoutLine]) -> ShareholderRegistryExtraction:
        result = ShareholderRegistryExtraction()
        result.legalName = self._company_value(layout, ["법인명", "상호", "회사명", "Company"])
        result.baseDate = self._date_value(layout, ["기준일", "작성기준일", "Base date"])
        result.totalShares = self._integer_value(layout, ["총주식수", "발행주식총수", "Total shares"])
        result.shareholders = self._shareholders(layout)
        if result.totalShares.normalized is None:
            inferred_total = sum(shareholder.shares or 0 for shareholder in result.shareholders) or None
            if inferred_total is not None:
                result.totalShares = ExtractedValue(raw=str(inferred_total), normalized=inferred_total, confidence=0.8)
        result.sealImpressionId = self._seal_value(layout)
        return result

    def _power_of_attorney(self, layout: list[LayoutLine]) -> PowerOfAttorneyExtraction:
        result = PowerOfAttorneyExtraction()
        result.delegatorName = self._person_value(layout, ["위임자", "위임인", "Delegator", "Principal"])
        result.delegateName = self._person_value(layout, ["수임자", "대리인", "Delegate", "Agent"])
        result.targetCorporateName = self._company_value(layout, ["대상 법인", "법인명", "상호", "Company"])
        result.authorityText = self._line_value(layout, ["위임범위", "권한", "Authority"])
        result.canApplyKyc = self._contains(layout, ["KYC 신청", "고객확인", "KYC application"])
        result.canSubmitDocuments = self._contains(layout, ["서류 제출", "document submission"])
        result.canReceiveVc = self._contains(layout, ["VC 수령", "인증서 수령", "자격증명 수령", "VC receipt"])
        result.validFrom = self._date_value(layout, ["유효기간 시작", "유효 시작일", "Valid from"])
        result.validUntil = self._date_value(layout, ["유효기간 종료", "유효 종료일", "유효기간", "Valid until"])
        result.issueDate = self._date_value(layout, ["작성일", "발급일", "Issue date"])
        result.hasSignatureOrSeal = self._contains(layout, ["서명", "날인", "직인", "인감", "signature", "seal"])
        result.sealImpressionId = self._seal_value(layout)
        return result

    def _seal_certificate(self, layout: list[LayoutLine]) -> SealCertificateExtraction:
        result = SealCertificateExtraction()
        result.subjectName = self._person_value(layout, ["성명", "대표자", "Subject", "Representative"])
        result.corporateName = self._company_value(layout, ["법인명", "상호", "Company"])
        result.certificateNumber = self._line_value(layout, ["증명서번호", "인감증명서번호", "Certificate number"])
        result.sealImpressionId = self._seal_value(layout)
        result.issueDate = self._date_value(layout, ["발급일", "Issue date"])
        return result

    def _organization_document(self, document_type: DocumentType, layout: list[LayoutLine]) -> OrganizationDocumentExtraction:
        result = OrganizationDocumentExtraction()
        result.legalName = self._company_value(layout, ["법인명", "상호", "단체명", "회사명", "Company", "Organization"])
        representative = self._person_value(layout, ["대표자", "대표자명", "Representative"])
        result.representative = self._representative(representative, layout)
        result.purposeVerification = PurposeVerificationExtraction(
            establishmentPurpose=self._line_value(layout, ["설립목적", "목적", "Establishment purpose", "Purpose"]),
            acceptableForPurposeVerification=document_type not in {
                DocumentType.OFFICIAL_LETTER,
                DocumentType.MEETING_MINUTES,
                DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE,
            },
            purposeVerificationSatisfied=document_type not in {
                DocumentType.OFFICIAL_LETTER,
                DocumentType.MEETING_MINUTES,
                DocumentType.ORGANIZATION_IDENTITY_CERTIFICATE,
            },
        )
        result.issueDate = self._date_value(layout, ["발급일", "작성일", "Issue date"])
        result.documentSummary = ExtractedValue(raw="\n".join(item.text for item in layout), normalized=None, confidence=0.7)
        result.sealImpressionId = self._seal_value(layout)
        return result

    def _shareholders(self, layout: list[LayoutLine]) -> list[Shareholder]:
        shareholders: list[Shareholder] = []
        for line in layout:
            if "|" in line.text and not self._is_header_or_total(line.text):
                parsed = self._parse_table_shareholder(line)
                if parsed:
                    shareholders.append(parsed)
                continue
            parsed = self._parse_compact_shareholder(line)
            if parsed:
                shareholders.append(parsed)
        return shareholders

    def _parse_table_shareholder(self, line: LayoutLine) -> Shareholder | None:
        cells = [cell.strip() for cell in line.text.split("|")]
        if len(cells) < 4:
            return None
        holder_type = self._holder_type(cells[1]) if len(cells) > 1 else HolderType.UNKNOWN
        shares = next((self.normalizer.integer(cell) for cell in cells[2:] if "주" in cell.lower() or self.normalizer.integer(cell)), None)
        percent = next((self.normalizer.percent(cell) for cell in cells[2:] if "%" in cell or "percent" in cell.lower()), None)
        return Shareholder(
            name=cells[0] or None,
            holderType=holder_type,
            birthDate=self.normalizer.date(cells[2]) if len(cells) > 2 else None,
            nationality=next((cell for cell in cells if cell.upper() in {"KR", "KOR", "US", "USA"}), None),
            shares=shares,
            ownershipPercent=percent,
            evidenceRefs=[line.evidence_id],
        )

    def _parse_compact_shareholder(self, line: LayoutLine) -> Shareholder | None:
        pattern = re.compile(
            r"(?:주주|Shareholder)\s*[:：]?\s*(?P<name>.+?),\s*(?P<type>개인|법인|기타|individual|corporate|company)"
            r".*?(?P<shares>[\d,]+)\s*(?:주|shares?).*?(?P<percent>\d+(?:\.\d+)?)\s*(?:%|percent)",
            re.IGNORECASE,
        )
        match = pattern.search(line.text)
        if not match:
            return None
        return Shareholder(
            name=match.group("name").strip(),
            holderType=self._holder_type(match.group("type")),
            shares=self.normalizer.integer(match.group("shares")),
            ownershipPercent=self.normalizer.percent(match.group("percent")),
            evidenceRefs=[line.evidence_id],
        )

    def _representative(self, representative_name: ExtractedValue, layout: list[LayoutLine]) -> PersonExtraction:
        return PersonExtraction(
            name=representative_name,
            birthDate=self._date_value(layout, ["대표자 생년월일", "대표 생년월일", "생년월일", "Birth date"]),
            nationality=self._line_value(layout, ["대표자 국적", "대표 국적", "국적", "Nationality"]),
            englishName=self._line_value(layout, ["대표자 영문명", "대표 영문명", "영문명", "English name"]),
        )

    def _layout(self, text: str | None) -> list[LayoutLine]:
        if not text:
            return []
        lines = [line.strip() for line in str(text).splitlines() if line.strip()]
        if len(lines) <= 1:
            lines = [part.strip() for part in re.split(r"[;\n]", str(text)) if part.strip()]
        return [LayoutLine(evidence_id=f"ocr-line-{index + 1}", text=line) for index, line in enumerate(lines)]

    def _line_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        for line in layout:
            for label in labels:
                pattern = rf"{re.escape(label)}\s*[:：]?\s*(.+)"
                match = re.search(pattern, line.text, flags=re.IGNORECASE)
                if match:
                    value = match.group(1).strip()
                    return ExtractedValue(raw=value, normalized=value, confidence=line.confidence, evidenceRefs=[line.evidence_id])
        return ExtractedValue()

    def _company_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        value = self._line_value(layout, labels)
        value.normalized = self.normalizer.company_name(value.raw)
        return value

    def _person_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        value = self._line_value(layout, labels)
        value.normalized = self.normalizer.person_name(value.raw)
        return value

    def _digits_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        value = self._line_value(layout, labels)
        value.normalized = self.normalizer.digits(value.raw)
        return value

    def _integer_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        value = self._line_value(layout, labels)
        value.normalized = self.normalizer.integer(value.raw)
        return value

    def _date_value(self, layout: list[LayoutLine], labels: Iterable[str]) -> ExtractedValue:
        value = self._line_value(layout, labels)
        value.normalized = self.normalizer.date(value.raw)
        return value

    def _seal_value(self, layout: list[LayoutLine]) -> ExtractedValue:
        value = self._line_value(layout, ["인감 식별값", "인감번호", "인감 코드", "인영 식별값", "Seal impression ID"])
        value.normalized = self.normalizer.digits(value.raw) or value.raw
        return value

    def _contains(self, layout: list[LayoutLine], keywords: Iterable[str]) -> ExtractedValue:
        refs = [line.evidence_id for line in layout if any(keyword.lower() in line.text.lower() for keyword in keywords)]
        return ExtractedValue(raw=bool(refs), normalized=bool(refs), confidence=1.0 if refs else 0.0, evidenceRefs=refs)

    def _holder_type(self, value: str | None) -> HolderType:
        text = str(value or "").lower()
        if "개인" in text or "individual" in text:
            return HolderType.INDIVIDUAL
        if "법인" in text or "회사" in text or "corporate" in text or "company" in text:
            return HolderType.CORPORATE
        return HolderType.UNKNOWN

    def _is_header_or_total(self, text: str) -> bool:
        compact = text.replace(" ", "")
        first_cell = text.split("|", 1)[0].strip()
        return "합계" in compact or first_cell in {"주주명", "투자자명", "회원명", "성명", "이름", "Name"}
