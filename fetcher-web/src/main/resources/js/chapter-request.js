window.addEventListener('DOMContentLoaded', (event) => {
    document.querySelectorAll(".chaptercard").forEach(card => {
        card.addEventListener('click', requestChapterHandler)
    })

    document.querySelectorAll(".chapter-card-error .try_again").forEach(card => {
        card.addEventListener('click', retryChapterDownload)
    })

    document.querySelector(".book-download-card__button").addEventListener(
        'click', requestBookHandler
    )

    document.querySelector(".book-card-error .try_again").addEventListener(
        'click', retryBookDownload
    )
})

function validateUrl(url) {
    try {
        let urlObject = new URL(url)
        if (!urlObject.protocol.startsWith("http")) return false
    } catch (err) {
        return false
    }
    return true
}

function requestChapterHandler(event) {
    event.preventDefault()
    let card = this
    if (!validateUrl(card.href)) return

    card.href = "javascript:void(0)" // prevent multiple request
    card.querySelectorAll(
        ".chaptercard__download-target p,.chaptercard__icon"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    card.querySelector(".chaptercard__download-target").classList.add("requesting")
    card.querySelectorAll(
        "p[name='requesting-text'],.chaptercard__spinner"
    ).forEach(item => {
        item.classList.remove("hidden") // show spinner
    })

    let url = window.location.pathname + '/' + card.dataset.chapterId
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('Requested content is not available');
        }
        return response.text()
    }).then(data => {
        card.href = data
        renderChapterSuccess(card)
    }).catch(error => {
        console.log(error)
        renderChapterError(card)
    })
    card.removeEventListener('click', requestChapterHandler)
}

function renderChapterSuccess(card) {
    card.setAttribute("target", "_blank")
    card.querySelector(".chaptercard__download-target").classList.remove("requesting")
    card.querySelector(".chaptercard__icon").textContent = "get_app"
    card.querySelectorAll(
        ".chaptercard__download-target p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    card.querySelectorAll(
        "p[name='download-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })

    initiateUrlDownload(card.href)
}

function renderChapterError(card) {
    card.classList.add("unavailable")
    card.querySelector(".chaptercard__title").classList.add("unavailable")
    card.querySelector(".chaptercard__download-target").classList.remove("requesting")
    card.querySelectorAll(
        ".chaptercard__download-target p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    card.querySelectorAll(
        "p[name='download-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })

    card.href = "#"
    card.addEventListener('click', requestChapterHandler)
    card.classList.add("hidden")

    let chapter = card.dataset.chapterId
    let error = document.querySelector(".chapter-card-error[data-chapter-id='"+chapter+"']")
    error.classList.remove("hidden")
}

function retryChapterDownload(event) {
    event.preventDefault()

    let error = this.closest(".chapter-card-error")
    error.classList.add("hidden")

    let chapter = error.dataset.chapterId

    let chapterCard = document.querySelector(".chaptercard[data-chapter-id='"+chapter+"']")
    chapterCard.classList.remove("hidden")
    chapterCard.querySelector(".chaptercard__title").classList.remove("unavailable")

    chapterCard.click()
}

function requestBookHandler(event) {
    event.preventDefault()
    let element = this
    if (!validateUrl(element.href)) return

    element.href = "javascript:void(0)" // prevent multiple request
    element.classList.add("requesting")
    element.querySelectorAll(
        "p,.chaptercard__icon"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    element.querySelectorAll(
        "p[name='requesting-text'],.chaptercard__spinner"
    ).forEach(item => {
        item.classList.remove("hidden") // show spinner
    })

    let url = window.location.pathname + '/' + element.dataset.chapterId
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('Requested content is not available');
        }
        return response.text()
    }).then(data => {
        element.href = data
        renderBookSuccess(element)
    }).catch(error => {
        console.log(error)
        renderBookError(element)
    })
    element.removeEventListener('click', requestBookHandler)
}

function renderBookSuccess(element) {
    element.setAttribute("target","_blank")
    element.classList.remove("requesting")
    element.querySelector(".chaptercard__icon").textContent = "get_app"
    element.querySelectorAll(
        "p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    element.querySelectorAll(
        "p[name='download-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })

    initiateUrlDownload(element.href)
}

function renderBookError(element) {
    element.classList.remove("requesting")
    element.querySelectorAll(
        "p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    element.querySelectorAll(
        "p[name='unavailable-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })
    element.href = "#"
    element.addEventListener('click', requestBookHandler)
    element.classList.add("hidden")

    document.querySelector(".book-card-error").classList.remove("hidden")
}

function retryBookDownload(event) {
    event.preventDefault()

    let error = this.closest(".book-card-error")
    error.classList.add("hidden")

    let bookCard = document.querySelector(".book-download-card__button")
    bookCard.classList.remove("hidden")

    bookCard.click()
}

function initiateUrlDownload(url) {
    let a = document.createElement('a');
    a.href = url;
    document.body.appendChild(a);
    a.click();
    a.remove();
}
