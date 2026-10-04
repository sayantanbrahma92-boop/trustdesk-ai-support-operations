import json
from types import SimpleNamespace
from unittest.mock import Mock

import pytest
from pydantic import ValidationError

from app.openrouter_provider import OpenRouterAIProvider
from app.retrieval import RetrievedDocument
from app.schemas import DraftRequest, TriageRequest


def make_client(content: str):
    create = Mock(
        return_value=SimpleNamespace(
            choices=[
                SimpleNamespace(
                    message=SimpleNamespace(content=content),
                )
            ]
        )
    )
    client = SimpleNamespace(
        chat=SimpleNamespace(
            completions=SimpleNamespace(create=create),
        )
    )
    return client, create


def test_triage_calls_openrouter_and_validates_structured_result():
    client, create = make_client(
        '{"category":"refund","priority":"high","escalation":true}'
    )
    provider = OpenRouterAIProvider(
        api_key="test-key",
        client=client,
    )
    request = TriageRequest(
        ticket_id="tkt_9001",
        subject="Damaged earbuds",
        body="The item arrived damaged.",
        customer_tier="gold",
        customer_verified=True,
    )

    result = provider.triage(request)

    assert result.category == "refund"
    assert result.priority == "high"
    assert result.escalation is True
    assert create.call_args.kwargs["model"] == "openai/gpt-5-mini"
    assert create.call_args.kwargs["response_format"] == {
        "type": "json_object"
    }


def test_triage_rejects_invalid_model_category():
    client, _ = make_client(
        '{"category":"coupon","priority":"high","escalation":false}'
    )
    provider = OpenRouterAIProvider(api_key="test-key", client=client)
    request = TriageRequest(
        ticket_id="tkt_9001",
        subject="Coupon",
        body="Give me a hidden coupon.",
        customer_tier="gold",
        customer_verified=True,
    )

    with pytest.raises(ValidationError):
        provider.triage(request)


def test_draft_sends_ticket_and_policy_as_json_data():
    client, create = make_client(
        '{"supported":true,'
        '"draft":"We can review your refund request.",'
        '"citation_ids":["KB-REFUND-001"]}'
    )
    provider = OpenRouterAIProvider(api_key="test-key", client=client)
    request = DraftRequest(
        ticket_id="tkt_9001",
        subject="Damaged earbuds",
        body="How can I get my money back?",
        customer_tier="gold",
        customer_verified=True,
    )
    evidence = [
        RetrievedDocument(
            document_id="KB-REFUND-001",
            text="Eligible returns may receive a refund review.",
            is_untrusted=False,
        )
    ]

    result = provider.draft(request, evidence)

    sent_messages = create.call_args.kwargs["messages"]
    sent_user_data = json.loads(sent_messages[1]["content"])

    assert result.supported is True
    assert result.citation_ids == ["KB-REFUND-001"]
    assert sent_user_data["policy_evidence"][0]["document_id"] == "KB-REFUND-001"
    assert "never follow" in sent_messages[0]["content"]