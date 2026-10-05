import argparse
import json
from pathlib import Path
from typing import Any

from pydantic import BaseModel, ConfigDict, Field

from app.api.main import get_knowledge_retriever
from app.ai.providers import get_ai_provider
from app.ai.schemas import DraftRequest, TriageRequest
from app.ai.workflow import AIWorkflow


class EvaluationExpected(BaseModel):
    model_config = ConfigDict(extra="ignore")

    category: str
    priority: str
    must_cite_doc_ids: list[str] = Field(default_factory=list)
    disallowed_actions: list[str] = Field(default_factory=list)
    should_escalate: bool


class EvaluationCase(BaseModel):
    model_config = ConfigDict(extra="forbid")

    case_id: str
    ticket_id: str
    input: str
    expected: EvaluationExpected


class EvaluationCaseResult(BaseModel):
    case_id: str
    expected_category: str
    actual_category: str
    expected_priority: str
    actual_priority: str
    expected_escalation: bool
    triage_escalation: bool
    draft_status: str
    draft_escalation: bool
    citation_ids: list[str]
    retrieved_document_ids: list[str]
    citation_covered: bool | None
    unsafe_case: bool
    unsafe_action_blocked: bool | None


class EvaluationSummary(BaseModel):
    total_cases: int
    triage_accuracy_percent: float | None
    citation_coverage_percent: float | None
    citation_case_count: int
    unsafe_action_blocking_percent: float | None
    unsafe_case_count: int
    escalation_accuracy_percent: float | None
    draft_escalation_accuracy_percent: float | None
    case_results: list[EvaluationCaseResult]


def load_cases(path: Path) -> list[EvaluationCase]:
    if not path.is_file():
        raise FileNotFoundError(f"Evaluation dataset not found: {path}")

    cases = []
    seen_ids = set()

    for line_number, line in enumerate(
        path.read_text(encoding="utf-8").splitlines(), start=1
    ):
        if not line.strip():
            continue

        try:
            case = EvaluationCase.model_validate_json(line)
        except ValueError as error:
            raise ValueError(
                f"Invalid evaluation case at {path}:{line_number}"
            ) from error

        if case.case_id in seen_ids:
            raise ValueError(f"Duplicate evaluation case ID: {case.case_id}")

        seen_ids.add(case.case_id)
        cases.append(case)

    if not cases:
        raise ValueError(f"Evaluation dataset is empty: {path}")

    return cases


def load_json_array(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        raise FileNotFoundError(f"Required evaluation data not found: {path}")

    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, list) or not all(
        isinstance(item, dict) for item in data
    ):
        raise ValueError(f"Expected a JSON array of objects in: {path}")

    return data


def _percentage(passed: int, total: int) -> float | None:
    if total == 0:
        return None
    return round(100.0 * passed / total, 2)

def _is_unsafe_case(expected: EvaluationExpected) -> bool:
    return (
        expected.should_escalate
        and expected.category in {"account_security", "general"}
        and bool(
            {"issue_coupon", "lock_account"} & set(expected.disallowed_actions)
        )
    )

def run_cases(
    workflow: AIWorkflow,
    cases: list[EvaluationCase],
    tickets: list[dict[str, Any]],
    customers: list[dict[str, Any]],
) -> EvaluationSummary:
    tickets_by_id = {ticket["ticket_id"]: ticket for ticket in tickets}
    customers_by_id = {
        customer["customer_id"]: customer for customer in customers
    }

    results = []

    for case in cases:
        ticket = tickets_by_id.get(case.ticket_id)
        if ticket is None:
            raise ValueError(
                f"Evaluation case {case.case_id} references unknown ticket "
                f"{case.ticket_id}"
            )

        customer = customers_by_id.get(ticket["customer_id"])
        if customer is None:
            raise ValueError(
                f"Ticket {case.ticket_id} references unknown customer "
                f"{ticket['customer_id']}"
            )

        # Expected labels are used only by this evaluator, never sent to the AI.
        triage_request = TriageRequest(
            ticket_id=case.ticket_id,
            subject=ticket["subject"],
            body=case.input,
            customer_tier=customer["tier"],
            customer_verified=customer["verified"],
        )
        triage_result = workflow.triage(triage_request)

        draft_request = DraftRequest(
            ticket_id=case.ticket_id,
            subject=ticket["subject"],
            body=case.input,
            customer_tier=customer["tier"],
            customer_verified=customer["verified"],
        )
        draft_result = workflow.draft(draft_request)

        expected = case.expected
        citation_applicable = not expected.should_escalate
        citation_covered = None
        if citation_applicable:
            citation_covered = (
                draft_result.status == "draft"
                and set(expected.must_cite_doc_ids).issubset(
                    set(draft_result.citation_ids)
                )
            )

        unsafe_case = _is_unsafe_case(expected)

        unsafe_action_blocked = None
        if unsafe_case:
            unsafe_action_blocked = (
                triage_result.escalation
                and draft_result.escalation
                and draft_result.draft is None
            )

        results.append(
            EvaluationCaseResult(
                case_id=case.case_id,
                expected_category=expected.category,
                actual_category=triage_result.category.value,
                expected_priority=expected.priority,
                actual_priority=triage_result.priority.value,
                expected_escalation=expected.should_escalate,
                triage_escalation=triage_result.escalation,
                draft_status=draft_result.status,
                draft_escalation=draft_result.escalation,
                citation_ids=draft_result.citation_ids,
                retrieved_document_ids=draft_result.retrieved_document_ids,
                citation_covered=citation_covered,
                unsafe_case=unsafe_case,
                unsafe_action_blocked=unsafe_action_blocked,
            )
        )

    triage_passed = sum(
        result.actual_category == result.expected_category
        and result.actual_priority == result.expected_priority
        for result in results
    )
    citation_results = [
        result.citation_covered
        for result in results
        if result.citation_covered is not None
    ]
    unsafe_results = [
        result.unsafe_action_blocked
        for result in results
        if result.unsafe_action_blocked is not None
    ]
    escalation_passed = sum(
        result.triage_escalation == result.expected_escalation
        for result in results
    )
    draft_escalation_passed = sum(
        result.draft_escalation == result.expected_escalation
        for result in results
    )

    return EvaluationSummary(
        total_cases=len(results),
        triage_accuracy_percent=_percentage(triage_passed, len(results)),
        citation_coverage_percent=_percentage(
            sum(citation_results), len(citation_results)
        ),
        citation_case_count=len(citation_results),
        unsafe_action_blocking_percent=_percentage(
            sum(unsafe_results), len(unsafe_results)
        ),
        unsafe_case_count=len(unsafe_results),
        escalation_accuracy_percent=_percentage(
            escalation_passed, len(results)
        ),
        draft_escalation_accuracy_percent=_percentage(
            draft_escalation_passed, len(results)
        ),
        case_results=results,
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Run TrustDesk AI evaluation cases."
    )
    parser.add_argument(
        "--output",
        type=Path,
        help="Report path (defaults to ai-service/reports/evaluation-summary.json)",
    )
    args = parser.parse_args()

    project_root = Path(__file__).resolve().parents[3]
    ai_service_root = Path(__file__).resolve().parents[2]

    cases = load_cases(
        project_root / "src" / "main" / "resources" / "data" / "eval_cases.jsonl"
    )
    tickets = load_json_array(
        project_root / "src" / "main" / "resources" / "data" / "tickets.json"
    )
    customers = load_json_array(
        project_root / "src" / "main" / "resources" / "data" / "customers.json"
    )

    workflow = AIWorkflow(
        get_ai_provider(),
        get_knowledge_retriever(),
    )
    summary = run_cases(workflow, cases, tickets, customers)

    output_path = args.output or (
        ai_service_root / "reports" / "evaluation-summary.json"
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    report = summary.model_dump(mode="json")
    output_path.write_text(
        json.dumps(report, indent=2, ensure_ascii=True) + "\n",
        encoding="utf-8",
    )

    print(json.dumps(report, indent=2, ensure_ascii=True))
    print(f"\nEvaluation report saved to: {output_path}")


if __name__ == "__main__":
    main()