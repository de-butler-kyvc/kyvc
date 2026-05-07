from typing import Any

from app.ai_assessment.enums import CheckStatus, Severity
from app.ai_assessment.engines.normalizer import Normalizer
from app.ai_assessment.schemas import CrossDocumentCheck, ExtractedValue


class CrossDocumentConsistencyChecker:
    def __init__(self, normalizer: Normalizer | None = None) -> None:
        self.normalizer = normalizer or Normalizer()

    def check(self, extracted_by_type: dict[str, Any], inputs: dict[str, str | None]) -> list[CrossDocumentCheck]:
        checks = []
        br = extracted_by_type.get("BUSINESS_REGISTRATION")
        cr = extracted_by_type.get("CORPORATE_REGISTRY")
        sh = extracted_by_type.get("SHAREHOLDER_REGISTRY")
        poa = extracted_by_type.get("POWER_OF_ATTORNEY")
        checks.extend(self._company_name_checks(br, cr, sh, poa))
        checks.extend(self._representative_checks(br, cr, poa))
        checks.extend(self._input_number_checks(br, cr, inputs))
        checks.extend(self._address_checks(br, cr))
        return checks

    def _company_name_checks(self, *docs: Any) -> list[CrossDocumentCheck]:
        values = []
        refs = []
        for name, doc in zip(["businessRegistration", "corporateRegistry", "shareholderRegistry", "powerOfAttorney"], docs):
            field = getattr(doc, "legalName", None) or getattr(doc, "targetCorporateName", None)
            if field and field.normalized:
                values.append({"source": name, "raw": field.raw, "normalized": field.normalized})
                refs.extend(field.evidenceRefs)
        return [self._same_value_check("COMPANY_NAME_MATCH", values, refs, "Company name consistency")]

    def _representative_checks(self, br: Any, cr: Any, poa: Any) -> list[CrossDocumentCheck]:
        values = []
        refs = []
        for name, field in [
            ("businessRegistration", getattr(br, "representativeName", None)),
            ("corporateRegistry", getattr(cr, "representativeName", None)),
            ("powerOfAttorneyDelegator", getattr(poa, "delegatorName", None)),
        ]:
            if field and field.normalized:
                values.append({"source": name, "raw": field.raw, "normalized": field.normalized})
                refs.extend(field.evidenceRefs)
        return [self._same_value_check("REPRESENTATIVE_NAME_MATCH", values, refs, "Representative name consistency")]

    def _input_number_checks(self, br: Any, cr: Any, inputs: dict[str, str | None]) -> list[CrossDocumentCheck]:
        checks = []
        input_brn = self.normalizer.digits(inputs.get("businessRegistrationNumber"))
        brn = self.normalizer.digits(getattr(getattr(br, "businessRegistrationNumber", None), "normalized", None))
        if input_brn and brn:
            checks.append(self._number_check("BUSINESS_REGISTRATION_NUMBER_MATCH", input_brn, brn, getattr(br, "businessRegistrationNumber")))
        input_crn = self.normalizer.digits(inputs.get("corporateRegistrationNumber"))
        crn = self.normalizer.digits(getattr(getattr(cr, "corporateRegistrationNumber", None), "normalized", None))
        if input_crn and crn:
            checks.append(self._number_check("CORPORATE_REGISTRATION_NUMBER_MATCH", input_crn, crn, getattr(cr, "corporateRegistrationNumber")))
        return checks

    def _address_checks(self, br: Any, cr: Any) -> list[CrossDocumentCheck]:
        br_addr = getattr(getattr(br, "businessAddress", None), "raw", None)
        cr_addr = getattr(getattr(cr, "headOfficeAddress", None), "raw", None)
        if not br_addr or not cr_addr:
            return []
        br_norm = "".join(str(br_addr).split())
        cr_norm = "".join(str(cr_addr).split())
        refs = getattr(br.businessAddress, "evidenceRefs", []) + getattr(cr.headOfficeAddress, "evidenceRefs", [])
        if br_norm == cr_norm or br_norm in cr_norm or cr_norm in br_norm:
            return [
                CrossDocumentCheck(
                    checkCode="ADDRESS_MATCH",
                    status=CheckStatus.PASS,
                    severity=Severity.LOW,
                    message="Business and head office addresses are consistent.",
                    values=[{"source": "businessRegistration", "raw": br_addr}, {"source": "corporateRegistry", "raw": cr_addr}],
                    evidenceRefs=refs,
                )
            ]
        return [
            CrossDocumentCheck(
                checkCode="ADDRESS_MATCH",
                status=CheckStatus.WARN,
                severity=Severity.MEDIUM,
                message="Business and head office addresses differ; review may be needed.",
                values=[{"source": "businessRegistration", "raw": br_addr}, {"source": "corporateRegistry", "raw": cr_addr}],
                evidenceRefs=refs,
            )
        ]

    def _same_value_check(self, code: str, values: list[dict[str, Any]], refs: list[str], label: str) -> CrossDocumentCheck:
        unique = {item["normalized"] for item in values if item.get("normalized")}
        if len(values) < 2:
            status = CheckStatus.UNKNOWN
            severity = Severity.LOW
            message = f"{label} could not be fully checked."
        elif len(unique) == 1:
            status = CheckStatus.PASS
            severity = Severity.LOW
            message = f"{label} passed."
        else:
            status = CheckStatus.FAIL
            severity = Severity.HIGH
            message = f"{label} failed."
        return CrossDocumentCheck(checkCode=code, status=status, severity=severity, message=message, values=values, evidenceRefs=refs)

    def _number_check(self, code: str, expected: str, actual: str, field: ExtractedValue) -> CrossDocumentCheck:
        passed = expected == actual
        return CrossDocumentCheck(
            checkCode=code,
            status=CheckStatus.PASS if passed else CheckStatus.FAIL,
            severity=Severity.LOW if passed else Severity.CRITICAL,
            message=f"{code} {'passed' if passed else 'failed'}.",
            values=[{"source": "applicationInput", "normalized": expected}, {"source": "document", "normalized": actual}],
            evidenceRefs=field.evidenceRefs,
        )
