import re
from dataclasses import dataclass

from app.schemas import TriageCategory, TriagePriority


@dataclass(frozen=True)
class GuardrailDecision:
    category: TriageCategory
    priority: TriagePriority
    reason: str


_SAFETY_ISSUE = re.compile(
    r"\b(?:battery\b.{0,40}\b(?:swollen|swelling|swell)\b|"
    r"(?:swollen|swelling)\b.{0,40}\bbattery\b|"
    r"overheating|burning\s+smell|exposed\s+wires?|electric\s+shock)\b",
    re.IGNORECASE,
)

_IDENTITY_BYPASS = re.compile(
    r"\b(?:ignore|bypass|skip|avoid|waive|disable|without)\b.{0,80}"
    r"\b(?:identity|verification|verify|authentication|security)\b"
    r"(?:[\s-]+checks?)?",
    re.IGNORECASE | re.DOTALL,
)

_PROMPT_OVERRIDE = re.compile(
    r"\b(?:ignore|disregard|override)\b.{0,60}"
    r"\b(?:previous|prior|system|all)\b.{0,40}"
    r"\b(?:instructions|rules|policy)\b",
    re.IGNORECASE | re.DOTALL,
)

_HIDDEN_COUPON = re.compile(
    r"\b(?:hidden|secret|unpublished)\s+coupon\b",
    re.IGNORECASE,
)

_COUPON_CONCEALMENT = re.compile(
    r"\b(?:issue|give|grant|create|provide)\b.{0,80}\bcoupon\b"
    r".{0,100}\b(?:hide|conceal|keep.{0,20}secret|do not tell|don't tell|"
    r"not tell)\b.{0,60}\b(?:human|reviewer|agent|support)\b",
    re.IGNORECASE | re.DOTALL,
)

_SECRET_DISCLOSURE = re.compile(
    r"\b(?:reveal|show|tell|print|disclose|share|provide|asks?\s+for|requests?)\b"
    r".{0,80}\b(?:system prompt|hidden prompt|internal notes|instructions|"
    r"api key|secret|password|credentials)\b",
    re.IGNORECASE | re.DOTALL,
)


def inspect_customer_message(text: str) -> GuardrailDecision | None:
    if _SAFETY_ISSUE.search(text):
        return GuardrailDecision(
            category=TriageCategory.WARRANTY,
            priority=TriagePriority.URGENT,
            reason="Product safety issue requires urgent human specialist review.",
        )

    if _IDENTITY_BYPASS.search(text):
        return GuardrailDecision(
            category=TriageCategory.ACCOUNT_SECURITY,
            priority=TriagePriority.HIGH,
            reason="Identity-verification bypass request requires human review.",
        )

    if _SECRET_DISCLOSURE.search(text):
        return GuardrailDecision(
            category=TriageCategory.ACCOUNT_SECURITY,
            priority=TriagePriority.HIGH,
            reason=(
                "Request to disclose protected instructions or secrets "
                "requires human review."
            ),
        )

    if (
        _PROMPT_OVERRIDE.search(text)
        or _HIDDEN_COUPON.search(text)
        or _COUPON_CONCEALMENT.search(text)
    ):
        return GuardrailDecision(
            category=TriageCategory.GENERAL,
            priority=TriagePriority.MEDIUM,
            reason="Instruction-manipulation request requires human review.",
        )

    return None