import logging
import os
import subprocess
from pathlib import Path
from typing import List, Optional

# Resolve the CLI jars relative to this file so callers that chdir (e.g. update_repo.py)
# still find them.
TOOLS_DIR = Path(__file__).resolve().parent / "tools"


def _jar(name: str) -> List[str]:
    return ["java", "-jar", str(TOOLS_DIR / name)]


def split_chapter(input_file, output_dir, verbose=False):
    run_process(
        _jar("tr-chunk-browser-cli.jar") + ["-s", "-f", str(input_file), "-o", str(output_dir)],
        input_file,
        verbose
    )


def convert_to_mp3(input_file_or_dir, bitrate, delete, verbose=False):
    command = _jar("audio-compressor-cli.jar") + ["-f", "mp3", "-b", str(bitrate)]
    if delete:
        command.append("-d")
    command += ["-i", str(input_file_or_dir)]
    run_process(command, input_file_or_dir, verbose)


def create_tr(input_dir, verbose=False):
    run_process(
        _jar("aoh-cli.jar") + ["-c", "-tr", str(input_dir)],
        input_dir,
        verbose
    )


def git_clone(url, verbose=False):
    # "--" stops a crafted URL from being parsed as a git option
    run_process(["git", "clone", "--", str(url)], None, verbose)


def pull_all_repos(verbose=False):
    # Pull every repository directly under the current directory. A failure in one repo
    # is logged and does not stop the others (same as the previous `find -exec` behaviour).
    for entry in sorted(os.scandir("."), key=lambda e: e.name):
        if not entry.is_dir(follow_symlinks=False):
            continue
        try:
            run_process(["git", "-C", entry.path, "pull"], None, verbose)
        except Exception as ex:
            logging.warning(str(ex))


def run_process(command: List[str], input_file: Optional[Path] = None, verbose=False):
    # Arguments are passed as a list with no shell, so file names containing shell
    # metacharacters (; | $ ` etc.) cannot inject commands.
    process = subprocess.run(
        command,
        capture_output=True,
        universal_newlines=True,
    )

    input_name = Path(input_file).name if input_file is not None else "n/a"

    if process.returncode != 0:
        error_data = {
            "input_file": input_name,
            "command": " ".join(command),
            "stacktrace": process.stderr
        }

        logging.error(f"Process error: file {input_name}", extra=error_data)
        raise Exception(f"There was an error in the process. {' '.join(command)}")
    else:
        if verbose:
            logging.debug(process.stdout)
