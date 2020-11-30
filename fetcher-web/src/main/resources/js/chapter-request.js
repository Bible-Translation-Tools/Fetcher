window.addEventListener('DOMContentLoaded', (event) => {
    document.querySelector(
    ".chapter-download-card__button"
    ).classList.add("request-book")

    document.querySelectorAll(".chaptercard__icon").forEach(element => {
        element.textContent = "send"
    })

    document.querySelectorAll(".chaptercard").forEach(card => {
        card.addEventListener('click', requestChapterHandler)
    })

    document.querySelector(".chapter-download-card__button").addEventListener(
        'click', requestBookHandler
    )
})

function validateUrl(url) {
    try {
        let validateUrl = new URL(url)
        if (!validateUrl.protocol.startsWith("http")) return false
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
        renderSuccess(data, card)
    }).catch(error => {
        console.log(error)
        renderError(card)
    })
    card.removeEventListener('click', requestChapterHandler)
}

function renderSuccess(data, card) {
    card.href = data
    card.setAttribute("target", "_blank")
    card.querySelector(".chaptercard__icon").textContent = "get_app"
    card.querySelector(".chaptercard__download-target").classList.add("download-ready")
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
}

function renderError(card) {
    card.classList.add("unavailable")
    card.querySelector(".chaptercard__title").classList.add("unavailable")
    card.querySelector(".chaptercard__download-target").classList.remove("requesting")
    card.querySelector(".chaptercard__download").classList.add("request-unavailable")
    card.querySelectorAll(
        ".chaptercard__download-target p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    card.querySelector(".chaptercard__icon").textContent = "error"
    card.querySelectorAll(
        "p[name='unavailable-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })
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
    element.classList.remove("requesting","request-book")
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
}

function renderBookError(element) {
    element.classList.remove("requesting")
    element.classList.add("request-unavailable")
    element.querySelectorAll(
        "p,.chaptercard__spinner"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    element.querySelector(".chaptercard__icon").textContent = "error"
    element.querySelectorAll(
        "p[name='unavailable-text'],.chaptercard__icon"
    ).forEach(item => {
        item.classList.remove("hidden")
    })
}