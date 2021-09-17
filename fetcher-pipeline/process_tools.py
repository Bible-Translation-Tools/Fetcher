import logging
import subprocess
from pathlib import Path
from string import Template


def split_chapter(input_file, output_dir, verbose=False):
    run_process(
        f'java -jar tools/tr-chunk-browser-cli.jar -s -f $input_file -o {output_dir}',
        input_file,
        verbose
    )


def convert_to_mp3(input_file_or_dir, bitrate, delete, verbose=False):
    should_delete = '-d' if delete else ''
    run_process(
        f'java -jar tools/audio-compressor-cli.jar -f mp3 -b {bitrate} {should_delete} -i $input_file',
        input_file_or_dir,
        verbose
    )


def create_tr(input_dir, verbose=False):
    run_process(
        f'java -jar tools/aoh-cli.jar -c -tr $input_file',
        input_dir,
        verbose
    )


def git_clone(url, verbose=False):
    run_process(
        f'git clone {url}',
        None,
        verbose
    )


def pull_all_repos(verbose=False):
    run_process(
        'find . -mindepth 1 -maxdepth 1 -type d -print -exec git -C {} pull \\;',
        None,
        verbose
    )


def run_process(command, input_file: Path = None, verbose=False):
    template = Template(command)
    template_command = template.substitute(input_file=input_file)

    process = subprocess.run(
        template_command,
        capture_output=True,
        universal_newlines=True,
        shell=True
    )

    if process.returncode != 0:
        error_data = {
            "input_file": input_file.name,
            "command": template.template,
            "stacktrace": process.stderr
        }

        logging.error(f"Process error: file {input_file.name if input_file else 'n/a'}", extra=error_data)
        raise Exception(f"There was an error in the process. {template_command}")
    else:
        if verbose:
            logging.debug(process.stdout)
