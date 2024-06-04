import os

language = "en"
resource = "ulb"
books = [
    {"book": "gen", "chapters": 50},
    {"book": "exo", "chapters": 40},
    {"book": "lev", "chapters": 27},
    {"book": "num", "chapters": 36},
    {"book": "deu", "chapters": 34},
    {"book": "jos", "chapters": 24},
    {"book": "jdg", "chapters": 21},
    {"book": "rut", "chapters": 4},
    {"book": "1sa", "chapters": 31},
    {"book": "2sa", "chapters": 24},
    {"book": "1ki", "chapters": 22},
    {"book": "2ki", "chapters": 25},
    {"book": "1ch", "chapters": 29},
    {"book": "2ch", "chapters": 36},
    {"book": "ezr", "chapters": 10},
    {"book": "neh", "chapters": 13},
    {"book": "est", "chapters": 10},
    {"book": "job", "chapters": 42},
    {"book": "psa", "chapters": 150},
    {"book": "pro", "chapters": 31},
    {"book": "ecc", "chapters": 12},
    {"book": "sng", "chapters": 8},
    {"book": "isa", "chapters": 66},
    {"book": "jer", "chapters": 52},
    {"book": "lam", "chapters": 5},
    {"book": "ezk", "chapters": 48},
    {"book": "dan", "chapters": 12},
    {"book": "hos", "chapters": 14},
    {"book": "jol", "chapters": 3},
    {"book": "amo", "chapters": 9},
    {"book": "oba", "chapters": 1},
    {"book": "jon", "chapters": 4},
    {"book": "mic", "chapters": 7},
    {"book": "nam", "chapters": 3},
    {"book": "hab", "chapters": 3},
    {"book": "zep", "chapters": 3},
    {"book": "hag", "chapters": 2},
    {"book": "zec", "chapters": 14},
    {"book": "mal", "chapters": 4},
    {"book": "mat", "chapters": 28},
    {"book": "mrk", "chapters": 16},
    {"book": "luk", "chapters": 24},
    {"book": "jhn", "chapters": 21},
    {"book": "act", "chapters": 28},
    {"book": "rom", "chapters": 16},
    {"book": "1co", "chapters": 16},
    {"book": "2co", "chapters": 13},
    {"book": "gal", "chapters": 6},
    {"book": "eph", "chapters": 6},
    {"book": "php", "chapters": 4},
    {"book": "col", "chapters": 4},
    {"book": "1th", "chapters": 5},
    {"book": "2th", "chapters": 3},
    {"book": "1ti", "chapters": 6},
    {"book": "2ti", "chapters": 4},
    {"book": "tit", "chapters": 3},
    {"book": "phm", "chapters": 1},
    {"book": "heb", "chapters": 13},
    {"book": "jas", "chapters": 5},
    {"book": "1pe", "chapters": 5},
    {"book": "2pe", "chapters": 3},
    {"book": "1jn", "chapters": 5},
    {"book": "2jn", "chapters": 1},
    {"book": "3jn", "chapters": 1},
    {"book": "jud", "chapters": 1},
    {"book": "rev", "chapters": 22}
]

root_dir = "./content"
resource_dir = os.path.join(root_dir, language, resource)


def create_book(parent_dir, book, file_format):
    os.makedirs(parent_dir, exist_ok=True)
    file_name = f"{language}_{resource}_{book}.{file_format}"
    file = os.path.join(parent_dir, file_name)
    with open(file, "w") as target_file:
        target_file.write("book audio data")


def create_chapter(parent_dir, book, chapter, file_format):
    os.makedirs(parent_dir, exist_ok=True)
    file_name = f"{language}_{resource}_{book}_c{chapter}.{file_format}"
    file = os.path.join(parent_dir, file_name)
    with open(file, "w") as target_file:
        target_file.write("chapter audio data")


def create_verses(parent_dir, book, chapter, file_format):
    os.makedirs(parent_dir, exist_ok=True)

    for v in range(1, 11):
        file_name = f"{language}_{resource}_{book}_c{chapter}"

        if book == "psa":
            verse = str(v).zfill(3)
        else:
            verse = str(v).zfill(2)

        file_name = f"{file_name}_v{verse}.{file_format}"
        file = os.path.join(parent_dir, file_name)
        with open(file, "w") as target_file:
            target_file.write("verse audio data")


def run():
    for book_dic in books:
        book = book_dic["book"]
        book_dir = os.path.join(resource_dir, book)
        file_formats = ["cue", "mp3", "tr", "wav"]

        contents_dir = os.path.join(book_dir, "CONTENTS")
        os.makedirs(contents_dir, exist_ok=True)

        for file_format in file_formats:
            if file_format == "cue":
                continue
            elif file_format == "mp3":
                contents_mp3_dir = os.path.join(contents_dir, "mp3")
                contents_mp3_hi_dir = os.path.join(contents_mp3_dir, "hi", "book")
                contents_mp3_low_dir = os.path.join(contents_mp3_dir, "low", "book")

                create_book(contents_mp3_hi_dir, book, file_format)
                create_book(contents_mp3_low_dir, book, file_format)
            elif file_format == "wav":
                contents_wav_dir = os.path.join(contents_dir, "wav", "book")
                create_book(contents_wav_dir, book, file_format)
            elif file_format == "tr":
                contents_tr_dir = os.path.join(contents_dir, "tr")
                contents_tr_wav_dir = os.path.join(contents_tr_dir, "wav", "verse")
                contents_tr_mp3_dir = os.path.join(contents_tr_dir, "mp3")
                contents_tr_mp3_hi_dir = os.path.join(contents_tr_mp3_dir, "hi", "verse")
                contents_tr_mp3_low_dir = os.path.join(contents_tr_mp3_dir, "low", "verse")

                create_book(contents_tr_wav_dir, book, file_format)
                create_book(contents_tr_mp3_hi_dir, book, file_format)
                create_book(contents_tr_mp3_low_dir, book, file_format)

        for chapter in range(1, (book_dic["chapters"] + 1)):
            chapter_dir = os.path.join(book_dir, str(chapter), "CONTENTS")

            for file_format in file_formats:
                format_dir = os.path.join(chapter_dir, file_format)

                if file_format == "cue" or file_format == "wav":
                    format_chapter_dir = os.path.join(format_dir, "chapter")
                    format_verse_dir = os.path.join(format_dir, "verse")

                    create_chapter(format_chapter_dir, book, chapter, file_format)
                    create_verses(format_verse_dir, book, chapter, file_format)

                elif file_format == "mp3":
                    hi_dir = os.path.join(format_dir, "hi")
                    low_dir = os.path.join(format_dir, "low")

                    hi_chapter_dir = os.path.join(hi_dir, "chapter")
                    hi_verse_dir = os.path.join(hi_dir, "verse")

                    create_chapter(hi_chapter_dir, book, chapter, file_format)
                    create_verses(hi_verse_dir, book, chapter, file_format)

                    low_chapter_dir = os.path.join(low_dir, "chapter")
                    low_verse_dir = os.path.join(low_dir, "verse")

                    create_chapter(low_chapter_dir, book, chapter, file_format)
                    create_verses(low_verse_dir, book, chapter, file_format)
                elif file_format == "tr":
                    tr_mp3_dir = os.path.join(format_dir, "mp3")
                    tr_wav_dir = os.path.join(format_dir, "wav", "verse")

                    tr_mp3_hi_dir = os.path.join(tr_mp3_dir, "hi", "verse")
                    tr_mp3_low_dir = os.path.join(tr_mp3_dir, "low", "verse")

                    create_chapter(tr_wav_dir, book, chapter, file_format)
                    create_verses(tr_mp3_hi_dir, book, chapter, file_format)
                    create_verses(tr_mp3_low_dir, book, chapter, file_format)


run()
