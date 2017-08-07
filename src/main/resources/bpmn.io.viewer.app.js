//mark all nodes with issues
function markNodes(canvas, bpmnFile) {
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
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
            } else if (elementsToMark[id].ruleName == "XorNamingConventionChecker") {
                canvas.addMarker(elementsToMark[id].elementId, 'XorNamingConventionChecker');
            } else {
                canvas.addMarker(elementsToMark[id].elementId, 'new');
            }
        }
    }
}

//mark invalide path
function markPath(canvas, id, pos) {
    for (y in elementsToMark) {
        if (elementsToMark[y].id == id) {
            for (x in elementsToMark[y].paths[pos]) {
                canvas.addMarker(elementsToMark[y].paths[pos][x].elementId, 'path');
            }
        }
    }
}

//mark one element
function markElement(canvas, id) {
    canvas.addMarker(id, 'oneElement');
}

//create issue count on each node
function addCountOverlay(overlays, bpmnFile) {

    //getElemtIds
    var eId = [];
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile))
            eId[id] = elementsToMark[id].elementId;
    }

    //doppelte Löschen
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

    //Anzahl ergänzen
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

    //Anzahl an alle Fehler hängen
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
        var overlayHtml = document.createElement("div");
        overlayHtml.setAttribute("class", "diagram-zahl");
        overlayHtml.appendChild(document.createTextNode(issues[id].anz));

        // add DialogMessage
        function clickOverlay(id) {
            //clear dialog
            var dp = document.querySelectorAll('.d');
            for (var i = 0; i < dp.length; i++) {
                if (dp[i].children.length === 0) {
                    dp[i].parentNode.removeChild(dp[i]);
                }
            }

            var eId = issues[id].i.elementId;
            for (y in issues) {
                if (issues[y].i.elementId == eId) {
                    var issue = issues[y].i;
                    var dl = document.getElementById("dia");

                    var hClass = document.createElement("h3");
                    var pRule = document.createElement("p");
                    var pMessage = document.createElement("p");

                    pMessage.setAttribute("id", "d_message");
                    pMessage.setAttribute("class", "d");
                    hClass.setAttribute("class", "d");
                    pRule.setAttribute("class", "d");

                    hClass.appendChild(document.createTextNode(issue.classification));
                    pRule.appendChild(document.createTextNode(issue.ruleName));
                    pMessage.appendChild(document.createTextNode(issue.message));

                    dl.appendChild(hClass);
                    dl.appendChild(pRule);
                    dl.appendChild(pMessage);
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
function createTable(bpmnFile) {
    var myTable = document.getElementById("table_issues");

    //fill table with all issuesof current model
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
            issue = elementsToMark[id];
            myParent = document.getElementsByTagName("body").item(0);

            myTBody = document.createElement("tbody");
            myRow = document.createElement("tr");
            //ruleName
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.ruleName);
            myRow.setAttribute("id", issue.ruleName) // mark hole row

            //create link 
            var a = document.createElement("a");
            a.appendChild(myText);
            //link to docu
            a.setAttribute("href", "https://github.com/viadee/vPAV/tree/master/docs/" + issue.ruleName + ".md");

            myCell.appendChild(a);
            myRow.appendChild(myCell);
            //elementId
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.elementId);

            //create link 
            var c = document.createElement("a");
            c.appendChild(myText);
            c.setAttribute("onclick", "selectModel('" + bpmnFile.replace("\\", "\\\\") + "','" + issue.elementId + "', 0 , 2)");
            c.setAttribute("href", "#");

            myCell.appendChild(c);
            myRow.appendChild(myCell);
            //elementName
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.elementName);
            myCell.appendChild(myText);
            myRow.appendChild(myCell);
            //classification
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.classification);
            myCell.appendChild(myText);
            myRow.appendChild(myCell);
            //message
            myCell = document.createElement("td");
            myText = document.createTextNode(issue.message);
            myCell.appendChild(myText);
            myRow.appendChild(myCell);
            //path
            myCell = document.createElement("td");
            var path_text = "";
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
                myText = document.createTextNode(path_text);

                //path markieren
                var p = issue.paths[x];

                var b = document.createElement("a");
                b.appendChild(myText);
                b.setAttribute("onclick", "selectModel('" + bpmnFile.replace("\\", "\\\\") + "','" + issue.id + "','" + x + "', 1)");
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
            myRow.appendChild(myCell);
            //---------
            myTBody.appendChild(myRow);
            myTable.appendChild(myTBody);
            myParent.appendChild(myTable);
        }
    }
}

/**
 * bpmn-js-seed
 *
 * This is an example script that loads an embedded diagram file <diagramXML>
 * and opens it using the bpmn-js viewer.
 */
function initDiagram(diagramXML, issue_id, path_nr, func) {
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
            if (countIssues(diagramXML.name) > 0) {
                if (func == 0 || func == null) {
                    markNodes(canvas, diagramXML.name);
                    addCountOverlay(overlays, diagramXML.name);
                } else if (func == 1) {
                    markPath(canvas, issue_id, path_nr);
                } else if (func == 2) {
                    markElement(canvas, issue_id);
                }
                createTable(diagramXML.name);
                tableVisible(true);
            } else {
                document.getElementById("noIssues").style.display = "initial";
                tableVisible(false);
            }
        });
    };

    bpmnViewer.xml = diagramXML.xml;

    bpmnViewer.reload = function (model) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, null, null, 0);
    };

    bpmnViewer.reloadMarkPath = function (model, issue_id, path_nr) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, issue_id, path_nr, 1);
    };

    bpmnViewer.reloadMarkElement = function (model, issue_id) {
        document.querySelector("#canvas").innerHTML = "";
        deleteTable();
        initDiagram(model, issue_id, null, 2);
    };

    // import xml
    importXML(diagramXML.xml);

    return bpmnViewer;
};

//set Filename as Header
function setUeberschrift(name) {
    subName = name.substr(0, name.length - 5);
    document.querySelector("#modell").innerHTML = "Consistency check: " + subName;
    document.getElementById("noIssues").style.display = "none";
    setFocus(name);
}

//hideTable
function tableVisible(show) {
    if (show) {
        document.getElementById("h1_table").style.display = "block";
        document.getElementById("table_issues").style.display = "table";
    } else {
        document.getElementById("h1_table").style.display = "none";
        document.getElementById("table_issues").style.display = "none";
    }
}

//get issue count from specific bpmnFile
function countIssues(bpmnFile) {
    count = 0;
    for (id in elementsToMark) {
        if (elementsToMark[id].bpmnFile == ("src\\main\\resources\\" + bpmnFile)) {
            count++;
        }
    }
    return count;
}

//dialog
var dialogOpen = false, lastFocus, dialog, okbutton, pagebackground;
function toggleDialog(sh) {
    dialog = document.querySelector('dialog');

    if (sh == 'show') {
        dialogOpen = true;
        // show the dialog  
        dialog.setAttribute('open', 'open');
    } else {
        dialogOpen = false;
        dialog.setAttribute('open', 'false');
    }
}

// List all ProcessInstances
(function () {
    for (id in diagramXMLSource) {
        model = diagramXMLSource[id];
        var ul = document.getElementById("linkList");
        var li = document.createElement("li");
        var a = document.createElement("a");
        var subName = model.name.substr(0, model.name.length - 5);
        li.appendChild(a);
        li.style.display = "inline";
        li.style.listStyle = "none";
        li.style.margin = "0";
        li.style.padding = "0";

        a.appendChild(document.createTextNode(subName + " (" + countIssues(model.name) + ")"));
        a.setAttribute("onclick", "selectModel('" + model.name.replace("\\", "\\\\") + "', null, null, 0 )");
        a.setAttribute("href", "#");
        a.style.padding = "0.4em";
        a.style.border = "1px solid black";
        a.style.color = "black";
        a.style.borderRadius = "10px 10px 0 0";
        a.style.boxShadow = "0px 5px 10px white inset";
        if (countIssues(model.name) == 0)
            a.style.background = "rgba(0, 255, 0, 0.5)";
        else
            a.style.background = "rgba(255, 0, 0, 0.5)";
        a.setAttribute("class", "focusClass");
        a.setAttribute("id", model.name);
        ul.appendChild(li);
    }
})();

function setFocus(name) {
    document.getElementById(name).focus();
}

//reload model diagram
function selectModel(name, issue_id, path_nr, func) {
    for (id in diagramXMLSource) {
        if (diagramXMLSource[id].name == name) {
            if (func == 0) {
                viewer.reload(diagramXMLSource[id]);
            } else if (func == 1) {
                viewer.reloadMarkPath(diagramXMLSource[id], issue_id, path_nr);
            } else if (func == 2) {
                viewer.reloadMarkElement(diagramXMLSource[id], issue_id);
            }
        }
    }
}
viewer = initDiagram(diagramXMLSource[0], 0, null);
