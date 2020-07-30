let list = document.getElementById("reports-list");
reportsPaths.forEach((path, index) => {
    let a = document.createElement("a");
    a.innerText = `Report ${index + 1}`;
    a.href = path;
    let li = document.createElement("li");
    li.appendChild(a);
    list.appendChild(li);
});