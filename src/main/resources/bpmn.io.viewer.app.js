//mark all nodes with issues
function markNodes(canvas, bpmnFile) {

    for (id in elementsToMark) {
        try {
            if ((elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) && (elementsToMark[id].elementId != "")) {
                if (elementsToMark[id].classification == "ERROR") {
                    canvas.addMarker(elementsToMark[id].elementId, 'error');
                } else if (elementsToMark[id].classification == "WARNING") {
                    canvas.addMarker(elementsToMark[id].elementId, 'warning');
                } else if (elementsToMark[id].classification == "INFO") {
                    canvas.addMarker(elementsToMark[id].elementId, 'info');
                }
            }
        } catch (err) {
            console.log("element not found");
        }
    }
}

//add Botton "mark all issues"
function activateButtonAllIssues(model) {
    var btReset = document.getElementById("reset");
    btReset.setAttribute("class", "btn btn-viadee mt-2 collapse.show");
    btReset.setAttribute("onclick", "selectModel('" + model.replace(/\\/g, "\\\\") + "', null, null, 0, 0)");
    btReset.setAttribute("href", "#");
}

//add attribute to link
function activateLinkSuccess(model) {
    var aSuccess = document.getElementById("success");
    if (countIssues(model, noIssuesElements) > 0)
        aSuccess.setAttribute("class", "btn btn-viadee mt-2 collapse.show");
    else
        aSuccess.setAttribute("class", "btn btn-viadee mt-2 collapse");
    aSuccess.setAttribute("onclick", "selectModel('" + model.replace(/\\/g, "\\\\") + "', null, null, 3, 0)");
    aSuccess.setAttribute("href", "#");
}

//mark invalide path
function markPath(canvas, id, pos, model) {
    activateButtonAllIssues(model);

    for (y in elementsToMark) {
        if (elementsToMark[y].id == id) {
            for (x in elementsToMark[y].paths[pos]) {
                if (elementsToMark[y].paths[pos][x].elementId != "")
                    canvas.addMarker(elementsToMark[y].paths[pos][x].elementId, 'path');
            }
        }
    }
}

//mark one element
function markElement(canvas, id, model) {
    activateButtonAllIssues(model);
    try {
        canvas.addMarker(id, 'oneElement');
    } catch (err) {
        console.log("element not found");
    }
}

//create issue count on each node
function addCountOverlay(overlays, bpmnFile) {

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
    var objFehler = { eid: "dummy", anz: 0 };
    var anzArray = [];

    for (i = 0; i < eIdUnique.length; i++) {
        var anzId = eIdUnique[i];
        for (j = 0; j < eId.length; j++) {
            if (eId[j] == anzId)
                anz++;
        }
        objFehler = { eid: eIdUnique[i], anz: anz };
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
                    issue = { i: elementsToMark[id], anz: anzArray[i].anz };
                    issues[id] = issue;
                }
            }
        }
    }

    //Add Overlays
    for (id in issues) {
        try {

            var overlayHtml = document.createElement("span");


            issueSeverity.forEach(element => {
                if (element.id == issues[id].i.elementId) {
                    if (element.Criticality == "ERROR") {
                        overlayHtml.setAttribute("class", "badge badge-pill badge-danger badge-pill-cursor");
                    }
                    if (element.Criticality == "WARNING") {
                        overlayHtml.setAttribute("class", "badge badge-pill badge-warning badge-pill-cursor");
                    }
                    if (element.Criticality == "INFO") {
                        overlayHtml.setAttribute("class", "badge badge-pill badge-info badge-pill-cursor");
                    }
                }
            });

            overlayHtml.setAttribute("type", "button");
            overlayHtml.setAttribute("data-toggle", "bmodal");
            overlayHtml.setAttribute("data-target", "#issueModal");
            overlayHtml.setAttribute("title", "issues");
            overlayHtml.innerHTML = issues[id].anz;

            // add DialogMessage
            function clickOverlay(id) {
                //clear dialog
                const dialogContent = document.querySelector(".modal-body");
                while (dialogContent.hasChildNodes()) {
                    dialogContent.removeChild(dialogContent.lastChild);
                }
                if (issues[id].i.elementId != "") {
                    var eId = issues[id].i.elementId;
                    for (y in issues) {
                        if (issues[y].i.elementId == eId) {
                            var issue = issues[y].i;

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
                            oImg.setAttribute('src', 'img/' + issue.classification + '.png');
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
                toggleDialog('show');
            }

            overlayHtml.onclick = (function () {
                var currentId = id;
                return function () {
                    clickOverlay(currentId);
                };

            })();
            attachOverlay();
        } catch (err) {
            console.log("element not found");
        }
    }

    function attachOverlay(r) {
        // attach the overlayHtml to a node
        overlays.add(issues[id].i.elementId, {
            position: {
                bottom: 10,
                right: 20
            },
            html: overlayHtml
        });
    }
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
    //delete tBodys
    var tb = document.querySelectorAll('tbody');
    for (var i = 0; i < tb.length; i++) {
        if (tb[i].children.length === 0) {
            tb[i].parentNode.removeChild(tb[i]);
        }
    }

    var myTable = document.getElementById("table_issues");
    //delete rows
    while (myTable.rows.length > 1) {
        myTable.deleteRow(myTable.rows.length - 1);
    }
}
//create issue table
function createTable(bpmnFile, tableContent) {
    var myTable = document.getElementById("table_issues");

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
            myText = document.createTextNode(issue.elementId);
            //create link 
            var c = document.createElement("a");
            c.appendChild(myText);
            if (issue.elementId != "") {
                c.setAttribute("onclick", "selectModel('" + bpmnFile.replace(/\\/g, "\\\\") + "','" + issue.elementId + "', 0 , 2)");
                c.setAttribute("href", "#");
                c.setAttribute("title", "mark element");
            }
            myCell.appendChild(c);
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
            oImg.setAttribute('src', 'img/' + issue.classification + '.png');
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
                    b.setAttribute("onclick", "selectModel('" + bpmnFile.replace(/\\/g, "\\\\") + "','" + issue.id + "','" + x + "', 1, '" + path_text + "')");
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
            myParent.appendChild(myTable);
        }
    }
}

/**
 * create Footer
 */
function createFooter() {
    const body = document.querySelector("body");
    var footer = document.createElement("footer");
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

/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js viewer.
 */
function initDiagram(diagramXML, issue_id, path_nr, func, success) {
    // create viewer
    var bpmnViewer = new window.BpmnJS({
        container: '#canvas'
    });

    // import function
    function importXML(xml) {

        // import diagram
        bpmnViewer.importXML(xml, function (err) {

            if (err) {
                return console.error('could not import BPMN 2.0 diagram', err);
            }

            var canvas = bpmnViewer.get('canvas'),
                overlays = bpmnViewer.get('overlays');

            // zoom to fit full viewport
            canvas.zoom('fit-viewport');
            setUeberschrift(diagramXML.name);
            if (countIssues(diagramXML.name, elementsToMark) > 0) {
                //createTable
                if (success)
                    createTable(diagramXML.name, noIssuesElements);
                else
                    createTable(diagramXML.name, elementsToMark);
                tableVisible(true);
                createFooter();

                //MarkElements
                if (func == 0 || func == null) {
                    markNodes(canvas, diagramXML.name);
                    addCountOverlay(overlays, diagramXML.name);
                } else if (func == 1) {
                    markPath(canvas, issue_id, path_nr, diagramXML.name);
                } else if (func == 2) {
                    markElement(canvas, issue_id, diagramXML.name);
                }
            } else {
                document.getElementById("success").setAttribute("class", "btn btn-viadee mt-2 collapse");
                createTable(diagramXML.name, noIssuesElements);
                tableVisible(true);
                createFooter();
            }
        });
    };

    bpmnViewer.xml = diagramXML.xml;

    bpmnViewer.reload = function (model, success) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, null, null, 0, success);
    };

    bpmnViewer.reloadMarkPath = function (model, issue_id, path_nr) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, issue_id, path_nr, 1, false);
    };

    bpmnViewer.reloadMarkElement = function (model, issue_id) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, issue_id, null, 2, false);
    };

    // import xml
    importXML(diagramXML.xml);

    return bpmnViewer;
};

//set Filename as Header
function setUeberschrift(name) {
    subName = name.substr(0, name.length - 5);
    document.querySelector("#modell").innerHTML = subName;
    var mDownload = document.getElementById("model_download");
    mDownload.setAttribute("href", "../../src/main/resources/" + name);
    setFocus(name);
}

//hideTable
function tableVisible(show) {
    if (show) {
        document.getElementById("tableHeader").style.display = "block";
        document.getElementById("table_issues").style.display = "table";
    } else {
        document.getElementById("tableHeader").style.display = "none";
        document.getElementById("table_issues").style.display = "none";
    }
}

//get issue count from specific bpmnFile
function countIssues(bpmnFile, tableContent) {
    count = 0;
    for (id in tableContent) {
        if (tableContent[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
            count++;
        }
    }
    return count;
}

//dialog
var dialogOpen = false, lastFocus, dialog, okbutton, pagebackground;
function toggleDialog(sh) {
    dialog = $('#issueModal');
    dialog.modal();
}

// List all ProcessInstances
(function () {
    var first = true;
    for (var id = 0; id <= diagramXMLSource.length - 1; id++) {
        model = diagramXMLSource[id];
        var ul = document.getElementById("linkList");
        var li = document.createElement("li");
        var a = document.createElement("a");
        var subName = model.name.substr(0, model.name.length - 5);
        li.appendChild(a);
        li.setAttribute("class", "nav-item");
        if (countIssues(model.name, elementsToMark) == 0)
            a.innerHTML = subName + " <span class='badge badge-pill badge-success pt-1 pb-1'>" + countIssues(model.name, elementsToMark) + "</span>";
        else
            a.innerHTML = subName + " <span class='badge badge-pill pt-1 pb-1 viadee-darkblue-text viadee-pill-bg'>" + countIssues(model.name, elementsToMark) + "</span>";
        a.setAttribute("onclick", "selectModel('" + model.name.replace(/\\/g, "\\\\") + "', null, null, 0, 0)");
        a.setAttribute("href", "#");
        if (first == true) {
            a.setAttribute("class", "nav-link active");
            first = false;
        } else {
            a.setAttribute("class", "nav-link");
        }

        a.setAttribute("id", model.name);
        ul.appendChild(li);
    }
})();

function setFocus(name) {
    document.getElementById(name).focus();
}

/**
 * reload model diagram
 * @param {*} name 
 * model name
 * @param {*} issue_id 
 * id of element to mark
 * @param {*} path_nr
 * number of path 
 * @param {*} func
 * 0 = normal 
 * 1 = mark path
 * 2 = mark one element
 * @param {*} path 
 * path to mark
 * @param {*} success
 * show checkers without issues 
 */
function selectModel(name, issue_id, path_nr, func, path) {

    var description = document.getElementById("tableHeader");
    if (func == 3) {
        description.setAttribute("data-content", 'Correct Checkers:');
    }
    if (func == 0) {
        description.setAttribute("data-content", 'Errors found:');
    }


    document.getElementById("rowPath").setAttribute("class", "collapse");

    //delete footer
    const footer = document.querySelector("footer");
    if (!(footer === null))
        footer.parentNode.removeChild(footer);
    for (var id = 0; id <= diagramXMLSource.length - 1; id++) {
        var a = document.getElementById(diagramXMLSource[id].name);
        a.setAttribute("class", "nav-link");
        if (diagramXMLSource[id].name === name) {
            a.setAttribute("class", "nav-link active");
            activateLinkSuccess(diagramXMLSource[id].name);
            if (func == 0) {
                viewer.reload(diagramXMLSource[id], false);
                document.getElementById("reset").setAttribute("class", "btn btn-viadee mt-2 collapse");
            } else if (func == 1) {
                viewer.reloadMarkPath(diagramXMLSource[id], issue_id, path_nr);
                document.getElementById('invalidPath').innerHTML = path;
                document.getElementById("rowPath").setAttribute("class", "collapse.show");
            } else if (func == 2) {
                viewer.reloadMarkElement(diagramXMLSource[id], issue_id);
            } else if (func == 3) {
                viewer.reload(diagramXMLSource[id], true);
                document.getElementById("success").setAttribute("class", "btn btn-viadee mt-2 collapse");
                activateButtonAllIssues(diagramXMLSource[id].name);
            }
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


viewer = initDiagram(diagramXMLSource[0], 0, null, false);
activateLinkSuccess(diagramXMLSource[0].name);
document.getElementById('vPAV').innerHTML = vPavVersion;
showUnlocatedCheckers();


