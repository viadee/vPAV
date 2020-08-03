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
        issueSeverity.forEach(element => {
            if (element.id === issue.i.elementId) {
                if (element.Criticality === "ERROR") {
                    issue.classes = "badge-danger";
                }
                if (element.Criticality === "WARNING") {
                    issue.classes = "badge-warning";
                }
                if (element.Criticality === "INFO") {
                    issue.classes = "badge-info";
                }
            }
        });
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

                    var plusIcon = document.createElement("img");
                    plusIcon.setAttribute("src", "img/plus_icon.png");
                    plusIcon.setAttribute("class", "button-icon mr-3");

                    var minusIcon = document.createElement("img");
                    minusIcon.setAttribute("src", "img/minus_icon.png");
                    minusIcon.setAttribute("class", "button-icon mr-3");

                    dCardAddIssueButton.setAttribute("class", "btn btn-viadee issue-button");
                    dCardAddIssueButton.addEventListener("click", addIssue.bind(null, [issue.id, issue.message, dCardAddIssueButton, dCardRemoveIssueButton]));
                    dCardAddIssueButton.innerHTML = "<span>Ignore Issue</span>";
                    dCardAddIssueButton.prepend(plusIcon);

                    dCardRemoveIssueButton.setAttribute("class", "btn btn-viadee issue-button");
                    dCardRemoveIssueButton.disabled = true;
                    dCardRemoveIssueButton.addEventListener("click", removeIssue.bind(null, [issue.id, issue.message, dCardAddIssueButton, dCardRemoveIssueButton]));
                    dCardRemoveIssueButton.innerHTML = "<span>Keep Issue</span>";
                    dCardRemoveIssueButton.prepend(minusIcon);

                    dCardIssueButtons.appendChild(dCardAddIssueButton);
                    dCardIssueButtons.appendChild(dCardRemoveIssueButton);

                    var oImg = document.createElement("img");
                    oImg.setAttribute('src', 'img/' + issue.classification.toLowerCase() + '.png');
                    oImg.setAttribute('alt', 'issue.classification');
                    oImg.setAttribute('class', 'float-left mr-2');
                    oImg.setAttribute("title", issue.classification);

                    dCardTitle.innerHTML = issue.ruleName;
                    dCardTitle.appendChild(oImg);
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
    ignoredIssues[issue[0]] = '#' + issue[1];
    issue[2].disabled = true;
    issue[3].disabled = false;
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

            addIssueButton.addEventListener("click", addIssue.bind(null, [issue.id, issue.message, addIssueButton, removeIssueButton]));
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
            var oImg = document.createElement("img");
            oImg.setAttribute('src', 'img/' + issue.classification.toLowerCase() + '.png');
            oImg.setAttribute('alt', 'issue.classification');
            oImg.setAttribute("title", issue.classification);
            myCell.appendChild(oImg);
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
    let myParent = document.getElementsByTagName("body").item(0);
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
    myParent.appendChild(myTable);
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
    var fP = document.querySelector("footer span");
    fP.innerHTML = viadee + " - " + vPavName + " " + vPavVersion;
}


//set Filename as Header
function setUeberschrift(name) {
    let subName = name.substr(0, name.length - 5);
    document.querySelector("#model").innerHTML = subName;
    setFocus(name);
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
(function () {
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
            a.innerHTML = subName + " <span class='badge badge-pill pt-1 pb-1 badge-result'>" + countIssues(model.name, elementsToMark) + "</span>";
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
})();

// List all view modes
function createViewModesNavBar(model) {
    if (countIssues(model, elementsToMark) <= 0)
        document.getElementById("showAllIssues").parentNode.remove();
    if (countIssues(model, noIssuesElements) <= 0)
        document.getElementById("showSuccess").parentNode.remove();
    if (proz_vars !== undefined && proz_vars.length <= 0)
        document.getElementById("showVariables").parentNode.remove();
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
            setUeberschrift(diagramXML.name);
            addCountOverlay(overlays, overlayData);
            markNodes(elements, canvas);
            controller.loadCodeElements();
            document.getElementById('project_name').innerText = properties["projectName"];
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

// Define attributes which reference source code
const sourceCodeAttributes = ["camunda:class", "class",
    "camunda:delegateExpression", "delegateExpression",
    "camunda:variableMappingClass", "camunda:variableMappingDelegateExpression"];

// Init
let bpmnFile = diagramXMLSource[0].name;
createViewModesNavBar(bpmnFile);
const controller = createViewController();
controller.init();
showUnlocatedCheckers();
