from app.ai_assessment.enums import BeneficialOwnershipMethod, BeneficialOwnershipStatus, HolderType
from app.ai_assessment.schemas import (
    BeneficialOwner,
    BeneficialOwnershipResult,
    EngineIssue,
    ShareholderRegistryExtraction,
)


class BeneficialOwnerEngine:
    def __init__(self, threshold_percent: float = 25.0, total_tolerance_percent: float = 1.0) -> None:
        self.threshold_percent = threshold_percent
        self.total_tolerance_percent = total_tolerance_percent

    def evaluate(
        self,
        shareholder_registry: ShareholderRegistryExtraction | None,
        representative_name: str | None = None,
    ) -> BeneficialOwnershipResult:
        if not shareholder_registry or not shareholder_registry.shareholders:
            issues = [EngineIssue(code="SHAREHOLDER_INFORMATION_MISSING", message="Shareholder information is missing.")]
            if representative_name:
                return BeneficialOwnershipResult(
                    status=BeneficialOwnershipStatus.MANUAL_REVIEW_REQUIRED,
                    method=BeneficialOwnershipMethod.REPRESENTATIVE_FALLBACK,
                    thresholdPercent=self.threshold_percent,
                    owners=[
                        BeneficialOwner(
                            name=representative_name,
                            holderType=HolderType.INDIVIDUAL,
                            basis="Representative fallback candidate; shareholder ownership is unavailable.",
                            confidence=0.3,
                        )
                    ],
                    issues=issues,
                )
            return BeneficialOwnershipResult(
                status=BeneficialOwnershipStatus.UNRESOLVED,
                method=BeneficialOwnershipMethod.REPRESENTATIVE_FALLBACK,
                thresholdPercent=self.threshold_percent,
                issues=issues,
            )

        self._fill_missing_percents(shareholder_registry)
        shareholders = shareholder_registry.shareholders
        issues = self._ownership_total_issues(shareholders)
        threshold_hits = [s for s in shareholders if (s.ownershipPercent or 0) >= self.threshold_percent]
        corporate_hits = [s for s in threshold_hits if s.holderType == HolderType.CORPORATE]
        if corporate_hits:
            issues.append(
                EngineIssue(
                    code="CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED",
                    message="Corporate shareholder at or above threshold requires lower-tier shareholder documents.",
                    evidenceRefs=self._refs(corporate_hits),
                )
            )
            return BeneficialOwnershipResult(
                status=BeneficialOwnershipStatus.NEEDS_SUPPLEMENT,
                method=BeneficialOwnershipMethod.CORPORATE_SHAREHOLDER_RECURSIVE_CHECK_REQUIRED,
                thresholdPercent=self.threshold_percent,
                owners=[self._owner(s, "Corporate shareholder requires recursive ownership check.") for s in corporate_hits],
                issues=issues,
            )

        individual_hits = [s for s in threshold_hits if s.holderType == HolderType.INDIVIDUAL]
        if individual_hits:
            return BeneficialOwnershipResult(
                status=BeneficialOwnershipStatus.MANUAL_REVIEW_REQUIRED if issues else BeneficialOwnershipStatus.RESOLVED,
                method=BeneficialOwnershipMethod.OWNERSHIP_THRESHOLD,
                thresholdPercent=self.threshold_percent,
                owners=[self._owner(s, f"Natural person owns at least {self.threshold_percent}%.") for s in individual_hits],
                issues=issues,
            )

        largest = max(shareholders, key=lambda s: s.ownershipPercent or -1)
        if largest.holderType == HolderType.CORPORATE:
            issues.append(
                EngineIssue(
                    code="CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED",
                    message="Largest shareholder is corporate; lower-tier shareholder documents are required.",
                    evidenceRefs=largest.evidenceRefs,
                )
            )
            status = BeneficialOwnershipStatus.NEEDS_SUPPLEMENT
            method = BeneficialOwnershipMethod.CORPORATE_SHAREHOLDER_RECURSIVE_CHECK_REQUIRED
        else:
            status = BeneficialOwnershipStatus.MANUAL_REVIEW_REQUIRED if issues else BeneficialOwnershipStatus.RESOLVED
            method = BeneficialOwnershipMethod.LARGEST_SHAREHOLDER
        return BeneficialOwnershipResult(
            status=status,
            method=method,
            thresholdPercent=self.threshold_percent,
            owners=[self._owner(largest, "No natural person at threshold; largest shareholder candidate.")],
            issues=issues,
        )

    def evaluate_recursive(
        self,
        shareholder_registry: ShareholderRegistryExtraction | None,
        lower_tier_registries: list[ShareholderRegistryExtraction],
        representative_name: str | None = None,
    ) -> BeneficialOwnershipResult:
        if not shareholder_registry or not shareholder_registry.shareholders:
            return self.evaluate(shareholder_registry, representative_name)

        registry_by_name = {
            self._company_key(registry.legalName.normalized or registry.legalName.raw): registry
            for registry in lower_tier_registries
            if registry is not shareholder_registry and self._company_key(registry.legalName.normalized or registry.legalName.raw)
        }
        owners, issues, used_largest = self._resolve_registry(
            shareholder_registry,
            registry_by_name,
            multiplier=1.0,
            path=[shareholder_registry.legalName.raw or "applicant"],
            visited={self._company_key(shareholder_registry.legalName.normalized or shareholder_registry.legalName.raw)},
        )
        if issues:
            if any(issue.code == "CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED" for issue in issues):
                return BeneficialOwnershipResult(
                    status=BeneficialOwnershipStatus.NEEDS_SUPPLEMENT,
                    method=BeneficialOwnershipMethod.CORPORATE_SHAREHOLDER_RECURSIVE_CHECK_REQUIRED,
                    thresholdPercent=self.threshold_percent,
                    owners=owners,
                    issues=issues,
                )
            return BeneficialOwnershipResult(
                status=BeneficialOwnershipStatus.MANUAL_REVIEW_REQUIRED,
                method=BeneficialOwnershipMethod.LARGEST_SHAREHOLDER if used_largest else BeneficialOwnershipMethod.OWNERSHIP_THRESHOLD,
                thresholdPercent=self.threshold_percent,
                owners=owners,
                issues=issues,
            )
        if owners:
            return BeneficialOwnershipResult(
                status=BeneficialOwnershipStatus.RESOLVED,
                method=BeneficialOwnershipMethod.LARGEST_SHAREHOLDER if used_largest else BeneficialOwnershipMethod.OWNERSHIP_THRESHOLD,
                thresholdPercent=self.threshold_percent,
                owners=owners,
            )
        return self.evaluate(shareholder_registry, representative_name)

    def _resolve_registry(
        self,
        registry: ShareholderRegistryExtraction,
        registry_by_name: dict[str, ShareholderRegistryExtraction],
        multiplier: float,
        path: list[str],
        visited: set[str | None],
    ) -> tuple[list[BeneficialOwner], list[EngineIssue], bool]:
        self._fill_missing_percents(registry)
        shareholders = registry.shareholders
        issues = self._ownership_total_issues(shareholders)
        selected = [s for s in shareholders if (s.ownershipPercent or 0) >= self.threshold_percent]
        used_largest = False
        if not selected and shareholders:
            selected = [max(shareholders, key=lambda s: s.ownershipPercent or -1)]
            used_largest = True

        owners: list[BeneficialOwner] = []
        for shareholder in selected:
            effective_percent = round(multiplier * ((shareholder.ownershipPercent or 0.0) / 100.0) * 100.0, 4)
            if shareholder.holderType == HolderType.INDIVIDUAL:
                owners.append(
                    BeneficialOwner(
                        name=shareholder.name or "UNKNOWN",
                        holderType=HolderType.INDIVIDUAL,
                        birthDate=shareholder.birthDate,
                        nationality=shareholder.nationality,
                        englishName=shareholder.englishName,
                        ownershipPercent=effective_percent,
                        basis=self._recursive_basis(path, shareholder.name, shareholder.ownershipPercent, effective_percent, used_largest),
                        evidenceRefs=shareholder.evidenceRefs,
                        confidence=0.9 if shareholder.ownershipPercent is not None else 0.6,
                    )
                )
                continue

            if shareholder.holderType != HolderType.CORPORATE:
                owners.append(self._owner(shareholder, "Selected shareholder has unknown holder type; manual review may be required."))
                continue

            corporate_key = self._company_key(shareholder.name)
            lower_registry = registry_by_name.get(corporate_key)
            if lower_registry is None:
                issues.append(
                    EngineIssue(
                        code="CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED",
                        message=f"Corporate shareholder {shareholder.name or 'UNKNOWN'} requires lower-tier shareholder documents.",
                        evidenceRefs=shareholder.evidenceRefs,
                    )
                )
                continue
            if corporate_key in visited:
                issues.append(
                    EngineIssue(
                        code="CORPORATE_SHAREHOLDER_RECURSIVE_DOCUMENT_REQUIRED",
                        message=f"Ownership chain contains a repeated corporate shareholder: {shareholder.name}.",
                        evidenceRefs=shareholder.evidenceRefs,
                    )
                )
                continue
            nested_owners, nested_issues, nested_used_largest = self._resolve_registry(
                lower_registry,
                registry_by_name,
                multiplier=multiplier * ((shareholder.ownershipPercent or 0.0) / 100.0),
                path=path + [shareholder.name or "corporate shareholder"],
                visited=visited | {corporate_key},
            )
            owners.extend(nested_owners)
            issues.extend(nested_issues)
            used_largest = used_largest or nested_used_largest
        return owners, issues, used_largest

    def _fill_missing_percents(self, registry: ShareholderRegistryExtraction) -> None:
        total_shares = registry.totalShares.normalized
        for shareholder in registry.shareholders:
            if shareholder.ownershipPercent is None and shareholder.shares is not None and total_shares:
                shareholder.ownershipPercent = round((shareholder.shares / total_shares) * 100, 4)

    def _ownership_total_issues(self, shareholders) -> list[EngineIssue]:
        percents = [s.ownershipPercent for s in shareholders if s.ownershipPercent is not None]
        if not percents:
            return [EngineIssue(code="OWNERSHIP_PERCENT_MISSING", message="Ownership percentages are missing.")]
        total = sum(percents)
        if abs(total - 100.0) > self.total_tolerance_percent:
            return [EngineIssue(code="OWNERSHIP_PERCENT_TOTAL_INVALID", message=f"Ownership total is {total:.2f}%, outside tolerance.")]
        return []

    def _recursive_basis(
        self,
        path: list[str],
        owner_name: str | None,
        local_percent: float | None,
        effective_percent: float,
        used_largest: bool,
    ) -> str:
        rule = "largest-shareholder fallback" if used_largest else f"at least {self.threshold_percent}% ownership/control chain"
        chain = " -> ".join([*path, owner_name or "UNKNOWN"])
        if local_percent is None:
            return f"Resolved through recursive ownership chain ({rule}): {chain}."
        return (
            f"Resolved through recursive ownership chain ({rule}): {chain}; "
            f"local ownership {local_percent:.2f}%, effective applicant ownership {effective_percent:.2f}%."
        )

    def _company_key(self, value: str | None) -> str:
        compact = "".join(str(value or "").upper().split())
        for token in ["주식회사", "(주)", "㈜", "株式会社", "CO.,LTD.", "CO.LTD.", "LTD.", "INC."]:
            compact = compact.replace(token.upper(), "")
        return compact

    def _owner(self, shareholder, basis: str) -> BeneficialOwner:
        return BeneficialOwner(
            name=shareholder.name or "UNKNOWN",
            holderType=shareholder.holderType,
            birthDate=shareholder.birthDate,
            nationality=shareholder.nationality,
            englishName=shareholder.englishName,
            ownershipPercent=shareholder.ownershipPercent,
            basis=basis,
            evidenceRefs=shareholder.evidenceRefs,
            confidence=0.95 if shareholder.ownershipPercent is not None else 0.6,
        )

    def _refs(self, shareholders) -> list[str]:
        refs = []
        for shareholder in shareholders:
            refs.extend(shareholder.evidenceRefs)
        return refs
