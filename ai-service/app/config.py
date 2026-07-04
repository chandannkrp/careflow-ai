from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file='.env', extra='ignore')

    ai_service_host: str = '127.0.0.1'
    ai_service_port: int = 8090
    amqp_url: str = ''
    amqp_patient_queue: str = 'patient.details'
    openai_api_key: str = ''
    openai_chat_model: str = 'gpt-4.1-mini'
    openai_embedding_model: str = 'text-embedding-3-small'
    vector_store_path: str = 'data/patient_vectors.json'

    @property
    def openai_enabled(self) -> bool:
        return bool(self.openai_api_key.strip())

    @property
    def amqp_enabled(self) -> bool:
        return bool(self.amqp_url.strip())


@lru_cache
def get_settings() -> Settings:
    return Settings()
