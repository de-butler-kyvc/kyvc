from dataclasses import dataclass, field
from typing import Any


DOCUMENT_TYPES = {
    "KR_BUSINESS_REGISTRATION_CERTIFICATE",
    "KR_CORPORATE_REGISTER_FULL_CERTIFICATE",
    "KR_SHAREHOLDER_REGISTER",
    "KR_STOCK_CHANGE_STATEMENT",
    "KR_CONTRIBUTOR_REGISTER",
    "KR_MEMBER_REGISTER",
    "KR_ARTICLES_OF_ASSOCIATION",
    "KR_BOARD_MEMBER_LIST",
    "KR_OPERATING_RULES",
    "KR_BYLAWS",
    "KR_MEETING_MINUTES",
    "KR_OFFICIAL_LETTER",
    "KR_ESTABLISHMENT_APPLICATION",
    "KR_PERMIT_APPLICATION",
    "KR_ESTABLISHMENT_PERMIT",
    "KR_INSTALLATION_REPORT_CERTIFICATE",
    "KR_AUTHORIZATION_CERTIFICATE",
    "KR_ENTITY_REALNAME_CERTIFICATE",
    "KR_UNIQUE_NUMBER_CERTIFICATE",
    "FOREIGN_INVESTMENT_REGISTRATION_CERTIFICATE",
    "FOREIGN_ENTITY_REALNAME_DOCUMENT",
    "FOREIGN_CORPORATE_REGISTRY_CERTIFICATE",
    "FOREIGN_SHAREHOLDER_REGISTER",
    "FOREIGN_BENEFICIAL_OWNER_DOCUMENT",
    "FOREIGN_ARTICLES_OF_ASSOCIATION",
    "FOREIGN_OPERATING_RULES",
    "FOREIGN_BYLAWS",
    "FOREIGN_MEETING_MINUTES",
    "FOREIGN_OFFICIAL_LETTER",
    "FOREIGN_ESTABLISHMENT_APPLICATION",
    "FOREIGN_PERMIT_APPLICATION",
    "FOREIGN_ESTABLISHMENT_PERMIT",
    "FOREIGN_AUTHORIZATION_CERTIFICATE",
}

ORIGINAL_POLICIES = {"OPTIONAL", "REQUIRED", "NOT_ALLOWED"}


@dataclass(frozen=True)
class DocumentRule:
    id: str
    required: bool = True
    one_of: set[str] = field(default_factory=set)
    not_allowed: set[str] = field(default_factory=set)
    original_policy: str = "OPTIONAL"
    required_when: dict[str, Any] | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "DocumentRule":
        return cls(
            id=str(data["id"]),
            required=bool(data.get("required", True)),
            one_of=set(data.get("oneOf") or []),
            not_allowed=set(data.get("notAllowed") or []),
            original_policy=str(data.get("originalPolicy") or "OPTIONAL"),
            required_when=data.get("requiredWhen"),
        )


def stock_company_rules() -> list[DocumentRule]:
    return [
        DocumentRule("entity-realname-evidence", one_of={"KR_BUSINESS_REGISTRATION_CERTIFICATE"}),
        DocumentRule("registry-evidence", one_of={"KR_CORPORATE_REGISTER_FULL_CERTIFICATE"}),
        DocumentRule("ownership-evidence", one_of={"KR_SHAREHOLDER_REGISTER", "KR_STOCK_CHANGE_STATEMENT"}),
    ]


DEFAULT_RULES_BY_LEGAL_ENTITY_TYPE: dict[str, list[DocumentRule]] = {
    "STOCK_COMPANY": stock_company_rules(),
    "LIMITED_OR_PARTNERSHIP_COMPANY": [
        DocumentRule("entity-realname-evidence", one_of={"KR_BUSINESS_REGISTRATION_CERTIFICATE"}),
        DocumentRule("registry-evidence", one_of={"KR_CORPORATE_REGISTER_FULL_CERTIFICATE"}),
        DocumentRule(
            "ownership-or-member-evidence",
            one_of={"KR_CONTRIBUTOR_REGISTER", "KR_MEMBER_REGISTER", "KR_ARTICLES_OF_ASSOCIATION"},
        ),
    ],
    "INCORPORATED_ASSOCIATION_OR_FOUNDATION": [
        DocumentRule("entity-realname-evidence", one_of={"KR_BUSINESS_REGISTRATION_CERTIFICATE"}),
        DocumentRule("registry-evidence", one_of={"KR_CORPORATE_REGISTER_FULL_CERTIFICATE"}),
        DocumentRule(
            "ownership-or-governance-evidence",
            one_of={
                "KR_CONTRIBUTOR_REGISTER",
                "KR_MEMBER_REGISTER",
                "KR_BOARD_MEMBER_LIST",
                "KR_ARTICLES_OF_ASSOCIATION",
            },
        ),
        DocumentRule(
            "purpose-evidence",
            one_of={
                "KR_ARTICLES_OF_ASSOCIATION",
                "KR_ESTABLISHMENT_APPLICATION",
                "KR_PERMIT_APPLICATION",
                "KR_ESTABLISHMENT_PERMIT",
                "KR_AUTHORIZATION_CERTIFICATE",
            },
            not_allowed={"KR_OFFICIAL_LETTER"},
        ),
    ],
    "COOPERATIVE_OR_ASSOCIATION": [
        DocumentRule("entity-realname-evidence", one_of={"KR_BUSINESS_REGISTRATION_CERTIFICATE"}),
        DocumentRule(
            "registry-or-rule-evidence",
            one_of={"KR_CORPORATE_REGISTER_FULL_CERTIFICATE", "KR_ARTICLES_OF_ASSOCIATION", "KR_BYLAWS"},
        ),
        DocumentRule(
            "member-or-operation-evidence",
            one_of={
                "KR_ARTICLES_OF_ASSOCIATION",
                "KR_OPERATING_RULES",
                "KR_BYLAWS",
                "KR_CONTRIBUTOR_REGISTER",
                "KR_MEMBER_REGISTER",
                "KR_MEETING_MINUTES",
            },
        ),
        DocumentRule(
            "purpose-evidence",
            one_of={
                "KR_ARTICLES_OF_ASSOCIATION",
                "KR_ESTABLISHMENT_APPLICATION",
                "KR_PERMIT_APPLICATION",
                "KR_ESTABLISHMENT_PERMIT",
                "KR_AUTHORIZATION_CERTIFICATE",
            },
            not_allowed={"KR_OFFICIAL_LETTER"},
        ),
    ],
    "UNIQUE_NUMBER_ENTITY": [
        DocumentRule("entity-realname-evidence", one_of={"KR_ENTITY_REALNAME_CERTIFICATE", "KR_UNIQUE_NUMBER_CERTIFICATE"}),
        DocumentRule(
            "governance-or-operation-evidence",
            one_of={"KR_ARTICLES_OF_ASSOCIATION", "KR_OPERATING_RULES", "KR_BYLAWS", "KR_MEETING_MINUTES", "KR_OFFICIAL_LETTER"},
        ),
        DocumentRule(
            "purpose-evidence",
            one_of={
                "KR_ARTICLES_OF_ASSOCIATION",
                "KR_ESTABLISHMENT_APPLICATION",
                "KR_PERMIT_APPLICATION",
                "KR_ESTABLISHMENT_PERMIT",
                "KR_INSTALLATION_REPORT_CERTIFICATE",
                "KR_AUTHORIZATION_CERTIFICATE",
            },
            not_allowed={"KR_UNIQUE_NUMBER_CERTIFICATE", "KR_OFFICIAL_LETTER", "KR_MEETING_MINUTES"},
        ),
    ],
    "FOREIGN_COMPANY": [
        DocumentRule(
            "entity-realname-evidence",
            one_of={"FOREIGN_INVESTMENT_REGISTRATION_CERTIFICATE", "FOREIGN_ENTITY_REALNAME_DOCUMENT"},
        ),
        DocumentRule("registry-evidence", one_of={"FOREIGN_CORPORATE_REGISTRY_CERTIFICATE"}),
        DocumentRule("ownership-evidence", one_of={"FOREIGN_SHAREHOLDER_REGISTER", "FOREIGN_BENEFICIAL_OWNER_DOCUMENT"}),
        DocumentRule(
            "nonprofit-rule-evidence",
            one_of={"FOREIGN_ARTICLES_OF_ASSOCIATION", "FOREIGN_OPERATING_RULES", "FOREIGN_BYLAWS", "FOREIGN_MEETING_MINUTES"},
            required_when={"nonProfit": True},
        ),
        DocumentRule(
            "purpose-evidence",
            one_of={
                "FOREIGN_ARTICLES_OF_ASSOCIATION",
                "FOREIGN_ESTABLISHMENT_APPLICATION",
                "FOREIGN_PERMIT_APPLICATION",
                "FOREIGN_ESTABLISHMENT_PERMIT",
                "FOREIGN_AUTHORIZATION_CERTIFICATE",
            },
            not_allowed={"FOREIGN_OFFICIAL_LETTER"},
            required_when={"any": [{"nonProfit": True}, {"purposeCheckRequired": True}]},
        ),
    ],
}


def document_rules_for_legal_entity_type(legal_entity_type: str | None) -> list[DocumentRule]:
    if not legal_entity_type:
        return []
    return DEFAULT_RULES_BY_LEGAL_ENTITY_TYPE.get(legal_entity_type, [])


def condition_matches(condition: dict[str, Any] | None, payload: dict[str, Any]) -> bool:
    if condition is None:
        return True
    if "any" in condition:
        return any(condition_matches(item, payload) for item in condition["any"])
    legal_entity = payload.get("legalEntity") if isinstance(payload.get("legalEntity"), dict) else {}
    for key, expected in condition.items():
        if payload.get(key) != expected and legal_entity.get(key) != expected:
            return False
    return True
