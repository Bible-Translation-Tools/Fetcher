window.addEventListener('load', function() {
    document.querySelector(".searchbar__input").addEventListener("keyup", search)
    document.querySelector(".searchbar__input ~ em").addEventListener("click", search)
    document.querySelector(".searchbar__input ~ em").addEventListener("mouseover", function(event){
        event.target.style.cursor = "pointer"
    })
})


function search(event) {
    let searchBox = document.querySelector(".searchbar__input")
    let text = searchBox.value
    if ((event.keyCode != undefined && event.keyCode != 13) ||
        text.trim() === "") {
        return
    }

    let url = window.location.href + "?search=" + text
    fetch(url).then(response => {
        if (!response.ok) {
            throw new Error('An error occurred when requesting ' + url);
        }
        return response.text()
    }).then(data => {
        renderSuccess(data)
    }).catch(error => {
        console.log(error)
    })
}

function renderSuccess(data) {
    document.querySelector(".l-list-container").innerHTML = data
}