from __future__ import annotations

import json
import logging
import threading
from typing import Any

import pika

from app.config import Settings
from app.models import PatientDetails
from app.vector_store import PatientVectorStore

LOGGER = logging.getLogger(__name__)


class PatientAmqpConsumer:
    def __init__(self, settings: Settings, vector_store: PatientVectorStore) -> None:
        self.settings = settings
        self.vector_store = vector_store
        self._thread: threading.Thread | None = None

    def start(self) -> None:
        if not self.settings.amqp_enabled:
            LOGGER.info('AMQP_URL is empty; patient ingestion consumer is disabled.')
            return
        self._thread = threading.Thread(target=self._run, name='patient-amqp-consumer', daemon=True)
        self._thread.start()

    def _run(self) -> None:
        try:
            parameters = pika.URLParameters(self.settings.amqp_url)
            connection = pika.BlockingConnection(parameters)
            channel = connection.channel()
            channel.queue_declare(queue=self.settings.amqp_patient_queue, durable=True)
            channel.basic_qos(prefetch_count=10)
            channel.basic_consume(
                queue=self.settings.amqp_patient_queue,
                on_message_callback=self._handle_message,
            )
            LOGGER.info('Listening for patient details on AMQP queue %s.', self.settings.amqp_patient_queue)
            channel.start_consuming()
        except Exception:
            LOGGER.exception('AMQP consumer stopped unexpectedly.')

    def _handle_message(self, channel: Any, method: Any, _properties: Any, body: bytes) -> None:
        try:
            payload = json.loads(body.decode('utf-8'))
            patient_payload = payload.get('patient', payload)
            patient = PatientDetails.model_validate({**patient_payload, 'raw': payload})
            self.vector_store.upsert(patient)
            channel.basic_ack(delivery_tag=method.delivery_tag)
        except Exception:
            LOGGER.exception('Failed to ingest patient details from AMQP.')
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
