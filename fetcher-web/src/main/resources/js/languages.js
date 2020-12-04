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
    let index = listContainer.children.length - listContainer.dataset.gatewayCount
    let url = `/gl/load-more?index=${index}`

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
        document.querySelector(".l-list-container").innerHTML = data
        if (searchBox.id == "searchbar_2"){
            document.querySelector("body").scroll(0,0)
        }
    }).catch(error => {
        console.log(error)
    })
}
