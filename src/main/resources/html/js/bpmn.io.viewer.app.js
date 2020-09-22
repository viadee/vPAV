//mark all nodes with issues
function markNodes(elements, canvas) {
    for (let element of elements) {
        try {
            canvas.addMarker(element.elementId, element.classification.toLowerCase());
        } catch (err) {
            console.log("element not found");
        }
    }
}

function filterElementsByModel(elements, bpmnFile) {
    return elements.filter(element => (element.bpmnFile === (properties["basepath"] + bpmnFile)) && (element.elementId !== ""));
}

//mark invalid path
function getElementsOnPath(id, pos) {
    let pathElements = [];
    for (let y in elementsToMark) {
        if (elementsToMark[y].id === id) {
            for (let x in elementsToMark[y].paths[pos]) {
                if (elementsToMark[y].paths[pos][x].elementId !== "")
                    pathElements.push({elementId: elementsToMark[y].paths[pos][x].elementId, classification: 'path'});
            }
        }
    }
    return pathElements;
}

//create issue count on each node
function addCountOverlay(overlays, elements) {
    //Add Overlays
    for (let element of elements) {
        try {
            let overlayHtml = document.createElement("span");
            overlayHtml.setAttribute("class", "badge badge-pill badge-pill-cursor " + element.classes);
            overlayHtml.setAttribute("type", "button");
            overlayHtml.setAttribute("data-toggle", "bmodal");
            overlayHtml.setAttribute("data-target", "#issueModal");
            overlayHtml.setAttribute("title", element.title);
            overlayHtml.innerHTML = element.anz.join("<br />");
            overlayHtml.onclick = () => element.clickOverlay();

            overlays.add(element.i.elementId, {
                position: {
                    bottom: 10,
                    right: 20
                },
                html: overlayHtml
            });
        } catch (err) {
            console.log("element not found");
        }
    }
}

function getProcessVariableOverlay(bpmnFile) {
    let filteredVariables = proz_vars
        .filter(p => p.bpmnFile === (properties["basepath"] + bpmnFile))
        .filter(p => p.elementIid !== "");

    return filteredVariables.map(p => {
        let overlayData = {};
        overlayData.i = p;
        overlayData.anz = [p.read.length, p.write.length, p.delete.length];
        overlayData.clickOverlay = createVariableDialog(p);
        overlayData.classes = "badge-info badge-variable-operations";
        overlayData.title = "variable operations";
        return overlayData;
    });
}

function getIssueOverlays(bpmnFile) {
    //getElemtIds
    var eId = [];
    for (let id in elementsToMark) {
        if (elementsToMark[id].bpmnFile === (properties["basepath"] + bpmnFile))
            if (elementsToMark[id].elementId !== "")
                eId[id] = elementsToMark[id].elementId;
    }

    //delete duplicates
    var unique = function (origArr) {
        var newArr = [],
            origLen = origArr.length,
            found,
            x, y;

        for (x = 0; x < origLen; x++) {
            found = undefined;
            for (y = 0; y < newArr.length; y++) {
                if (origArr[x] === newArr[y]) {
                    found = true;
                    break;
                }
            }
            if (!found) newArr.push(origArr[x]);
        }
        return newArr;
    }
    var eIdUnique = unique(eId);

    //add count
    var i, j;
    var anz = 0;
    var objFehler = {eid: "dummy", anz: 0};
    var anzArray = [];

    for (i = 0; i < eIdUnique.length; i++) {
        var anzId = eIdUnique[i];
        for (j = 0; j < eId.length; j++) {
            if (eId[j] === anzId)
                anz++;
        }
        objFehler = {eid: eIdUnique[i], anz: anz};
        anzArray[i] = objFehler;
        anz = 0;
    }

    //Add count on each issue
    var issue = {i: "dummy", anz: 0};
    var issues = [];
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile === (properties["basepath"] + bpmnFile)) {
            var obj = elementsToMark[id];
            for (var i = 0; i < anzArray.length; i++) {
                if (anzArray[i].eid === obj.elementId) {
                    issue = {i: elementsToMark[id], anz: [anzArray[i].anz]};
                    issues[id] = issue;
                }
            }
        }
    }
    //Add dialog contents
    issues.forEach(issue => issue.clickOverlay = createIssueDialog(issues));
    issues.forEach(issue => issue.title = "issues");
    issues.forEach(issue => {
        const distinctClassifications = unique(elementsToMark
            .filter(element => element.elementId === issue.i.elementId)
            .map(element => element.classification));
        if (distinctClassifications.includes("ERROR")) {
            issue.classes = "badge-danger";
        } else if (distinctClassifications.includes("WARNING")) {
            issue.classes = "badge-warning";
        } else if (distinctClassifications.includes("INFO")) {
            issue.classes = "badge-info";
        }
    });
    return issues;
}

function getCodeReferenceOverlays(bpmnFile, elements) {
    var numReferences = [];
    for (let [key, e] of Object.entries(elements)) {
        let extRefs = Object.values(e.extensions).map(ext => Object.keys(ext).length).reduce((a, b) => a + b, 0);
        numReferences.push({
            anz: [Object.keys(e.references).length + extRefs],
            title: "references",
            classes: "badge-warning",
            i: {elementId: key},
            clickOverlay: createCodeReferenceDialog(key, e)
        });
    }

    return numReferences;
}

function createIssueDialog(elements) {
    // add DialogMessage
    return function clickOverlay() {
        //clear dialog
        const dialogContent = document.querySelector(".modal-body");
        while (dialogContent.hasChildNodes()) {
            dialogContent.removeChild(dialogContent.lastChild);
        }
        document.querySelector(".modal-title").innerHTML = "Issues";
        if (this.i.elementId !== "") {
            let eId = this.i.elementId;
            for (let y in elements) {
                if (elements[y].i.elementId === eId) {
                    var issue = elements[y].i;

                    var dCard = document.createElement("div");
                    dCard.setAttribute("class", "card bg-light mb-3");

                    var dCardBody = document.createElement("div");
                    dCardBody.setAttribute("class", "card-body");

                    var dCardTitle = document.createElement("h5");
                    dCardTitle.setAttribute("class", "card-header");

                    var dCardText = document.createElement("p");
                    dCardText.setAttribute("class", "card-text");

                    var dCardElementDescription = document.createElement("p");
                    dCardElementDescription.setAttribute("class", "card-elementDescription");

                    var dCardRuleDescription = document.createElement("p");
                    dCardRuleDescription.setAttribute("class", "card-ruleDescription");

                    var dCardImplementationDetails = document.createElement("p");
                    dCardImplementationDetails.setAttribute("class", "card-implementationDetails");

                    var dCardIssueId = document.createElement("p");
                    dCardIssueId.setAttribute("class", "card-issueId issue-id");

                    var dCardIssueButtons = document.createElement("p");

                    var dCardAddIssueButton = document.createElement("button");
                    var dCardRemoveIssueButton = document.createElement("button");

                    var plusIcon = document.createElement("span");
                    plusIcon.setAttribute("class", "text-white fas fa-plus-circle fa-lg mr-3");

                    var minusIcon = document.createElement("span");
                    minusIcon.setAttribute("class", "text-white fas fa-minus-circle fa-lg mr-3");

                    dCardAddIssueButton.setAttribute("class", "btn btn-viadee issue-button");
                    dCardAddIssueButton.addEventListener("click", addIssue.bind(null, [issue.id, issue.elementId, issue.message, dCardAddIssueButton, dCardRemoveIssueButton]));
                    dCardAddIssueButton.innerHTML = "<span>Ignore Issue</span>";
                    dCardAddIssueButton.prepend(plusIcon);

                    dCardRemoveIssueButton.setAttribute("class", "btn btn-viadee issue-button");
                    dCardRemoveIssueButton.disabled = true;
                    dCardRemoveIssueButton.addEventListener("click", removeIssue.bind(null, [issue.id, issue.message, dCardAddIssueButton, dCardRemoveIssueButton]));
                    dCardRemoveIssueButton.innerHTML = "<span>Keep Issue</span>";
                    dCardRemoveIssueButton.prepend(minusIcon);

                    dCardIssueButtons.appendChild(dCardAddIssueButton);
                    dCardIssueButtons.appendChild(dCardRemoveIssueButton);

                    let oIcon = document.createElement("span");
                    oIcon.setAttribute('class', 'float-left mr-2 ' + mapClassificationTypeToToSpanIconClass(issue.classification));

                    dCardTitle.innerHTML = issue.ruleName;
                    dCardTitle.appendChild(oIcon);
                    dCardText.innerHTML = "<h6><b>Issue:</b></h6> " + issue.message;
                    dCardRuleDescription.innerHTML = "<h6><b>Rule:</b></h6> " + issue.ruleDescription;
                    dCardElementDescription.innerHTML = "<h6><b>Reason:</b></h6> " + issue.elementDescription;
                    dCardImplementationDetails.innerHTML = "<h6><b>Implementation Details:</b></h6> " + issue.implementationDetails;
                    dCardIssueId.innerHTML = "<h6><b>Issue Id:</b></h6>" + issue.id;


                    dCard.appendChild(dCardTitle);
                    dCardBody.appendChild(dCardText);
                    if (issue.ruleDescription)
                        dCardBody.appendChild(dCardRuleDescription);
                    if (issue.elementDescription)
                        dCardBody.appendChild(dCardElementDescription);
                    if ("implementationDetails" in issue)
                        dCardBody.appendChild(dCardImplementationDetails);
                    dCardBody.appendChild(dCardIssueId);
                    dCardBody.appendChild(dCardIssueButtons);
                    dCard.appendChild(dCardBody);

                    dialogContent.appendChild(dCard);
                }
            }
        }
        const dialogFooter = document.querySelector(".modal-footer");
        while (dialogFooter.hasChildNodes()) {
            dialogFooter.removeChild(dialogFooter.lastChild);
        }
        let downloadButton = document.createElement("button");
        downloadButton.setAttribute("type", "button");
        downloadButton.setAttribute("class", "btn btn-viadee download");
        downloadButton.setAttribute("onclick", "downloadFile()");
        downloadButton.innerHTML = "Download ignoreIssues";
        dialogFooter.appendChild(downloadButton);
        let closeButton = document.createElement("button");
        closeButton.setAttribute("type", "button");
        closeButton.setAttribute("class", "btn btn-viadee");
        closeButton.setAttribute("data-dismiss", "modal");
        closeButton.innerHTML = "Close";
        dialogFooter.appendChild(closeButton);
        showDialog('show');
    }
}

function mapClassificationTypeToToSpanIconClass(classification) {
    switch (classification) {
        case "INFO":
            return "text-primary fas fa-info-circle fa-lg";
        case "SUCCESS":
            return "text-success fas fa-check-circle fa-lg";
        case "WARNING":
            return "text-warning fas fa-exclamation-circle fa-lg";
        case "ERROR":
            return "text-danger fas fa-times-circle fa-lg";
    }
}

function createVariableDialog(processVariable) {
    // add DialogMessage
    return function clickOverlay() {
        //clear dialog
        const dialogContent = document.querySelector(".modal-body");
        while (dialogContent.hasChildNodes()) {
            dialogContent.removeChild(dialogContent.lastChild);
        }
        document.querySelector(".modal-title").innerHTML = "Process Variables";
        var dCard = document.createElement("div");
        dCard.setAttribute("class", "card bg-light mb-3");

        var dCardBody = document.createElement("div");
        dCardBody.setAttribute("class", "card-body");

        var dCardTitle = document.createElement("h5");
        dCardTitle.setAttribute("class", "card-header");
        let elementName = processVariable.elementName !== undefined ? processVariable.elementName : processVariable.elementId;
        dCardTitle.innerHTML = `'${elementName}' accesses the following process variables:`;
        dCard.appendChild(dCardTitle);

        if (processVariable.read.length > 0)
            dCardBody.appendChild(createCardForVariableOperations(processVariable.read, "Reads:"));
        if (processVariable.write.length > 0)
            dCardBody.appendChild(createCardForVariableOperations(processVariable.write, "Writes:"));
        if (processVariable.delete.length > 0)
            dCardBody.appendChild(createCardForVariableOperations(processVariable.delete, "Deletes:"));


        dCard.appendChild(dCardBody);
        dialogContent.appendChild(dCard);

        const dialogFooter = document.querySelector(".modal-footer");
        while (dialogFooter.hasChildNodes()) {
            dialogFooter.removeChild(dialogFooter.lastChild);
        }
        let closeButton = document.createElement("button");
        closeButton.setAttribute("type", "button");
        closeButton.setAttribute("class", "btn btn-viadee");
        closeButton.setAttribute("data-dismiss", "modal");
        closeButton.innerHTML = "Close";
        dialogFooter.appendChild(closeButton);

        showDialog('show');
    }
}

function createCodeReferenceDialog(name, element) {
    // add DialogMessage
    return function clickOverlay() {
        //clear dialog
        const dialogContent = document.querySelector(".modal-body");
        while (dialogContent.hasChildNodes()) {
            dialogContent.removeChild(dialogContent.lastChild);
        }
        document.querySelector(".modal-title").innerHTML = "Code References";

        // Create card for direct references
        if (Object.keys(element.references).length > 0) {
            let directReferences = "<table class='card-text code-table'><tbody>";

            for (const [key, value] of Object.entries(element.references)) {
                directReferences += `<tr><td>${key}</td><td>${value}</td></tr>`;
            }
            directReferences += "</tbody></table>";

            dialogContent.appendChild(createCard("Direct Code References", directReferences));
        }

        // Create card per extension
        for (const [ext, value] of Object.entries(element.extensions)) {
            let extReferences = "<table class='card-text code-table'><tbody>";

            for (const [key, ref] of Object.entries(value)) {
                extReferences += `<tr><td>${key}</td><td>${ref}</td></tr>`;
            }
            extReferences += "</tbody></table>";

            dialogContent.appendChild(createCard("References in " + ext, extReferences));
        }

        const dialogFooter = document.querySelector(".modal-footer");
        while (dialogFooter.hasChildNodes()) {
            dialogFooter.removeChild(dialogFooter.lastChild);
        }
        let closeButton = document.createElement("button");
        closeButton.setAttribute("type", "button");
        closeButton.setAttribute("class", "btn btn-viadee");
        closeButton.setAttribute("data-dismiss", "modal");
        closeButton.innerHTML = "Close";
        dialogFooter.appendChild(closeButton);

        showDialog('show');
    }
}

function createCard(title, content) {
    var dCard = document.createElement("div");
    dCard.setAttribute("class", "card bg-light mb-3");

    var dCardBody = document.createElement("div");
    dCardBody.setAttribute("class", "card-body");
    dCardBody.innerHTML = content;

    var dCardTitle = document.createElement("h5");
    dCardTitle.setAttribute("class", "card-header");
    dCardTitle.innerHTML = title;
    dCard.appendChild(dCardTitle);

    dCard.appendChild(dCardBody);
    return dCard;
}

function createCardForVariableOperations(operations, title) {
    var dCardText = document.createElement("p");
    dCardText.setAttribute("class", "card-text");

    let operationsText = operations.map(o => createShowOperationsLink(o.name).outerHTML + ` ('${o.elementChapter}', '${o.fieldType}')`).join("<br />");
    dCardText.innerHTML = `<h6><b>${title}</b></h6> ` + operationsText;

    return dCardText;
}

// Add single issue to the ignoreIssues list
function addIssue(issue) {
    ignoredIssues[issue[0]] = `#Element-ID: ${issue[1]}; ${issue[2]}`;
    issue[3].disabled = true;
    issue[4].disabled = false;
}

// Remove single issue from ignoreIssues list
function removeIssue(issue) {
    delete ignoredIssues[issue[0]];
    issue[2].disabled = false;
    issue[3].disabled = true;
}

// download the ignoreIssues file
function downloadFile() {
    var value;
    var blob = "";
    Object.keys(ignoredIssues).forEach(function (key) {
        value = ignoredIssues[key];
        blob = blob + value + "\n" + key + "\n";
    });
    download(new Blob([blob]), "ignoreIssues.txt", "text/plain");
}

// download the BPMN xml file
function downloadModel() {
    download(controller.currentModel.xml, controller.currentModel.name, "application/xml");
}

//delete table under diagram
function deleteTable() {
    let myTable = document.getElementById("table");
    while (myTable.firstChild) {
        myTable.removeChild(myTable.firstChild);
    }
}

function createTableHeader(id, content) {
    let myTh = document.createElement("th");
    myTh.setAttribute("id", id);
    myTh.innerHTML = content;
    return myTh;
}

//create issue table
function createIssueTable(bpmnFile, tableContent, mode) {
    var myTable = document.getElementById("table");
    myTable.setAttribute("class", "table table-issues table-row table-bordered .table-responsive");
    let myTHead = document.createElement("thead");
    let myRow = document.createElement("tr");
    myRow.setAttribute("id", "tr_ueberschriften");
    myRow.setAttribute("class", "table-primary");
    myRow.appendChild(createTableHeader("th_ruleName", "Rule-Name"));
    myRow.appendChild(createTableHeader("th_elementId", "Element-Id"));
    myRow.appendChild(createTableHeader("th_elementName", "Element-Name"));
    myRow.appendChild(createTableHeader("th_classification", "Class"));
    myRow.appendChild(createTableHeader("th_message", "Message"));
    myRow.appendChild(createTableHeader("th_implementationDetails", "Implementation Details"));
    myRow.appendChild(createTableHeader("th_paths", "Invalid Sequenceflow"));
    myTHead.appendChild(myRow);
    myTable.appendChild(myTHead);

    //fill table with all issuesof current model
    for (let issue of tableContent) {
        if (issue.bpmnFile === (properties["basepath"] + bpmnFile)) {
            let myParent = document.getElementsByTagName("body").item(0);
            let myTBody = document.createElement("tbody");
            myRow = document.createElement("tr");

            //ruleName
            var myCell = document.createElement("td");
            var innerDiv = document.createElement("div");
            var myText = document.createTextNode(issue.ruleName);
            innerDiv.appendChild(myText);

            //buttons to add and remove issue
            var addIssueButton = document.createElement("button");
            addIssueButton.setAttribute("class", "btn btn-viadee issue-button-table-add");

            var removeIssueButton = document.createElement("button");
            removeIssueButton.setAttribute("class", "btn btn-viadee issue-button-table-remove");

            addIssueButton.addEventListener("click", addIssue.bind(null, [issue.id, issue.elementId, issue.message, addIssueButton, removeIssueButton]));
            addIssueButton.innerHTML = "Ignore Issue";

            removeIssueButton.setAttribute("disabled", true);
            removeIssueButton.addEventListener("click", removeIssue.bind(null, [issue.id, issue.message, addIssueButton, removeIssueButton]));
            removeIssueButton.innerHTML = "Keep Issue";

            myCell.setAttribute("id", issue.classification); // mark cell

            //create link for default checkers
            var a = document.createElement("a");
            a.appendChild(innerDiv);

            defaultCheckers.forEach(element => {
                if (issue.ruleName === element.rulename) {
                    a.setAttribute("href", "https://viadee.github.io/vPAV/Checker/" + issue.ruleName + ".html");
                    a.setAttribute("title", "Checker documentation");
                    a.setAttribute("target", "_blank");
                    a.style.fontWeight = 'bold';
                }
            });

            myCell.appendChild(a);

            //based on selection show button
            if (mode === tableViewModes.ISSUES) {
                myCell.appendChild(addIssueButton);
                myCell.appendChild(removeIssueButton);
            }

            //link to docu
            myRow.appendChild(myCell);

            //elementId
            myCell = document.createElement("td");
            if (issue.elementId !== undefined) {
                myCell.appendChild(createMarkElementLink(issue.elementId));
            }
            myRow.appendChild(myCell);

            //elementName
            myCell = document.createElement("td");
            if (issue.elementId !== undefined) {
                myText = document.createTextNode(issue.elementName);
                myCell.appendChild(myText);
            }
            myRow.appendChild(myCell);

            //classification
            myCell = document.createElement("td");
            myCell.setAttribute("align", "center");
            let oIcon = document.createElement("span");
            oIcon.setAttribute('class', mapClassificationTypeToToSpanIconClass(issue.classification));
            myCell.appendChild(oIcon);
            myRow.appendChild(myCell);

            //message
            myCell = document.createElement("td");
            //add links for process variables contained in message
            let messageText = issue.message;
            processVariables.filter(p => issue.message.includes(`'${p.name}'`))
                .forEach(p => messageText = messageText.replace(p.name, createShowOperationsLink(p.name).outerHTML));
            myCell.innerHTML = messageText;
            myRow.appendChild(myCell);

            // implementation details
            myCell = document.createElement("td");
            myCell.setAttribute("style", "word-break: break-all");
            myCell.innerHTML = "";
            if ("implementationDetails" in issue) {
                myCell.innerHTML = issue.implementationDetails;
            }
            myRow.appendChild(myCell);

            //path
            myCell = document.createElement("td");
            var path_text = "";
            if (issue.elementId != null) {
                for (x in issue.paths) {
                    for (y in issue.paths[x]) {
                        if (issue.paths[x][y].elementName == null)
                            if (y < issue.paths[x].length - 1)
                                path_text += issue.paths[x][y].elementId + " -> ";
                            else
                                path_text += issue.paths[x][y].elementId;
                        else if (y < issue.paths[x].length - 1)
                            path_text += issue.paths[x][y].elementName + " -> ";
                        else
                            path_text += issue.paths[x][y].elementName
                    }
                    myText = document.createTextNode("Mark invalid flow");

                    //mark path
                    var p = issue.paths[x];

                    var b = document.createElement("a");
                    b.appendChild(myText);
                    b.setAttribute("onclick", "controller.showPath('" + issue.id + "','" + x + "', '" + path_text + "')");
                    b.setAttribute("href", "#");

                    myCell.appendChild(b);
                    path_text = "";

                    //add break
                    let br = document.createElement("br");
                    myCell.appendChild(br);
                    //only add break if its not the last one
                    if (x < issue.paths.length - 1) {
                        let brz = document.createElement("br");
                        myCell.appendChild(brz);
                    }
                }
            }
            myRow.appendChild(myCell);
            //---------
            myTBody.appendChild(myRow);
            myTable.appendChild(myTBody);
        }
    }
}

//create process variable table
function createVariableTable(bpmnFile, tableContent) {
    let myTable = document.getElementById("table");
    myTable.setAttribute("class", "table table-variables table-row table-bordered .table-responsive")
    let myTHead = document.createElement("thead");
    let myRow = document.createElement("tr");
    myRow.setAttribute("id", "tr_ueberschriften");
    myRow.setAttribute("class", "table-primary");
    myRow.appendChild(createTableHeader("th_ruleName", "Process Variable"));
    myRow.appendChild(createTableHeader("th_reads", "Read by Elements"));
    myRow.appendChild(createTableHeader("th_writes", "Written by Elements"));
    myRow.appendChild(createTableHeader("th_deletes", "Deleted by Elements"));
    myTHead.appendChild(myRow);
    myTable.appendChild(myTHead);

    //fill table with all variables of current model
    for (let processVariable of tableContent) {
        if (processVariable.bpmnFile !== (properties["basepath"] + bpmnFile))
            continue;

        let myTBody = document.createElement("tbody");
        let myRow = document.createElement("tr");

        let myCell = document.createElement("td");
        myCell.appendChild(createShowOperationsLink(processVariable.name));
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        let elementLinks = processVariable.read.map(p => createMarkElementLink(p.elementId));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        elementLinks = processVariable.write.map(p => createMarkElementLink(p.elementId));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        elementLinks = processVariable.delete.map(p => createMarkElementLink(p.elementId));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);
        //---------
        myTBody.appendChild(myRow);
        myTable.appendChild(myTBody);
    }
    document.getElementById("content").appendChild(myTable);
}

function createMarkElementLink(elementId) {
    let myText = document.createTextNode(elementId);
    //create link
    let c = document.createElement("a");
    c.appendChild(myText);
    if (elementId !== "") {
        c.setAttribute("onclick", "controller.markElement('" + elementId + "')");
        c.setAttribute("href", "#");
        c.setAttribute("title", "mark element");
    }
    return c;
}

function createShowOperationsLink(processVariableName) {
    //create link
    let c = document.createElement("a");
    let myText = document.createTextNode(processVariableName);
    c.appendChild(myText);
    c.setAttribute("onclick", "controller.showVariableOperations('" + processVariableName + "')");
    c.setAttribute("href", "#");
    c.setAttribute("title", "mark operations");
    c.setAttribute("data-dismiss", "modal");
    return c;
}

/**
 * create Footer
 */
function createFooter() {
    const oldFooter = document.getElementById("footer");
    if (oldFooter) {
        oldFooter.remove();
    }
    const footer = `
    <footer id="footer" class="footer sticky-bottom viadee-footer mt-auto py-3">
    <div class="container-fluid">
        <span class="text-muted-viadee">${viadee + " - " + vPavName + " " + vPavVersion}</span>
        <a class="text-muted-viadee float-right pr-2" href="https://viadee.github.io/vPAV/#licenses" target="_blank">Licenses</a>
        <a class="text-muted-viadee float-right pr-2" href="https://www.viadee.de/impressum-datenschutz.html"
           target="_blank">Imprint</a>
    </div>
</footer>
`;
    document.getElementById("content").insertAdjacentHTML("afterEnd", footer);
}

function createHeader(modelFileName) {
    const oldHeader = document.getElementById("header");
    if (oldHeader) {
        oldHeader.parentNode.removeChild(oldHeader);
    }
    const modelName = modelFileName ? modelFileName.substr(0, modelFileName.length - 5) : "";
    const projectName = properties ? properties["projectName"] : "";
    const header = `
<nav id="header" role="heading" class="navbar navbar-fixed-top viadee-lightblue-bg">
            <ul class="nav align-items-center">
                <li class="nav-item">
                    <button id="navbar-toggle" class="btn btn-outline-light d-none" type="button"
                            onclick="toggleSideBar()"
                            aria-controls="nav-content" aria-expanded="false" aria-label="Toggle navigation">
                        <span class="fas fa-chevron-left"></span>
                    </button>
                </li>
                <li class="nav-item">
                    <a href="https://viadee.github.io/vPAV/" target="_blank">
                        <img class="d-block" src="img/vPAV.png" alt="vPAV" height="60" title="documentation"/>
                    </a>
                </li>
                <li class="nav-item">
                    <h4 class="nav-item navbar-text text-white mb-0 ml-4 mr-2" id="model">${modelName}</h4>
                </li>
                ${properties ? `<li class="nav-item">
                    <a id="model_download" class="nav-link pl-2 pr-1 mx-1 py-3 my-n2" onclick="downloadModel()">
                        <span class="text-white fas fa-arrow-alt-circle-down fa-2x"></span>
                    </a>
                </li>` : ""}
            </ul>
            <ul class="nav navbar-center align-items-center">
                <li class="nav-item">
                    <h3 id="project_name" class="navbar-text text-white">${projectName}</h3>
                </li>
            </ul>
            <ul class="nav navbar-right align-items-center">
                <li class="nav-item">
                    <a class="nav-link pl-2 pr-1 mx-1 py-3 my-n2" id="github-logo"
                       href="https://github.com/viadee/vPAV">
                        <span class="text-white fab fa-github fa-2x"></span>
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link pl-2 pr-1 mx-1 py-0 my-n2" href="https://www.viadee.de/">
                        <img src="img/viadee_weiss.png" alt="viadee Homepage" height="70" title="viadee Homepage"/>
                    </a>
                </li>
            </ul>
        </nav>
    `;
    document.getElementById("content").insertAdjacentHTML("beforeBegin", header);
    if (typeof reportData !== "undefined" && reportData.isMultiProjectScan === "true") {
        createProjectsNavbar();
    }
}

function createProjectsNavbar() {
    document.getElementById("navbar-toggle").classList.toggle("d-none");
    if (!document.getElementById("sidebar-wrapper")) {
        const sidebarHtml = `
            <div id="sidebar-wrapper" class="border-right">
            <div id="sidebar" class="list-group list-group-flush">
                <a href="#" class="list-group-item list-group-item-action"
                onclick="createProjectSummary()">
                <div class="row">
                        <i class="col-auto text-white font-weight-bold mr-2 fas fa-list-ul"></i>
                      <p class="col text-white font-weight-bold">Project summary</p>     
                  </div> 
                </a>
                ${projectNamesSorted.map((name) => {
            return `
                    <a href="#" class="list-group-item list-group-item-action"
                        onclick="loadExternalReport('${projectNameToPathMap.get(name)}')">
                        <div class="row">
                        <i class="col-auto text-white mr-2 fas fa-file"></i>
                      <p class="col text-white">${name}</p>   
                      </div>   
                    </a>
                    `
        }).join("")}
            </div>
        </div>
    `;
        document.getElementById("wrapper").insertAdjacentHTML("afterbegin", sidebarHtml);
        //Set active the selected project item sidebar, otherwise set active the first sidebar item, which is the project overview
        const projectName = properties ? properties.projectName : "";
        const loadedProjectEntry = Array.from(document.querySelectorAll("#sidebar a > div > p"))
            .find(paragraph => paragraph.textContent === projectName);
        loadedProjectEntry ? loadedProjectEntry.parentNode.parentNode.classList.toggle("selected") :
            document.querySelector("#sidebar > a").classList.toggle("selected");
    }
}

async function createProjectSummary() {
    resetDocument();
    await loadDomElements(createScriptTags(['data/infoPOM.js'], true)); // infoPOM is needed to fill footer
    createHeader();
    createFooter();
    setTooltips();
    const mainContent = document.getElementById("content");
    const summaryTemplate = `
              ${projectNamesSorted.map((name) => {
        return `
        <div class="col mt-3 mb-3">
            <h3 class="row">
                <span class="badge badge-secondary">
                    ${projectNameToSummaryMap.get(name).projectName}
                </span>
            </h3>
            <div class="row small-box-container">           
                ${smallBoxTemplate("Issue ratio", "fas fa-percentage fa-xs",
            Math.round(projectNameToSummaryMap.get(name).issuesRatio), toolTips.issuesRatio)}
                ${smallBoxTemplate("Flawed elements ratio", "fas fa-info-circle fa-xs",
            Math.round(projectNameToSummaryMap.get(name).flawedElementsRatio), toolTips.flawedElementsRatio)}
                ${smallBoxTemplate("Warning elements ratio", "fas fa-exclamation-triangle fa-xs",
            Math.round(projectNameToSummaryMap.get(name).warningElementsRatio), toolTips.warningElementsRatio)}
                ${smallBoxTemplate("Error elements ratio", "fas fa-times-circle fa-xs",
            Math.round(projectNameToSummaryMap.get(name).errorElementsRatio), toolTips.errorElementsRatio)} 
            </div>
                <button type="button" class="row btn ml-1" data-toggle="modal" 
                data-target="#modalTable"
                onclick="createModalTable('${name}')"}>
                <div class="d-flex flex-row justify-content-center align-items-center">
                    <h4 class="text-white text-nowrap">More info</h4>
                    <i class="text-white fas fa-arrow-circle-right h4 ml-1"></i>
                </div>
                </button>
                </div>   
             `
    }).join("")}
              
        <div id="modalTable" class="modal fade" tabindex="-1" role="dialog">
  <div class="modal-dialog mw-100" role="document">
    <div class="modal-content w-100 h-100">
      <div class="modal-header viadee-lightblue-bg viadee-head">
        <h4 class="modal-title text-white">Project statistics</h4>
        <button type="button" class="close text-white btn-viadee-close" data-dismiss="modal" aria-label="Close">
          <span class="fas fa-times"></span>
        </button>
      </div>
      <div class="modal-body table-responsive">
        <table id="table">
        </table>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-viadee" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>         
`;
    mainContent.innerHTML = summaryTemplate;
    $(".knob").knob(); //Init display of knobs
    $('[data-toggle="tooltip"]').tooltip();//Init tooltips
}

function smallBoxTemplate(label, icon, knobValue, toolTip) {
    return `
            <div class="col text-center small-box mb-0 border-secondary">
                    <input class="knob" data-readonly="true" value="${knobValue}" data-width="150" data-height="150"
                        data-fgcolor="#DFAD47"  data-bgcolor="#7EBCA9" data-inputcolor="#6c757d" readonly="readonly">
                        <div class="d-flex flex-row justify-content-between align-items-center rounded mt-1 p-1">
                            <h5 class="flex-column knob-label text-white mb-0">${label}</h5>
                            <span class="flex-column fa-stack fa-1x mb-0"
                            data-toggle="tooltip" data-placement="bottom" title="${toolTip}">
                                <i class="h5 text-white fas fa-circle fa-stack-1x"></i>
                                <i class="h5 fas text-info fas fa-question-circle fa-stack-1x fa-inverse"></i>
                            </span>         
                        </div>
                <div class="icon">
                    <i class="${icon}"></i>
                </div>
            </div>
    `;
}

function createModalTable(projectName) {

    const projectNamesRowIndex = new Map();
    const projectMainRowFormat = (row, index) => {
        const displayIndex = index + 1;
        if (!row.modelName) {
            // if (!projectNamesRowIndex.get(row.projectName)) {
            projectNamesRowIndex.set(row.projectName, displayIndex);
            // }
            return {classes: `viadee-lightblue-bg font-weight-bold treegrid-${displayIndex}`}
        } else {
            return {classes: `treegrid-${displayIndex} treegrid-parent-${projectNamesRowIndex.get(row.projectName)}`}
        }
    }
    const percentageFormat = value => `${Math.round(value)}%`;
    const modelNameFormat = modelPath => {
        const pathWithoutSuffix = modelPath.substr(0, modelPath.length - 5);
        const modelName = pathWithoutSuffix.substring(pathWithoutSuffix.lastIndexOf("/") + 1, pathWithoutSuffix.length);
        return modelName;
    }
    const headerFormat = (column) => {
        return {classes: "align-top"};
    };
    const columnDefinitions = [
        {field: 'projectName', title: 'Project name', sortable: true},
        {field: 'modelName', title: 'Model name', sortable: true, formatter: modelNameFormat},
        {field: 'totalElements', title: 'Total elements', sortable: true},
        {field: 'ignoredElements', title: 'Ignored elements', sortable: true},
        {field: 'analyzedElements', title: 'Analyzed elements', sortable: true},
        {field: 'issues', title: 'Issues', sortable: true},
        {field: 'ignoredIssues', title: 'Ignored issues', sortable: true},
        {field: 'warnings', title: 'Warnings', sortable: true},
        {field: 'errors', title: 'Errors', sortable: true},
        {field: 'warningElements', title: 'Warning elements', sortable: true},
        {field: 'errorElements', title: 'Error elements', sortable: true},
        {field: 'issuesRatio', title: 'Issues ratio', sortable: true, formatter: percentageFormat},
        {field: 'warningRatio', title: 'Warning ratio', sortable: true, formatter: percentageFormat},
        {field: 'errorRatio', title: 'Error ratio', sortable: true, formatter: percentageFormat},
        {field: 'warningElementsRatio', title: 'Warning elements ratio', sortable: true, formatter: percentageFormat},
        {field: 'errorElementsRatio', title: 'Error elements ratio', sortable: true, formatter: percentageFormat},
        {field: 'flawedElementsRatio', title: 'Flawed elements ratio', sortable: true, formatter: percentageFormat}];
    const tableData = Array.from(projectNameToSummaryMap.values())
        .concat(Array.from(projectNameToSummaryMap.values()).map(summary => summary.models).flat(1))
        .sort((a, b) => (a.projectName > b.projectName) ? 1 : ((b.projectName > a.projectName) ? -1 : 0));
    const $table = $('#table');
    $table.on("post-header.bs.table", () => {
        //direct styling of inner heading divs otherwise not possible
        document.querySelectorAll(".th-inner").forEach(heading => heading.classList.add("text-wrap"));
    });
    $table.on("load-success.bs.table post-body.bs.table refresh.bs.table reset-view.bs.table", () => {
        //copied from treegrid examples. For unknown reasons it is needed but it is unclear why. Too bad.
        const columns = $table.bootstrapTable('getOptions').columns;
        if (columns && columns[0][1].visible) {
            $table.treegrid({treeColumn: 0});
        }
        //workaround, as setting an empty indentTemplate during treegrid init won't work for some reason
        document.querySelectorAll("span.treegrid-indent")
            .forEach(element => element.remove());

        //Remove spacing in treegrid subitems
        document.querySelectorAll("span.treegrid-expander:not(.treegrid-expander-expanded):not(.treegrid-expander-collapsed)")
            .forEach(element => element.remove());

        //Setting tooltips
        document.querySelectorAll("thead > tr > th").forEach(header => {
            header.setAttribute("data-toggle", "tooltip");
            header.setAttribute("data-placement", "bottom");
            header.setAttribute("data-original-title", toolTips[header.getAttribute("data-field")]);
        });
    });
    $table.bootstrapTable({
        columns: columnDefinitions, data: tableData, showColumns: true, showFullscreen: true,
        classes: "table table-bordered", buttonsClass: "viadee",
        search: true, searchText: projectName, showToggle: true, showSearchClearButton: true, showExport: true,
        exportTypes: ['json', 'xml', 'csv', 'txt', 'sql'],
        headerStyle: headerFormat, rowStyle: projectMainRowFormat
    });

    $table.treegrid();//Init tree view
    $('[data-toggle="tooltip"]').tooltip();//Init tooltips
    $('#modalTable').on("shown.bs.modal", () => {
        //Trigger first time dom cleanup
        $table.bootstrapTable('resetView');
    });
    $('#modalTable').on("hidden.bs.modal", () => {
        $table.bootstrapTable('destroy');
    });
}

//get issue count from specific bpmnFile
function countIssues(bpmnFile, tableContent) {
    let count = 0;
    for (let id in tableContent) {
        if (tableContent[id].bpmnFile === (properties["basepath"] + bpmnFile)) {
            count++;
        }
    }
    return count;
}

function showDialog() {
    $('#issueModal').modal();
}

// List all ProcessInstances
function createDiagramTabs() {
    var first = true;
    for (var id = 0; id <= diagramXMLSource.length - 1; id++) {
        let model = diagramXMLSource[id];
        var ul = document.getElementById("linkList");
        var li = document.createElement("li");
        var a = document.createElement("a");
        var subName = model.name.substr(0, model.name.length - 5);
        li.appendChild(a);
        li.setAttribute("class", "nav-item right-margin");
        if (countIssues(model.name, elementsToMark) === 0)
            a.innerHTML = subName + " <span class='badge badge-pill badge-success badge-result pt-1 pb-1'>" + countIssues(model.name, elementsToMark) + "</span>";
        else
            a.innerHTML = subName + " <span class='badge badge-pill pt-1 pb-1 badge-result text-white'>" + countIssues(model.name, elementsToMark) + "</span>";
        a.setAttribute("onclick", "controller.switchModel('" + model.name.replace(/\\/g, "\\\\") + "')");
        a.setAttribute("href", "#");
        if (first === true) {
            a.setAttribute("class", "nav-link model-selector active");
            first = false;
        } else {
            a.setAttribute("class", "nav-link model-selector");
        }

        a.setAttribute("id", model.name);
        ul.appendChild(li);
    }
}

// List all view modes
function createViewModesNavBar(model) {
    if (countIssues(model, elementsToMark) <= 0)
        document.getElementById("showAllIssues").parentNode.classList.add("hide");
    else
        document.getElementById("showAllIssues").parentNode.classList.remove("hide");

    if (countIssues(model, noIssuesElements) <= 0)
        document.getElementById("showSuccess").parentNode.classList.add("hide");
    else
        document.getElementById("showSuccess").parentNode.classList.remove("hide");

    if (proz_vars !== undefined && proz_vars.length <= 0)
        document.getElementById("showVariables").parentNode.classList.add("hide");
    else
        document.getElementById("showVariables").parentNode.classList.remove("hide");
}

function setFocus(name) {
    document.getElementById(name).focus();
}

const tableViewModes = Object.freeze({
    ISSUES: Symbol("issues"),
    NO_ISSUES: Symbol("no issues"),
    VARIABLES: Symbol("process variables")
});

const overlayViewModes = Object.freeze({
    ISSUES: Symbol("issues"),
    VARIABLES: Symbol("process variables"),
    CODE: Symbol("code references")
});

function createViewController() {
    let ctrl = {};
    var doc = document.getElementById("viewModeNavBar");
    let bpmnViewer;
    let code_elements = {};

    /**
     * bpmn-js-seed
     *
     * This is an example script that loads an embedded diagram file <diagramXML>
     * and opens it using the bpmn-js viewer.
     */
    function updateDiagram(diagramXML, elements, overlayData) {
        document.getElementById("rowPath").setAttribute("class", "collapse");
        // remove current diagram
        document.querySelector("#canvas").innerHTML = "";

        // create viewer
        bpmnViewer = new window.BpmnJS({
            container: '#canvas'
        });

        // import diagram
        bpmnViewer.importXML(diagramXML.xml, function (err) {
            if (err) {
                return console.error('could not import BPMN 2.0 diagram', err);
            }

            let canvas = bpmnViewer.get('canvas'),
                overlays = bpmnViewer.get('overlays');

            // zoom to fit full viewport
            canvas.zoom('fit-viewport');
            createHeader(diagramXML.name);
            addCountOverlay(overlays, overlayData);
            markNodes(elements, canvas);
            controller.loadCodeElements();
        });
    }

    function updateTable(tableViewMode, diagramName) {
        deleteTable();
        document.getElementById("viewModeNavBar").querySelectorAll("a").forEach(a => a.setAttribute("class", "nav-link table-selector"));

        if (tableViewMode === tableViewModes.VARIABLES) {
            document.getElementById("showVariables").setAttribute("class", "nav-link table-selector active");
            createVariableTable(diagramName, processVariables);
        } else if (countIssues(diagramName, elementsToMark) > 0) {
            if (tableViewMode === tableViewModes.ISSUES) {
                document.getElementById("showAllIssues").setAttribute("class", "nav-link table-selector active");
                createIssueTable(diagramName, elementsToMark, tableViewMode);
            } else {
                document.getElementById("showSuccess").setAttribute("class", "nav-link table-selector active");
                createIssueTable(diagramName, noIssuesElements, tableViewMode);
            }
        } else {
            createIssueTable(diagramName, noIssuesElements, tableViewMode);
        }
        createFooter();
    }

    function updateView(overlayViewMode, tableViewMode, model) {
        controller.currentTableViewMode = tableViewMode;
        controller.currentOverlayViewMode = overlayViewMode;
        controller.currentModel = model;

        let elements, overlayData;
        if (overlayViewMode === overlayViewModes.ISSUES) {
            elements = filterElementsByModel(elementsToMark, model.name);
            overlayData = getIssueOverlays(model.name);
        } else if (overlayViewMode === overlayViewModes.VARIABLES) {
            elements = [];
            overlayData = getProcessVariableOverlay(model.name);
        } else if (overlayViewMode === overlayViewModes.CODE) {
            elements = [];
            overlayData = getCodeReferenceOverlays(model.name, code_elements);
        }

        updateDiagram(model, elements, overlayData);
        updateTable(tableViewMode, model.name);

        let btReset = document.getElementById("reset");
        btReset.setAttribute("class", "btn btn-viadee mt-2 collapse");
        btReset.setAttribute("onclick", "controller.resetOverlay()");
    }

    function getModel(modelName) {
        for (let model of diagramXMLSource) {
            let a = document.getElementById(model.name);
            a.setAttribute("class", "nav-link model-selector");
            if (model.name === modelName) {
                a.setAttribute("class", "nav-link active model-selector");
                return model;
            }
        }
    }

    ctrl.init = function () {
        updateView(overlayViewModes.ISSUES, tableViewModes.ISSUES, diagramXMLSource[0]);
    };

    ctrl.showIssues = function () {
        updateView(overlayViewModes.ISSUES, tableViewModes.ISSUES, this.currentModel);
    };

    ctrl.showSuccessfulCheckers = function () {
        updateView(overlayViewModes.ISSUES, tableViewModes.NO_ISSUES, this.currentModel);
    };

    ctrl.showProcessVariables = function () {
        updateView(overlayViewModes.VARIABLES, tableViewModes.VARIABLES, this.currentModel);
    };

    ctrl.showPath = function (elementId, path_nr, path) {
        updateDiagram(this.currentModel, getElementsOnPath(elementId, path_nr), []);

        document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse.show");
        document.getElementById('invalidPath').innerHTML = path;
        document.getElementById("rowPath").setAttribute("class", "collapse.show");
        document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse");
    };

    ctrl.markElement = function (elementId) {
        updateDiagram(this.currentModel, [{elementId: elementId, classification: 'one-element'}], []);
        document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse.show");
    };

    ctrl.showVariableOperations = function (variableName) {
        let processVariable = processVariables.find(p => p.name === variableName);
        let operations = processVariable.read.concat(processVariable.write, processVariable.delete);
        let elements = operations.map(o => {
            o.classification = "one-element";
            return o;
        });

        updateDiagram(this.currentModel, elements, getProcessVariableOverlay(this.currentModel.name));
        document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse.show");
    };

    ctrl.resetOverlay = function () {
        updateView(this.currentOverlayViewMode, this.currentTableViewMode, this.currentModel);
    };

    ctrl.switchModel = function (modelName) {
        let model = getModel(modelName);
        if (model === null) throw "model not found";

        document.querySelectorAll("#linkList li a").forEach(a => a.setAttribute("class", "nav-link model-selector"));
        document.getElementById(model.name).setAttribute("class", "nav-link model-selector active");

        updateView(overlayViewModes.ISSUES, tableViewModes.ISSUES, model);
        createViewModesNavBar(model.name);
    };

    // Create list of code elements
    ctrl.loadCodeElements = function () {
        // Use all elements
        Object.values(bpmnViewer.get('elementRegistry')._elements).forEach(element => {
            // Loop through all possible code references
            sourceCodeAttributes.forEach(ref => {
                if (Object.keys(element.element.businessObject.$attrs).includes(ref)) {
                    if (!(element.element.id in code_elements)) {
                        code_elements[element.element.id] = {"references": {}, "extensions": {}};
                    }
                    code_elements[element.element.id].references[ref] = element.element.businessObject.$attrs[ref];
                }
            });

            // Loop through extension elements like listeners
            if (element.element.businessObject.hasOwnProperty("extensionElements")) {
                element.element.businessObject.extensionElements.values.forEach(extension => {
                    // Loop through all possible code references
                    sourceCodeAttributes.forEach(ref => {
                        if (Object.keys(extension).includes(ref)) {
                            if (!(element.element.id in code_elements)) {
                                code_elements[element.element.id] = {"references": {}, "extensions": {}};
                            }
                            if (!(extension.$type in code_elements[element.element.id].extensions)) {
                                code_elements[element.element.id].extensions[extension.$type] = {};
                            }
                            code_elements[element.element.id].extensions[extension.$type][ref] = extension[ref];
                        }
                    });
                });
            }
        });
    };

    // (Un-)highlight code references
    ctrl.showElementsWithCodeReferences = function (btn) {
        updateView(overlayViewModes.CODE, tableViewModes.ISSUES, this.currentModel);
    };

    return ctrl;
}

function showUnlocatedCheckers() {
    unlocatedCheckers.forEach(element => {

        var warningMsg =
            `<div class='row' id='unlocatedCheckers'>
            <div class="col">
                <div class="alert alert-danger mt-2 mb-0 ml-0 pb-1 pt-1 viadee-big-alert"
                role="alert">${element.message}</div>
            </div>
        </div>`;

        document.getElementById("unlocatedCheckersContainer").innerHTML += warningMsg;
    });
}

function toggleSideBar() {
    const iconToggleButton = document.querySelector("#navbar-toggle > span");
    iconToggleButton.classList.toggle("fa-chevron-left");
    iconToggleButton.classList.toggle("fa-chevron-right");
    document.getElementById("sidebar-wrapper").classList.toggle("collapse");
    //removes empty space space on the left between canvas frame and sidebar
    Array.from(document.getElementsByClassName("col")).forEach(element => element.classList.toggle("pl-0"));
}

// Define attributes which reference source code
const sourceCodeAttributes = ["camunda:class", "class",
    "camunda:delegateExpression", "delegateExpression",
    "camunda:variableMappingClass", "camunda:variableMappingDelegateExpression"];

function resetDocument() {
    document.documentElement.innerHTML = documentBackup.documentElement.innerHTML;
    unloadDataScriptTags();
}

async function loadExternalReport(reportPath) {
    resetDocument();
    await loadDomElements(createScriptTags(generateScriptSourcesArray(reportPath), true));
    initPage();
}

const toolTips = {};

function setTooltips() {
    toolTips.projectName = "Folder name of the Java project";
    toolTips.modelName = "Name of the BPMN file project resource"
    toolTips.totalElements = "Total amount fo BPMN elements";
    toolTips.ignoredElements = "BPMN elements having ignored issues";
    toolTips.analyzedElements = "BPMN elements without any ignored issues";
    toolTips.issues = "Total issues amount";
    toolTips.ignoredIssues = "Ignored issues count";
    toolTips.flawedElements = "BPMN elements having issues";
    toolTips.warnings = "Amount of warnings within issues";
    toolTips.errors = "Amount of errors within issues";
    toolTips.warningElements = "BPMN element count with warnings";
    toolTips.errorElements = "BPMN element count with errors";
    toolTips.issuesRatio = "Issues / (analyzed elements) * 100";
    toolTips.warningRatio = "Warnings / (analyzed elements) * 100";
    toolTips.errorRatio = "Errors / analyzed elements * 100";
    toolTips.warningElementsRatio = "Warning elements / (analyzed elements) * 100";
    toolTips.errorElementsRatio = "Error elements / (analyzed elements) * 100";
    toolTips.flawedElementsRatio = "Flawed elements / (analyzed elements) * 100";
}

var controller;
var bpmnFile;

function initPage() {
    if (typeof reportData !== "undefined" &&
        reportData.isMultiProjectScan === "true" &&
        projectSummaryFirstTimeLoaded === false) {
        projectSummaryFirstTimeLoaded = true;
        createProjectSummary();
    } else {
        createDiagramTabs();
        bpmnFile = diagramXMLSource[0].name;
        createViewModesNavBar(bpmnFile);
        controller = createViewController();
        controller.init();
        showUnlocatedCheckers();
    }
}

initPage();

