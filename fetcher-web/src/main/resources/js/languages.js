window.addEventListener('load', function() {
    document.querySelectorAll(".searchbar__input").forEach(function(elem, i) {
        elem.addEventListener("keyup", search)
        elem.id = "searchbar_".concat(i + 1)
        elem.setAttribute("data-target", "searchbar_".concat(i + 1))
    })
    document.querySelectorAll(".searchbar__input ~ em").forEach(function(elem, i) {
        elem.addEventListener("click", search)
        elem.addEventListener("mouseover", function(event) {
            event.target.style.cursor = "pointer"
        })
        elem.setAttribute("data-target", "searchbar_".concat(i + 1))
    })
})

function loadMore() {
    let listContainer = document.querySelector(".l-list-container")
    let index = listContainer.children.length - listContainer.dataset.offsetCount
    if (index < 0) return

    let url = `${window.location.origin}${window.location.pathname}/load-more?index=${index}`
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('An error occurred when requesting ' + url);
        }
        return response.text()
    }).then(data => {
        listContainer.innerHTML += data
    }).catch(error => {
        console.log(error)
    })
}

function loadMoreSearchResult() {
    let listContainer = document.querySelector(".l-list-container")
    let index = listContainer.children.length
    let text = document.querySelector(".searchbar__input").value

    let url = window.location.href + `/filter?keyword=${text}&index=${index}`
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('An error occurred when requesting ' + url);
        }
        return response.text()
    }).then(data => {
        listContainer.innerHTML += data
        updateBottomSection()
    }).catch(error => {
        console.log(error)
        updateBottomSection(true)
    })
}

function search(event) {
    let targetId = event.target.getAttribute("data-target")
    let searchBox = document.querySelector("#" + targetId)
    let text = searchBox.value

    document.querySelectorAll(".searchbar__input").forEach(elem =>
        elem.value = text
    )

    if ((event.keyCode != undefined && event.keyCode != 13) ||
        text.trim() === "") {
        return
    }

    let url = window.location.href + "/filter?keyword=" + text
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('An error occurred when requesting ' + url);
        }
        return response.text()
    }).then(data => {
        renderSearchResult(data)

        if (searchBox.id == "searchbar_2"){
            document.querySelector("body").scroll(0,0)
        }
    }).catch(error => {
        console.log(error)
        updateBottomSection(true)
    })
}

function renderSearchResult(data) {
    let listContainer = document.querySelector(".l-list-container")
    listContainer.innerHTML = data
    listContainer.dataset.offsetCount = 0

    document.querySelector(".load_more_button").setAttribute("onclick", "loadMoreSearchResult()")
    updateBottomSection()
}

function updateBottomSection(err = false) {
    if (document.getElementById("isLast") != null || err) {
        // inactivate "show more"
        document.querySelectorAll(
            ".bottom_section__info,a.load_more_button"
        ).forEach(elem =>
            elem.classList.add("hidden")
        )
    } else {
        document.querySelectorAll(
            ".bottom_section__info,a.load_more_button"
        ).forEach(elem =>
            elem.classList.remove("hidden")
        )
    }
}