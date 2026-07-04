from openai import OpenAI
from app.config import get_settings


def main() -> None:
    settings = get_settings()
    if not settings.openai_api_key:
        raise SystemExit('OPENAI_API_KEY is empty. Add it to your environment or ai-service/.env first.')

    client = OpenAI(api_key=settings.openai_api_key)
    response = client.responses.create(
        model=settings.openai_chat_model,
        input='Reply with exactly: CareFlow OpenAI connection ok',
    )
    print(response.output_text)


if __name__ == '__main__':
    main()
