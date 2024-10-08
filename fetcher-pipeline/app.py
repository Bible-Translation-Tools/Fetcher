import argparse
import asyncio
import json
import logging
import os
from argparse import Namespace
from pathlib import Path
import sys
from time import sleep, time
import traceback
from typing import Tuple, List
from datetime import datetime
from urllib.parse import urljoin
from file_utils import calc_md5_hash
import sentry_sdk
from azure.servicebus.aio import ServiceBusClient
from azure.servicebus import ServiceBusMessage

from chapter_worker import ChapterWorker
from tr_worker import TrWorker
from verse_worker import VerseWorker
from book_worker import BookWorker


class App:

    def __init__(
        self,
        input_dir: Path,
        verbose=False,
        hour=1,
        minute=0,
        message_queue_exclude=[],
        bus_connection_string="",
        bus_topic="",
    ):
        self.__ftp_dir = input_dir
        self.verbose = verbose
        self.hour = hour
        self.minute = minute
        self.message_queue_exclude_args = message_queue_exclude
        self.BUS_CONNECTION_STRING = bus_connection_string
        self.BUS_TOPIC = bus_topic

    def start(self):
        """Start app"""

        chapter_worker = ChapterWorker(self.__ftp_dir, self.verbose)
        verse_worker = VerseWorker(self.__ftp_dir, self.verbose)
        tr_worker = TrWorker(self.__ftp_dir, self.verbose)
        book_worker = BookWorker(self.__ftp_dir, self.verbose)

        wait_timer = (self.hour * 3600) + (self.minute * 60)

        if wait_timer == 0:
            logging.debug("Set timer to more than zero minutes")
            exit(0)

        while True:
            # Set will be shared amongst workers mutably to avoid multiple rglobs. Yes, this does read into memory, but you can't rewind an rglob generator, and some of the workers need more than a single file at a time, so this saves any nested glob looksups as well.  Ad hoc testing suggests paths of this size, can fit about 50 million into 2gigs of memory, and this should be fewer than that. As for speed, more ad hoc testing showed It taking about 1 or 2 minutes to glob 250,000 files.  If this chokes, should probably looking to getting "all_files" by iter_dir each language I think, and then aggregating or just spawning workers for each language.
            all_files = self.glob_all_into_set()
            # Each worker mutates the set of all_files for any created/deleted files.
            chapter_worker.execute(all_files)
            verse_worker.execute(all_files)
            tr_worker.execute(all_files)
            book_worker.execute(all_files)
            report = self.get_report(
                (
                    chapter_worker.get_report(),
                    verse_worker.get_report(),
                    tr_worker.get_report(),
                    book_worker.get_report(),
                )
            )
            if report is not None:
                time = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                logging.info(f"Fetcher Pipeline Report {time}", extra=report)
            self.send_messages_to_queue(self.message_queue_exclude_args)
            # free all_files from memory while sleeping for next run, but also to get a fresh set in case anythign updated.
            all_files.clear()
            sleep(wait_timer)

    def glob_all_into_set(self):
        """Glob all files into a set"""
        start_time = time()
        all_files = set(sorted(self.__ftp_dir.rglob("*"), key=lambda p: str(p).lower()))
        end_time = time()
        logging.info(
            f"Elapsed time to glob all into a set files: {end_time - start_time}"
        )
        logging.info(f"Total number of files: {len(all_files)}")
        size = sys.getsizeof(all_files)
        logging.info(
            f"Size of paths in memory: {size / 1000 } KB or {size / 1000 / 1000} MB"
        )
        return all_files

    @staticmethod
    def get_report(reports):
        """Generate workers report"""

        report = {
            "resources_created": [],
            "resources_deleted": [],
        }
        for r in reports:
            report["resources_created"] += r["resources_created"]
            report["resources_deleted"] += r["resources_deleted"]

        if len(report["resources_created"]) > 0 or len(report["resources_deleted"]) > 0:
            return report

        return None

    def send_messages_to_queue(self, exclude_args: List):
        """Send messages to queue"""
        logging.info("Sending messages to queue")
        try:
            queue_start_time = time()
            cdn_url = os.getenv("CDN_BASE_URL")
            bus_messages = []

            def chunk_array(in_array, size):
                return [in_array[i : i + size] for i in range(0, len(in_array), size)]

            with open("./book_catalog.json", "r") as json_file:
                books = json.load(json_file)

            # iterate through each language
            logging.info(f"iterating through {self.__ftp_dir}")
            for language_dir in self.__ftp_dir.iterdir():
                # don't process stray files at this level, only lang dirs.  e.g. en/
                if not language_dir.is_dir() or "analysis" in language_dir.name:
                    continue
                logging.info(f"doing {language_dir}")
                for project_dir in language_dir.iterdir():
                    # only en/ulb
                    if not project_dir.is_dir():
                        continue
                    logging.debug(f"doing {project_dir}")
                    # Common data for each lang/project
                    common_message_data = None
                    unique_message_data = []
                    # Generate message for each file
                    for file_path in project_dir.rglob("*"):
                        # For each lang/project, get all files, and filter out dirs and excluded args
                        if file_path.is_dir():
                            continue
                        if self.should_skip_file(file_path, exclude_args):
                            continue

                        # given path of /content/etc;
                        root_parts = self.__ftp_dir.parts
                        # exclude the common prefix of content directory and just get lang/proj/book/chap/etc;
                        parts = file_path.parts[len(root_parts) :]
                        lang = parts[0]
                        resource = parts[1]
                        book_slug = parts[2]
                        chapter = parts[3] or None
                        if common_message_data is None:
                            common_message_data = {
                                "languageIetf": lang,
                                "name": f"{lang}/{resource}",
                                "type": "audio",
                                "domain": "scripture",
                                "resourceType": "bible",
                                "namespace": "audio_biel",
                                "files": [],
                                # The session identifier of the message for a sessionful entity. The creates FIFO behavior for subscriptions on azure service bus
                                "session_id": f"audio_biel_{lang}_{resource}",
                            }

                        item = {
                            "size": file_path.stat().st_size,
                            "url": urljoin(cdn_url, str(file_path)),
                            "fileType": file_path.suffix[1:],
                            "hash": calc_md5_hash(file_path),
                            "isWholeBook": chapter is None,
                            "isWholeProject": False,  # no whole projects on the cdn, i.e. no audio bible of entire ulb.
                            "bookName": next(
                                (
                                    sub.get("name")
                                    for sub in books
                                    if sub["slug"] == book_slug
                                ),
                                None,
                            )
                            or book_slug.capitalize(),
                            "bookSlug": book_slug.capitalize(),
                            "chapter": chapter,
                        }
                        unique_message_data.append(item)
                        # For each lang/project, get all files, and filter out dirs and excluded args
                    # Now that we have file info for each, genearte final messages by merging common and unique properties into a single message of common + files of batch size N. Part of the for project_dir in language_dir loop. 500 is an arbitrary number that eems to given enough padding to not go over the 256 limit.
                    if common_message_data is not None:
                        chunks = chunk_array(unique_message_data, 500)
                        for chunk in chunks:
                            chunk_message = common_message_data.copy()
                            chunk_message["files"] = chunk
                            bus_messages.append(chunk_message)
            logging.info(f"Sending {len(bus_messages)} messages to queue")
            if len(bus_messages) > 0:
                # The whole loop isn't managed by asyncio, but we can run just this one coroutine with it. it'll set up and tear down an event loop as needed.
                asyncio.run(self.send_messages(bus_messages))
                queue_end_time = time()
                logging.info(
                    f"Queue finished in {queue_end_time - queue_start_time} seconds!"
                )
                # self.send_messages(bus_messages)
            else:
                logging.info("No messages to send")
        except Exception as e:
            logging.critical(f"Error sending messages to queue:{e}")
            traceback.print_exc()
            # blow up. If we can't send messages to bus, data not made availble in api.
            sys.exit(1)

    async def send_messages(self, messages):

        async with ServiceBusClient.from_connection_string(
            conn_str=self.BUS_CONNECTION_STRING, logging_enable=True
        ) as service_bus_client:
            sender = service_bus_client.get_topic_sender(topic_name=self.BUS_TOPIC)
            async with sender:
                # batch_message = await sender.create_message_batch()
                # sender.
                for message in messages:
                    try:
                        bus_message = ServiceBusMessage(
                            json.dumps(message), session_id=message["session_id"]
                        )
                        await sender.send_messages(bus_message)
                        # batch_message.add_message(bus_message)
                    except Exception as e:
                        logging.error(e)
                        logging.critical(
                            "Error sending messages to queue. Exiting since data won't make it to the api."
                        )
                        sys.exit(1)

                logging.debug(
                    f"Done sending messages to queue. Sent {len(messages)} messages."
                )

    @staticmethod
    def should_skip_file(file_path: Path, exlude_args: List[str]) -> bool:
        for arg in exlude_args:
            if arg.startswith("."):
                if file_path.suffix == arg:
                    return True
                # The parts look like ('/', 'content', 'hi', 'ulb', 'eph', '2', 'CONTENTS', 'tr', 'mp3', 'hi', 'verse', 'hi_ulb_eph_c2.tr')... We don't want to include "/", "content", "lang", or "ulb (project type) in the filter" So, slice to get "eph/2/CONTENTS/tr/mp3/hi/verse/hi_ulb_eph_c2.tr" for example
            elif f"/{arg}/" in "/".join(file_path.parts[4:]):
                return True
            elif ".hash" == file_path.name:
                return True
        return False


def get_arguments() -> Tuple[Namespace, List[str]]:
    """Parse command line arguments"""

    exclude_args_choices = [
        "verse",
        "chapter",
        "book",
        "hi",
        "low",
        ".mp3",
        ".wav",
        ".cue",
        ".tr",
    ]
    log_level_choices = ["debug", "info", "warning", "error", "critical"]

    parser = argparse.ArgumentParser(
        description="Split and convert chapter files to mp3"
    )
    parser.add_argument("-i", "--input-dir", type=Path, help="Input directory")
    parser.add_argument(
        "-l",
        "--log_level",
        choices=log_level_choices,
        default="info",
        help="Set logging level",
    )
    parser.add_argument(
        "-v", "--verbose", action="store_true", help="Enable logs from subprocess"
    )
    parser.add_argument(
        "-hr",
        "--hour",
        type=int,
        default=1,
        help="Frequency of executing workers in hours",
    )
    parser.add_argument(
        "-mn",
        "--minute",
        type=int,
        default=0,
        help="Frequency of executing workers in minutes",
    )
    parser.add_argument(
        "-mqe",
        "--queue_exclude",
        nargs="*",
        help=f"Exclude files from passing to queue. Options: {exclude_args_choices}",
        choices=exclude_args_choices,
    )

    return parser.parse_known_args()


def main():
    """Launch application"""
    args, unknown = get_arguments()

    log_level = {
        "debug": logging.DEBUG,
        "info": logging.INFO,
        "warning": logging.WARNING,
        "error": logging.ERROR,
        "critical": logging.CRITICAL,
    }.get(args.log_level, logging.INFO)

    logging.basicConfig(
        format="%(asctime)s - %(levelname)s: %(message)s", level=log_level
    )

    sentry_sdk.init(os.getenv("SENTRY_DSN"), traces_sample_rate=0.0)
    BUS_CONNECTION_STR = os.getenv("SERVICE_BUS_CONNECTION_STRING")
    TOPIC_NAME = os.getenv("SERVICE_BUS_TOPIC_NAME")

    if os.getenv("CDN_BASE_URL") is None:
        raise Exception("CDN_BASE_URL for bus queue is not set")
    if BUS_CONNECTION_STR is None:
        raise Exception("SERVICE_BUS_CONNECTION_STRING is not set")
    if TOPIC_NAME is None:
        raise Exception("SERVICE_BUS_TOPIC_NAME is not set")
    app = App(
        args.input_dir,
        args.verbose,
        args.hour,
        args.minute,
        args.queue_exclude,
        BUS_CONNECTION_STR,
        TOPIC_NAME,
    )
    logging.info(args)
    app.start()


if __name__ == "__main__":
    main()
