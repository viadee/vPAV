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
    return elements.filter(element =>(element.bpmnFile === ("src\\main\\resources\\" + bpmnFile)) && (element.elementId !== ""));
}

//mark invalid path
function getElementsOnPath(id, pos) {
    let pathElements = [];
    for (y in elementsToMark) {
        if (elementsToMark[y].id == id) {
            for (x in elementsToMark[y].paths[pos]) {
                if (elementsToMark[y].paths[pos][x].elementId != "")
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
            overlayHtml.setAttribute("class", "badge badge-pill badge-pill-cursor " + element.colorClass);
            overlayHtml.setAttribute("type", "button");
            overlayHtml.setAttribute("data-toggle", "bmodal");
            overlayHtml.setAttribute("data-target", "#issueModal");
            overlayHtml.setAttribute("title", element.title);
            overlayHtml.innerHTML = element.anz;
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
        .filter(p => p.bpmnFile === ("src\\main\\resources\\" + bpmnFile))
        .filter(p => p.elementIid !== "");

    return filteredVariables.map(p => {
        let overlayData = {};
        overlayData.i = p;
        overlayData.anz = p.read.length + p.write.length + p.delete.length;
        overlayData.clickOverlay = createVariableDialog(p);
        overlayData.colorClass = "badge-info";
        overlayData.title = "variable operations";
        return overlayData;
    });
}

function getIssueOverlays(bpmnFile) {
    //getElemtIds
    var eId = [];
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile))
            if (elementsToMark[id].elementId != "")
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
            if (eId[j] == anzId)
                anz++;
        }
        objFehler = {eid: eIdUnique[i], anz: anz};
        anzArray[i] = objFehler;
        anz = 0;
    }

    //Add count on each issue
    var issue = { i: "dummy", anz: 0 };
    var issues = [];
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
            var obj = elementsToMark[id];
            for (var i = 0; i < anzArray.length; i++) {
                if (anzArray[i].eid == obj.elementId) {
                    issue = {i: elementsToMark[id], anz: anzArray[i].anz};
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
                    issue.colorClass = "badge-danger";
                }
                if (element.Criticality === "WARNING") {
                    issue.colorClass = "badge-warning";
                }
                if (element.Criticality === "INFO") {
                    issue.colorClass = "badge-info";
                }
            }
        });
    });
    return issues;
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

                    var dCardIssueId = document.createElement("p");
                    dCardIssueId.setAttribute("class", "card-issueId issue-id");

                    var dCardIssueButton = document.createElement("button");
                    dCardIssueButton.setAttribute("class", "btn btn-viadee issue-button");
                    dCardIssueButton.addEventListener("click", addIssue.bind(null, [issue.id, issue.message, dCardIssueButton]));
                    dCardIssueButton.innerHTML = "Add Issue";

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
                    dCardIssueId.innerHTML = "<h6><b>Issue Id:</b></h6>" + issue.id;


                    dCard.appendChild(dCardTitle);
                    dCardBody.appendChild(dCardText);
                    if (issue.ruleDescription)
                        dCardBody.appendChild(dCardRuleDescription);
                    if (issue.elementDescription)
                        dCardBody.appendChild(dCardElementDescription);
                    dCardBody.appendChild(dCardIssueId);
                    dCard.appendChild(dCardBody);
                    dCardBody.appendChild(dCardIssueButton);

                    dialogContent.appendChild(dCard);
                }
            }
        }
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

        dialogContent.appendChild(createCardForVariableOperations(processVariable.read, "Reads", processVariable.bpmnFile));
        dialogContent.appendChild(createCardForVariableOperations(processVariable.write, "Writes", processVariable.bpmnFile));
        dialogContent.appendChild(createCardForVariableOperations(processVariable.delete, "Deletes", processVariable.bpmnFile));

        showDialog('show');
    }
}

function createCardForVariableOperations(operations, title, bpmnFile) {
    var dCard = document.createElement("div");
    dCard.setAttribute("class", "card bg-light mb-3");

    var dCardBody = document.createElement("div");
    dCardBody.setAttribute("class", "card-body");

    var dCardTitle = document.createElement("h5");
    dCardTitle.setAttribute("class", "card-header");

    var dCardText = document.createElement("p");
    dCardText.setAttribute("class", "card-text");

    dCardTitle.innerHTML = title;
    dCardText.innerHTML = operations.map(p => createShowOperationsLink(bpmnFile, p).outerHTML).join(", ");

    dCard.appendChild(dCardTitle);
    dCardBody.appendChild(dCardText);
    dCard.appendChild(dCardBody);

    return dCard;
}

// Add single issue to the ignoreIssues list
function addIssue(issue){         
    ignoredIssues[issue[0]] = '#' + issue[1].substring(0,29) + "..";
    issue[2].disabled = true;
}

// download the ignoreIssues file 
function downloadFile(){
    var value;
    var blob = "";
    Object.keys(ignoredIssues).forEach(function(key) {
        value = ignoredIssues[key];
        blob = blob + value + "\n"+ key + "\n";
    });    
    download(new Blob([blob]),"ignoreIssues.txt", "text/plain");
}

//delete table under diagram
function deleteTable() {
    let myTable = document.getElementById("table_issues");
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
function createIssueTable(bpmnFile, tableContent) {
    var myTable = document.getElementById("table_issues");
    let myTHead = document.createElement("thead");
    let myRow = document.createElement("tr");
    myRow.setAttribute("id", "tr_ueberschriften");
    myRow.setAttribute("class", "table-primary");
    myRow.appendChild(createTableHeader("th_ruleName", "Rule-Name"));
    myRow.appendChild(createTableHeader("th_elementId", "Element-Id"));
    myRow.appendChild(createTableHeader("th_elementName", "Element-Name"));
    myRow.appendChild(createTableHeader("th_classification", "Class"));
    myRow.appendChild(createTableHeader("th_message", "Message"));
    myRow.appendChild(createTableHeader("th_paths", "Invalid Sequenceflow"));
    myTHead.appendChild(myRow);
    myTable.appendChild(myTHead);

    //fill table with all issuesof current model
    for (id in tableContent) {
        if (tableContent[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
            issue = tableContent[id];
            myParent = document.getElementsByTagName("body").item(0);
            myTBody = document.createElement("tbody");
            myRow = document.createElement("tr");

            //ruleName
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.ruleName);
            myCell.setAttribute("id", issue.classification) // mark cell

            //create link for default checkers
            var a = document.createElement("a");
            a.appendChild(myText);

            defaultCheckers.forEach(element => {
                if (issue.ruleName == element.rulename) {
                    a.setAttribute("href", "https://viadee.github.io/vPAV/" + issue.ruleName + ".html");
                    a.setAttribute("title", "Checker documentation");
                }
            });

            myCell.appendChild(a);

            //link to docu            
            myRow.appendChild(myCell);

            //elementId
            myCell = document.createElement("td");
            myCell.appendChild(createMarkElementLink(bpmnFile, issue));
            myRow.appendChild(myCell);

            //elementName
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.elementName);
            myCell.appendChild(myText);
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
            myText = document.createTextNode(issue.message);
            myCell.appendChild(myText);
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
                        else
                            if (y < issue.paths[x].length - 1)
                                path_text += issue.paths[x][y].elementName + " -> ";
                            else
                                path_text += issue.paths[x][y].elementName
                    }
                    myText = document.createTextNode("Mark invalid flow");

                    //mark path
                    var p = issue.paths[x];

                    var b = document.createElement("a");
                    b.appendChild(myText);
                    b.setAttribute("onclick", "showPath('" + bpmnFile.replace(/\\/g, "\\\\") + "','" + issue.id + "','" + x + "', '" + path_text + "')");
                    b.setAttribute("href", "#");

                    myCell.appendChild(b);
                    path_text = "";

                    //add break
                    br = document.createElement("br");
                    myCell.appendChild(br);
                    //only add break if its not the last one
                    if (x < issue.paths.length - 1) {
                        brz = document.createElement("br");
                        myCell.appendChild(brz);
                    }
                }
            }
            myRow.appendChild(myCell);
            //---------
            myParent.setAttribute("class", "container-fluid");
            myTBody.appendChild(myRow);
            myTable.appendChild(myTBody);
        }
    }
}

//create process variable table
function createVariableTable(bpmnFile, tableContent) {
    let myParent = document.getElementsByTagName("body").item(0);
    let myTable = document.getElementById("table_issues");
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
        if (processVariable.bpmnFile !== ("src\\main\\resources\\" + bpmnFile))
            continue;

        let myTBody = document.createElement("tbody");
        let myRow = document.createElement("tr");

        let myCell = document.createElement("td");
        myCell.appendChild(createShowOperationsLink(bpmnFile, processVariable.name));
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        let elementLinks = processVariable.read.map(p => createMarkElementLink(bpmnFile, p));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        elementLinks = processVariable.write.map(p => createMarkElementLink(bpmnFile, p));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);

        myCell = document.createElement("td");
        elementLinks = processVariable.delete.map(p => createMarkElementLink(bpmnFile, p));
        myCell.innerHTML = elementLinks.map(l => l.outerHTML).join(", ");
        myRow.appendChild(myCell);
        //---------
        myParent.setAttribute("class", "container-fluid");
        myTBody.appendChild(myRow);
        myTable.appendChild(myTBody);
    }
    myParent.appendChild(myTable);
}

function createMarkElementLink(bpmnFile, element) {
    let myText = document.createTextNode(element.elementName);
    //create link
    let c = document.createElement("a");
    c.appendChild(myText);
    if (element.elementId !== "") {
        c.setAttribute("onclick", "showMarkedElement('" + bpmnFile.replace(/\\/g, "\\\\") + "','" + element.elementId + "')");
        c.setAttribute("href", "#");
        c.setAttribute("title", "mark element");
    }
    return c;
}

function createShowOperationsLink(bpmnFile, processVariableName) {
    //create link
    let c = document.createElement("a");
    let myText = document.createTextNode(processVariableName);
    c.appendChild(myText);
    c.setAttribute("onclick", "showVariableOperations('" + bpmnFile.replace(/\\/g, "\\\\") + "','" + processVariableName + "')");
    c.setAttribute("href", "#");
    c.setAttribute("title", "mark operations");
    c.setAttribute("data-dismiss", "modal");
    return c;
}

/**
 * create Footer
 */
function createFooter() {
    const body = document.querySelector("body");
    let footer = document.querySelector("footer");
    //delete footer first
    if (!(footer === null))
        footer.parentNode.removeChild(footer);

    footer = document.createElement("footer");
    footer.setAttribute("class", "footer viadee-footer");

    var fP = document.createElement("span");
    fP.setAttribute("class", "text-muted-viadee");
    fP.innerHTML = viadee + " - " + vPavName + " " + vPavVersion;

    var aL = document.createElement("a");
    aL.setAttribute("class", "text-muted-viadee float-right pr-2");
    aL.setAttribute("href", "https://viadee.github.io/vPAV/#licenses");
    aL.innerHTML = "Licenses";

    var aI = document.createElement("a");
    aI.setAttribute("class", "text-muted-viadee float-right pr-2");
    aI.setAttribute("href", "https://www.viadee.de/impressum-datenschutz.html");
    aI.innerHTML = "Imprint";

    fP.appendChild(aL);
    fP.appendChild(aI);
    footer.appendChild(fP);
    body.appendChild(footer);
}

const tableViewModes = Object.freeze({
    ISSUES:   Symbol("issues"),
    NO_ISSUES:  Symbol("no issues"),
    VARIABLES: Symbol("process variables")
});

function createTableFromViewMode(tableViewMode, diagramName) {
    deleteTable();
    if (tableViewMode === tableViewModes.VARIABLES) {
        createVariableTable(diagramName, processVariables);
    } else if (countIssues(diagramName, elementsToMark) > 0) {
        if (tableViewMode === tableViewModes.ISSUES)
            createIssueTable(diagramName, elementsToMark);
        else
            createIssueTable(diagramName, noIssuesElements);
    } else {
        createIssueTable(diagramName, noIssuesElements);
    }
    createFooter();
}

/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js viewer.
 */
function initDiagram(diagramXML, elements, overlayData) {
    // remove current diagram
    document.querySelector("#canvas").innerHTML = "";

    // create viewer
    let bpmnViewer = new window.BpmnJS({
        container: '#canvas'
    });

    // import diagram
    bpmnViewer.importXML(diagramXML.xml, function (err) {
        if (err) {
            return console.error('could not import BPMN 2.0 diagram', err);
        }

        var canvas = bpmnViewer.get('canvas'),
            overlays = bpmnViewer.get('overlays');

        // zoom to fit full viewport
        canvas.zoom('fit-viewport');
        setUeberschrift(diagramXML.name);
        addCountOverlay(overlays, overlayData);
        markNodes(elements, canvas);
    });
}

//set Filename as Header
function setUeberschrift(name) {
    subName = name.substr(0, name.length - 5);
    document.querySelector("#modell").innerHTML = subName;
    var mDownload = document.getElementById("model_download");
    mDownload.setAttribute("href", "../../src/main/resources/" + name);
    setFocus(name);
}

//get issue count from specific bpmnFile
function countIssues(bpmnFile, tableContent) {
    count = 0;
    for (id in tableContent) {
        if (tableContent[id].bpmnFile === ("src\\main\\resources\\" + bpmnFile)) {
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
        li.setAttribute("class", "nav-item");
        if (countIssues(model.name, elementsToMark) === 0)
            a.innerHTML = subName + " <span class='badge badge-pill badge-success pt-1 pb-1'>" + countIssues(model.name, elementsToMark) + "</span>";
        else
            a.innerHTML = subName + " <span class='badge badge-pill pt-1 pb-1 viadee-darkblue-text viadee-pill-bg'>" + countIssues(model.name, elementsToMark) + "</span>";
        a.setAttribute("onclick", "showIssues('" + model.name.replace(/\\/g, "\\\\") + "', tableViewModes.ISSUES)");
        a.setAttribute("href", "#");
        if (first === true) {
            a.setAttribute("class", "nav-link active");
            first = false;
        } else {
            a.setAttribute("class", "nav-link");
        }

        a.setAttribute("id", model.name);
        ul.appendChild(li);
    }
})();

// List all view modes
function createViewModesNavBar(model) {
    if (countIssues(model, elementsToMark) > 0)
        createNavItem("All issues", "showAllIssues", "showIssues('" + model.replace(/\\/g, "\\\\") + "', tableViewModes.ISSUES)");
    if (countIssues(model, noIssuesElements) > 0)
        createNavItem("Checkers without issues", "showSuccess", "showIssues('" + model.replace(/\\/g, "\\\\") + "', tableViewModes.NO_ISSUES)");
    if (proz_vars !== undefined && proz_vars.length > 0)
        createNavItem("Process variables", "showVariables", "showProcessVariables('" + model.replace(/\\/g, "\\\\") + "')");
}

function createNavItem(title, id, onClick) {
    let ul = document.getElementById("viewModeNavBar");
    let li = document.createElement("li");
    let a = document.createElement("a");
    a.innerHTML = title;
    a.setAttribute("onclick", onClick);
    a.setAttribute("href", "#");
    a.setAttribute("class", "nav-link");
    a.setAttribute("id", id);
    li.appendChild(a);
    li.setAttribute("class", "nav-item");

    ul.appendChild(li);
}

function setFocus(name) {
    document.getElementById(name).focus();
}

/**
 * reload model diagram with issue overlay
 * @param modelName
 * specify which model to show
 * @param tableViewMode
 * specify which issue to show in table
 */
function showIssues(modelName, tableViewMode) {
    document.getElementById("rowPath").setAttribute("class", "collapse");

    let diagramXML = getModel(modelName);
    if (diagramXML === undefined)
        return;

    initDiagram(diagramXML, filterElementsByModel(elementsToMark, diagramXML.name), getIssueOverlays(diagramXML.name));
    createTableFromViewMode(tableViewMode, diagramXML.name);
}

function showPath(modelName, elementId, path_nr, path) {
    let diagramXML = getModel(modelName);
    if (diagramXML === undefined)
        return;

    document.getElementById('invalidPath').innerHTML = path;
    document.getElementById("rowPath").setAttribute("class", "collapse.show");
    document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse");

    initDiagram(diagramXML, getElementsOnPath(elementId, path_nr), []);
}

function showProcessVariables(modelName) {
    document.getElementById("rowPath").setAttribute("class", "collapse");

    let diagramXML = getModel(modelName);
    if (diagramXML === undefined)
        return;

    initDiagram(diagramXML, [], getProcessVariableOverlay(diagramXML.name));
    createTableFromViewMode(tableViewModes.VARIABLES, diagramXML.name);
}

function showMarkedElement(modelName, elementId) {
    document.getElementById("rowPath").setAttribute("class", "collapse");

    let diagramXML = getModel(modelName);
    if (diagramXML === undefined)
        return;

    initDiagram(diagramXML, [{elementId: elementId, classification: 'one-element'}], []);
}

function showVariableOperations(modelName, variableName) {
    document.getElementById("rowPath").setAttribute("class", "collapse");

    let diagramXML = getModel(modelName);
    if (diagramXML === undefined)
        return;

    let processVariable = processVariables.find(p => p.name === variableName);
    let operations = processVariable.read.concat(processVariable.write, processVariable.delete);
    let elements = operations.map(o => {
        o.classification = "one-element";
        return o;
    });

    initDiagram(diagramXML, elements, getProcessVariableOverlay(diagramXML.name));
}

function getModel(modelName) {
    for (let id = 0; id <= diagramXMLSource.length - 1; id++) {
        var a = document.getElementById(diagramXMLSource[id].name);
        a.setAttribute("class", "nav-link");
        if (diagramXMLSource[id].name === modelName) {
            a.setAttribute("class", "nav-link active");
            return diagramXMLSource[id];
        }
    }
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

// Init
let bpmnFile = diagramXMLSource[0].name;
createViewModesNavBar(bpmnFile);
showIssues(bpmnFile, tableViewModes.ISSUES);
document.getElementById('vPAV').innerHTML = vPavVersion;
showUnlocatedCheckers();


