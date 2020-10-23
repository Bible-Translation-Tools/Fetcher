function requestContent(event, element) {
    event.stopPropagation();

    let requestUrl = element.dataset.requestUrl;
    let downloadStatesContainer = element.closest('.chaptercard__download-target');
    
    downloadStatesContainer.querySelectorAll(
        ".chaptercard__download-target p, .chaptercard__icon"
    ).forEach(item => {
        item.classList.add("hidden");
    });
    downloadStatesContainer.querySelectorAll(
        ".chaptercard__spinner, p[name='requesting-text']"
    ).forEach(item => {
        item.classList.remove("hidden");
    });

    // TODO: fetch some stuff with the requestUrl and make changes based on the response...

}

function requestLinkHandler(event) {
    let card = this;
    if (card.href == "javascript:void(0)") {
        event.preventDefault();
        return;
    }
    // hide all the stuff
    card.querySelectorAll(".chaptercard__download-target p,.chaptercard__icon").forEach(item => {
        item.classList.add("hidden")
    });
    // unhide the "Requesting" button and downloading spinner
    card.querySelectorAll("p[name='requesting-text'],.chaptercard__spinner").forEach(item => {
        item.classList.remove("hidden")
    });
    card.querySelector(".chaptercard__download-target").classList.add("requesting")
    let url = card.href;
    card.href = "javascript:void(0)"; // prevent multi click
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('Request returned error');
        }
        return response.text()
    }).then(data => {
        card.href = data;
        card.setAttribute("target", "_blank");
        card.querySelector(".chaptercard__icon").textContent = "get_app";
        card.querySelector(".chaptercard__download-target").classList.add("download-ready");
        card.querySelector(".chaptercard__download-target").classList.remove("requesting");
        card.querySelectorAll(".chaptercard__download-target p,.chaptercard__spinner").forEach(item => {
            item.classList.add("hidden")
        });
        card.querySelectorAll("p[name='download-text'],.chaptercard__icon").forEach(item => {
            item.classList.remove("hidden")
        });
    }).catch(error => {
        console.log(error);
        card.href = "javascript:void(0)";
        card.classList.add("unavailable");
        card.querySelectorAll("div").forEach(item => {
            item.classList.add("unavailable")
        });
        card.querySelectorAll("p[name='requesting-text'],.chaptercard__spinner").forEach(item => {
            item.classList.add("hidden")
        });
    });
    card.removeEventListener('click', requestLinkHandler);
}

function downloadRC(url) {
    if (response.ok) {
        element.href = response.text();
        element.querySelector(".chaptercard__icon").textContent = "get_app";
        element.querySelector(".chaptercard__download-text").textContent = "Download";
    } else {
        // render chapter unavailable
        element.querySelector(".chaptercard__download-text").textContent = "Unavailable";
    }
}