import logging
import subprocess
from datetime import datetime


def fix_metadata(input_file, verbose=False):
    run_process(
        f'java -jar tools/bttConverter.jar -f {input_file} -m chunk',
        verbose
    )


def split_chapter(input_file, output_dir, verbose=False):
    run_process(
        f'java -jar tools/tr-chunk-browser-cli.jar -s -f {input_file} -o {output_dir}',
        verbose
    )


def convert_to_mp3(input_file_or_dir, bitrate, delete, verbose=False):
    should_delete = '-d' if delete else ''
    run_process(
        f'java -jar tools/audio-compressor-cli.jar -f mp3 -b {bitrate} {should_delete} -i {input_file_or_dir}',
        verbose
    )


def create_tr(input_dir, verbose=False):
    run_process(
        f'java -jar tools/aoh-cli.jar -c -tr {input_dir}',
        verbose
    )


def git_clone(url, verbose=False):
    run_process(
        f'git clone {url}',
        verbose
    )


def pull_all_repos(verbose=False):
    run_process(
        'find . -mindepth 1 -maxdepth 1 -type d -print -exec git -C {} pull \\;',
        verbose
    )


def run_process(command, verbose=False):
    process = subprocess.run(
        command,
        capture_output=True,
        universal_newlines=True,
        shell=True
    )

    if process.returncode != 0:
        error_data = {
            "command": command,
            "error": process.stderr
        }
        time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        logging.error(f"Fetcher Error {time}", extra=error_data)
        raise Exception(f"There was an error in a process. {process.stderr}")
    else:
        if verbose:
            logging.debug(process.stdout)
