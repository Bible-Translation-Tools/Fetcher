import logging
import re
import zlib
from pathlib import Path
from tempfile import mkdtemp


def init_temp_dir() -> Path:
    path = Path(mkdtemp())
    return path


def rm_tree(path):
    for child in path.iterdir():
        if child.is_file():
            child.unlink()
        else:
            rm_tree(child)
    path.rmdir()


def copy_dir(src_dir: Path, target_dir: Path, grouping='verse', quality='hi', media=None) -> Path:
    """ Iterate src_dir to copy files """

    t_file = None

    for src_file in src_dir.glob('*.*'):
        f = copy_file(src_file, target_dir, grouping, quality, media)
        if t_file is None:
            t_file = f

    return t_file.parent if t_file is not None else None


def copy_file(src_file: Path, target_dir: Path, grouping='verse', quality='hi', media=None) -> Path:
    """ Copy src_file to specified directory in target_dir """

    path_without_extension = src_file.stem
    path_without_extension = re.sub(r'_t[\d]+$', '', path_without_extension)
    extension = src_file.suffix

    if extension == '.mp3':
        t_dir = target_dir.joinpath(extension[1:], quality, grouping)
    elif extension == '.tr':
        if media is not None:
            if media == 'mp3':
                t_dir = target_dir.joinpath(extension[1:], media, quality, grouping)
            else:
                t_dir = target_dir.joinpath(extension[1:], media, grouping)
        else:
            raise Exception("Media is not defined for TR container")
    else:
        t_dir = target_dir.joinpath(extension[1:], grouping)

    t_file = t_dir.joinpath(path_without_extension + extension)

    logging.debug(f'Copying file: {src_file} to {t_file}')

    if not t_file.exists():
        t_dir.mkdir(parents=True, exist_ok=True)
        t_file.write_bytes(src_file.read_bytes())
        logging.debug('Copied successfully!')
    else:
        logging.debug('File exists, skipping...')

    return t_file


def check_file_exists(file: Path, remote_dir: Path, media: str, grouping='verse', quality='hi') -> bool:
    """ Check if converted version of the source file exists in remote directory """

    path_without_extension = file.stem
    path_without_extension = re.sub(r'_t[\d]+$', '', path_without_extension)

    if media is None:
        raise Exception('Media is not specified')

    if media == 'mp3':
        r_dir = remote_dir.joinpath(media, quality, grouping)
    else:
        r_dir = remote_dir.joinpath(media, grouping)

    r_file = r_dir.joinpath(f'{path_without_extension}.{media}')

    logging.debug(f'Checking file: {r_file}')

    return r_file.exists()


def check_dir_empty(src_dir: Path) -> bool:
    """ Check if directory is empty or doesn't exist """

    return not src_dir.exists() or not any(src_dir.iterdir())


def has_new_files(src_dir: Path, target_dir: Path) -> bool:
    """ Check if files in target_dir are different than in src_dir """

    for s in src_dir.iterdir():
        s_name = re.sub(r'_t[\d]+.*$', s.suffix, s.name)
        t = target_dir.joinpath(s_name)

        if not t.exists():
            return True

        s_hash = zlib.adler32(s.read_bytes())
        t_hash = zlib.adler32(t.read_bytes())

        if s_hash != t_hash:
            return True

    return False


def rel_path(src: Path, root: Path) -> Path:
    return Path(*src.parts[len(root.parts):])
