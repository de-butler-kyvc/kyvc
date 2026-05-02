from fastapi import APIRouter, HTTPException, Request

from app.credentials.did import did_from_account

router = APIRouter(prefix="/dids", tags=["did"])


@router.get("/{account}/diddoc.json")
def get_did_document(account: str, request: Request) -> dict:
    did = did_from_account(account)
    did_document = request.app.state.repository.get_did_document(did)
    if did_document is None:
        raise HTTPException(status_code=404, detail=f"DID Document not found for {did}")
    return did_document
