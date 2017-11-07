//mark all nodes with issues
function markNodes(canvas, bpmnFile) {
    for (id in elementsToMark) {
        if ((elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) && (elementsToMark[id].elementId != "")) {
            if (elementsToMark[id].ruleName == "VersioningChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'VersioningChecker');
            } else if (elementsToMark[id].ruleName == "ProcessVariablesNameConventionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'ProcessVariablesNameConventionChecker');
            } else if (elementsToMark[id].ruleName == "JavaDelegateChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'JavaDelegateChecker');
            } else if (elementsToMark[id].ruleName == "EmbeddedGroovyScriptChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'EmbeddedGroovyScriptChecker');
            } else if (elementsToMark[id].ruleName == "DmnTaskChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'DmnTaskChecker');
            } else if (elementsToMark[id].ruleName == "ProcessVariablesModelChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'ProcessVariablesModelChecker');
            } else if (elementsToMark[id].ruleName == "TaskNamingConventionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'TaskNamingConventionChecker');
            } else if (elementsToMark[id].ruleName == "NoScriptChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'NoScriptChecker');
            } else if (elementsToMark[id].ruleName == "XorConventionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'XorConventionChecker');
            } else if (elementsToMark[id].ruleName == "ElementIdConventionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'ElementIdConventionChecker');
            } else if (elementsToMark[id].ruleName == "TimerExpressionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'TimerExpressionChecker');
            } else if (elementsToMark[id].ruleName == "NoExpressionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'NoExpressionChecker');
            } else if (elementsToMark[id].ruleName == "MessageEventChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'MessageEventChecker');
            } else {
                canvas.addMarker(elementsToMark[id].elementId, 'new');
            }
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
    canvas.addMarker(id, 'oneElement');
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
        var overlayHtml = document.createElement("span");
        overlayHtml.setAttribute("class", "badge badge-pill badge-danger");
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


                        var oImg = document.createElement("img");
                        oImg.setAttribute('src', 'img/' + issue.classification + '.png');
                        oImg.setAttribute('alt', 'issue.classification');
                        oImg.setAttribute('class', 'float-left mr-2');
                        oImg.setAttribute("title", issue.classification);

                        dCardTitle.innerHTML = issue.ruleName;
                        dCardTitle.appendChild(oImg);
                        dCardText.innerHTML = issue.message;

                        dCard.appendChild(dCardTitle);
                        dCardBody.appendChild(dCardText);
                        dCard.appendChild(dCardBody);

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
            myCell.setAttribute("id", issue.ruleName) // mark cell
            //create link 
            var a = document.createElement("a");
            a.appendChild(myText);
            //link to docu
            a.setAttribute("href", "https://viadee.github.io/vPAV/" + issue.ruleName + ".html");
            a.setAttribute("title", "checker documentation");
            myCell.appendChild(a);
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
    footer.setAttribute("class", "footer pt-1 pb-1 pl-2 m-0");
    footer.style.backgroundColor = "#CED6E3";

    var fP = document.createElement("span");
    fP.setAttribute("class", "text-muted");
    fP.innerHTML = viadee + " - " + vPavName + " " + vPavVersion;

    var aL = document.createElement("a");
    aL.setAttribute("class", "text-muted float-right pr-2");
    aL.setAttribute("href", "https://viadee.github.io/vPAV/#licenses");
    aL.innerHTML = "Licenses";

    var aI = document.createElement("a");
    aI.setAttribute("class", "text-muted float-right pr-2");
    aI.setAttribute("href", "https://www.viadee.de/impressum-datenschutz.html");
    aI.innerHTML = "Impressum";

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
                if (func == 0 || func == null) {
                    markNodes(canvas, diagramXML.name);
                    addCountOverlay(overlays, diagramXML.name);
                } else if (func == 1) {
                    markPath(canvas, issue_id, path_nr, diagramXML.name);
                } else if (func == 2) {
                    markElement(canvas, issue_id, diagramXML.name);
                }
                if (success)
                    createTable(diagramXML.name, noIssuesElements);
                else
                    createTable(diagramXML.name, elementsToMark);
                tableVisible(true);
                createFooter();
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
    document.querySelector("#modell").innerHTML = "Consistency check: " + subName;
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
    for (var id = 0; id <= diagramXMLSource.length -1; id++) {
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
            a.innerHTML = subName + " <span class='badge badge-pill pt-1 pb-1 viadee-darkblue-text viadee-lightblue-bg'>" + countIssues(model.name, elementsToMark) + "</span>";
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
    document.getElementById("rowPath").setAttribute("class", "collapse");

    //delete footer
    const footer = document.querySelector("footer");
    if (!(footer === null))
        footer.parentNode.removeChild(footer);
    for (var id = 0; id <= diagramXMLSource.length -1; id++) {
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
viewer = initDiagram(diagramXMLSource[0], 0, null, false);
activateLinkSuccess(diagramXMLSource[0].name);
document.getElementById('vPAV').innerHTML = vPavVersion;