import pytest
from pydantic import ValidationError

from app.ai.schemas import TriageRequest, TriageResponse


def test_accepts_minimal_triage_request():
    request = TriageRequest(
        ticket_id="tkt_9001",
        subject="Received damaged earbuds",
        body="My earbuds arrived damaged.",
        customer_tier="gold",
        customer_verified=True,
    )

    assert request.ticket_id == "tkt_9001"
    assert request.customer_verified is True


def test_accepts_valid_triage_response():
    response = TriageResponse(
        category="refund",
        priority="high",
        escalation=True,
    )

    assert response.model_dump(mode="json") == {
        "category": "refund",
        "priority": "high",
        "escalation": True,
    }


def test_rejects_category_outside_allowed_values():
    with pytest.raises(ValidationError):
        TriageResponse(
            category="coupon",
            priority="high",
            escalation=False,
        )


def test_rejects_priority_outside_allowed_values():
    with pytest.raises(ValidationError):
        TriageResponse(
            category="refund",
            priority="critical",
            escalation=False,
        )


def test_rejects_non_boolean_escalation():
    with pytest.raises(ValidationError):
        TriageResponse(
            category="refund",
            priority="high",
            escalation="false",
        )


def test_request_rejects_unneeded_order_context():
    with pytest.raises(ValidationError):
        TriageRequest(
            ticket_id="tkt_9001",
            subject="Received damaged earbuds",
            body="My earbuds arrived damaged.",
            customer_tier="gold",
            customer_verified=True,
            order_id="ord_5001",
        )