from fastapi import APIRouter, Request

from app.ai_assessment.api_models import LlmPrimaryAssessmentRequest, LlmPrimaryAssessmentResponse
from app.ai_assessment.llm_primary import assess_documents_with_llm_primary


router = APIRouter(prefix="/ai-assessment", tags=["ai-assessment"])


@router.post("/assessments/llm-primary", response_model=LlmPrimaryAssessmentResponse)
def assess_llm_primary(payload: LlmPrimaryAssessmentRequest, request: Request) -> LlmPrimaryAssessmentResponse:
    return assess_documents_with_llm_primary(payload, request.app.state.settings)
