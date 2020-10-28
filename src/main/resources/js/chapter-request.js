window.addEventListener('DOMContentLoaded', (event) => {
    document.querySelectorAll(".chaptercard__icon").forEach(element => {
        element.textContent = "send"
    })

    document.querySelectorAll(".chaptercard").forEach(card => {
            card.addEventListener('click', requestLinkHandler)
    })
})

function requestLinkHandler(event) {
    event.preventDefault()
    let card = this
    card.href = "javascript:void(0)" // prevent multiple request
    card.querySelectorAll(
        ".chaptercard__download-target p,.chaptercard__icon"
    ).forEach(item => {
        item.classList.add("hidden")
    })
    card.querySelectorAll(
        "p[name='requesting-text'],.chaptercard__spinner"
    ).forEach(item => {
        item.classList.remove("hidden") // show spinner
    })
    card.querySelector(".chaptercard__download-target").classList.add("requesting")

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
    card.removeEventListener('click', requestLinkHandler)
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