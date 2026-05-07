from fastapi import APIRouter, HTTPException, Request

from app.ai_assessment.api_models import LlmPrimaryAssessmentRequest, LlmPrimaryAssessmentResponse
from app.ai_assessment.llm_primary import assess_documents_with_llm_primary


router = APIRouter(prefix="/ai-assessment", tags=["ai-assessment"])


@router.post("/assessments/llm-primary", response_model=LlmPrimaryAssessmentResponse)
def assess_llm_primary(payload: LlmPrimaryAssessmentRequest, request: Request) -> LlmPrimaryAssessmentResponse:
    try:
        return assess_documents_with_llm_primary(payload, request.app.state.settings)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except FileNotFoundError as exc:
        raise HTTPException(status_code=400, detail=f"document file not found: {exc}") from exc
