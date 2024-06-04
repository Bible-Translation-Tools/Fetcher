import glob
import json
import os
import re
from collections.abc import MutableSequence
from hashlib import md5
from mmap import mmap, ACCESS_READ
from pathlib import Path
from urllib.parse import urljoin

from fastapi import FastAPI
from pydantic import BaseModel

from azure.servicebus.aio import ServiceBusClient
from azure.servicebus import ServiceBusMessage


import os

NAMESPACE_CONNECTION_STR = os.getenv("NAMESPACE_CONNECTION_STR")
QUEUE_NAME = os.getenv("QUEUE_NAME")
CONTENT_URL = os.getenv("CONTENT_URL")
CONTENT_DIR = os.getenv("CONTENT_DIR")

# NAMESPACE_CONNECTION_STR = "connection_string"
# QUEUE_NAME = "fetcher_queue"
# CONTENT_URL = "https://audio-content.bibleineverylanguage.org"
# CONTENT_DIR = "./content"


class Filter(BaseModel):
    language_id: str
    resource_id: str
    book_slug: str | None = None
    chapter: int | None = None
    exclude_format: MutableSequence[str] = []
    exclude_quality: MutableSequence[str] = []
    exclude_grouping: MutableSequence[str] = []


class Parts(BaseModel):
    language_id: str
    resource_id: str
    book_slug: str
    chapter: int | None


app = FastAPI()


@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/receive_messages")
async def get_message():
    async with ServiceBusClient.from_connection_string(
        conn_str=NAMESPACE_CONNECTION_STR,
        logging_enable=True
    ) as service_bus_client:
        # if a client doesn't want to talk to bus directly, can consume through this endpoint to get messages as http. 
        async with service_bus_client:
            receiver = service_bus_client.get_queue_receiver(queue_name=QUEUE_NAME)
            messages = await receiver.receive_messages(max_wait_time=5, max_message_count=20)
            final = []
            for message in messages:
                obj = json.loads(str(message))
                final.append(obj)
                await receiver.complete_message(message)

            return final


@app.post("/send_messages")
async def read_content(content_filter: Filter):
    items = []
    # 
    target_dir = os.path.join(CONTENT_DIR, content_filter.language_id, content_filter.resource_id)

    # Narrow to book or chapter only level: 
    if content_filter.book_slug is not None:
        target_dir = os.path.join(target_dir, content_filter.book_slug)
        if content_filter.chapter is not None:
            target_dir = os.path.join(target_dir, str(content_filter.chapter))

    with open("./book_catalog.json", "r") as json_file:
        books = json.load(json_file)

    exclude_verse = True if "verse" in content_filter.exclude_grouping else False
    exclude_chapter = True if "chapter" in content_filter.exclude_grouping else False
    exclude_book = True if "book" in content_filter.exclude_grouping else False

    exclude_hi = True if "hi" in content_filter.exclude_quality else False
    exclude_low = True if "low" in content_filter.exclude_quality else False

    exclude_mp3 = True if "mp3" in content_filter.exclude_format else False
    exclude_wav = True if "wav" in content_filter.exclude_format else False
    exclude_cue = True if "cue" in content_filter.exclude_format else False
    exclude_tr = True if "tr" in content_filter.exclude_format else False

    message = None

    for filename in glob.iglob(target_dir + '**/**', recursive=True):
        if not os.path.isdir(filename):
            if "/verse/" in filename and exclude_verse:
                continue

            if "/chapter/" in filename and exclude_chapter:
                continue

            if "/book/" in filename and exclude_book:
                continue

            if "/hi/" in filename and exclude_hi:
                continue

            if "/low/" in filename and exclude_low:
                continue

            if filename.endswith(".mp3") and exclude_mp3:
                continue

            if filename.endswith(".wav") and exclude_wav:
                continue

            if filename.endswith(".cue") and exclude_cue:
                continue

            if filename.endswith(".tr") and exclude_tr:
                continue

            parts = extract_parts(filename)

            if message is None:
                message = {
                    "languageIetf": parts.language_id,
                    "name": f"{parts.language_id}_{parts.resource_id}",
                    "type": "audio",
                    "domain": "scripture",
                    "resourceType": "bible",
                    "namespace": "audio_biel",
                    "files": []
                }

            item = {
                "size": os.path.getsize(filename),
                "url": path_to_url(filename),
                "fileType": Path(filename).suffix[1:],
                "hash": calc_md5_hash(filename),
                "isWholeBook": parts.chapter is None,
                "isWholeProject": False,
                "bookName": get_book_name(books, parts.book_slug),
                "bookSlug": parts.book_slug.capitalize(),
                "chapter": parts.chapter
            }

            items.append(item)

    messages = []

    if message is not None:
        # chunks are done in about this size because azure service bus has a 256kb  size limit, and unchunked a nt in all file types and qualities is 3000+ files, would be over limit. Some prelim testing saw this number of urls consistently come in around 225 kb give or take a little. 
        chunks = split_array(items, 800)
        print(len(chunks))
        for chunk in chunks:
            chunk_message = message.copy()
            chunk_message["files"] = chunk
            messages.append(chunk_message)

        await send_messages(messages)

    return messages


def extract_parts(path_str: str) -> Parts:
    # dependent on cdn file structure
    parts = re.search(re.escape(CONTENT_DIR) + r"/(.+?)/(.+?)/(.+?)(?:/(\d+))?/.*?", path_str)
    return Parts(
        language_id=parts.group(1),
        resource_id=parts.group(2),
        book_slug=parts.group(3),
        chapter=None if parts.group(4) is None else parts.group(4)
    )


def calc_md5_hash(file_path) -> str:
    with open(file_path) as file, mmap(file.fileno(), 0, access=ACCESS_READ) as file:
        return md5(file).hexdigest()


def get_book_name(books, slug) -> str:
    book = next((sub for sub in books if sub["slug"] == slug), None)
    return book["name"] if book is not None else slug.capitalize()


def path_to_url(file_path) -> str:
    return urljoin(CONTENT_URL, file_path)


def split_array(in_array, size):
    return [in_array[i:i + size] for i in range(0, len(in_array), size)]


async def send_messages(messages):
    async with ServiceBusClient.from_connection_string(
        conn_str=NAMESPACE_CONNECTION_STR,
        logging_enable=True
    ) as service_bus_client:
        sender = service_bus_client.get_queue_sender(queue_name=QUEUE_NAME)
        async with sender:
            batch_message = await sender.create_message_batch()
            for message in messages:
                try:
                    batch_message.add_message(ServiceBusMessage(json.dumps(message)))
                except ValueError:
                    break

                await sender.send_messages(batch_message)
