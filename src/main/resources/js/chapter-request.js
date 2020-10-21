window.addEventListener('DOMContentLoaded', (event) => {
    document.querySelectorAll(".chaptercard__icon").forEach(element => {
        element.textContent = "send"
    })

    document.querySelectorAll(".chaptercard").forEach(card => {
            card.addEventListener('click', event => {
                event.preventDefault()
                card.querySelector(".chaptercard__download-target p,.chaptercard__icon").classList.add("hidden")
                card.querySelector("p[name='requesting-text'],.chaptercard__spinner").classList.remove("hidden")
                let url = card.href
                fetch(url)
                    .then(response => response.text())
                    .then(data => {
                        card.href = data
                        card.querySelector(".chaptercard__icon").textContent = "get_app"
                        card.querySelectorAll(".chaptercard__download-target p,.chaptercard__spinner").forEach(item => {
                            item.classList.add("hidden")
                        })
                        card.querySelectorAll("p[name='download-text'],.chaptercard__icon").forEach(item => {
                            item.classList.remove("hidden")
                        })
                    })
                    .catch(error => {
                        card.href = "javascript:void(0)"
                        card.classList.add("unavailable")
                        card.querySelectorAll("div").forEach(elem => {
                            elem.classList.add("unavailable")
                        })
                        card.querySelector("p[name='requesting-text'],.chaptercard__spinner").classList.add("hidden")
                    })
            })
    })
})

function downloadRC(url) {
    if (response.ok) {
        element.href = response.text()
        element.querySelector(".chaptercard__icon").textContent = "get_app"
        element.querySelector(".chaptercard__download-text").textContent = "Download"
    } else {
        // render chapter unavailable
        element.querySelector(".chaptercard__download-text").textContent = "Unavailable"
    }
}