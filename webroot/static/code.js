const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
fetch(servicesRequest)
    .then(function (response) {
        return response.json();
    })
    .then(function (serviceList) {
        serviceList.forEach(service => {
            var li = document.createElement("li");
            li.appendChild(document.createTextNode(service.name + ': ' + service.status + ': ' + service.creationDate + ': ' + service.url));
            // TODO add delete button
            var button = document.createElement("button");
            button.innerHTML = "x";
            button.onclick = evt => deleteService(service.url);
            li.appendChild(button);
            listContainer.appendChild(li);
        });
    });

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let urlName = document.querySelector('#url-name').value;
    let name = document.querySelector('#name').value;
    fetch('/service', {
        method: 'post',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({url: urlName, name: name})
    }).then(res => location.reload());
}

function deleteService(url) {
        fetch('/service', {
            method: 'delete',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({url: url})
        }).then(res => location.reload());
}
