function generateJsDataArray(dataPath = 'data/') {
    //generated model data by vPAV
    let dataFiles = ["checkers.js",
        "bpmn_model.js",
        "bpmn_validation.js",
        "bpmn_validation_success.js",
        "infoPOM.js",
        "issue_severity.js",
        "ignoredIssues.js",
        "processVariables.js",
        "properties.js"]
    return dataFiles.map(file => dataPath + file);
}

function loadDomElements(scriptNodes, callback = null) {
    let fragment = document.createDocumentFragment();
    if (callback) {
        const lastScript = scriptNodes[scriptNodes.length - 1];
        lastScript.onload = callback;
    }
    scriptNodes.forEach(script => {
        fragment.appendChild(script);
    });
    document.body.appendChild(fragment);
}

function unloadDataScripts() {
    Array.from(document.getElementsByClassName("data-script")).forEach(script => {
        document.body.removeChild(script);
    });
    diagramXMLSource = undefined;
    elementsToMark = undefined;
    noIssuesElements = undefined;
    unlocatedCheckers = undefined;
    defaultCheckers = undefined;
    ignoredIssues = undefined;
    vPavVersion = undefined;
    viadee = undefined;
    vPavName = undefined;
    issueSeverity = undefined;
    proz_vars = undefined;
    processVariables = undefined;
    properties = undefined;
}

function createScriptTags(scriptSources, isData = false) {
    return scriptSources.map(scriptSource => {
        let script = document.createElement("script");
        script.src = scriptSource;
        if (isData) {
            script.className = "data-script";
        } else {
            script.async = false; //script tags added by DOM API are async by default (╯°□°）╯︵ ┻━┻
            script.defer = true;
        }
        return script;
    });
}

function loadLogicJs() {
    if (!window.BpmnJS) {
        const executionScripts = createScriptTags([
            //bootstrap with dependencies
            "js/jquery-3.5.1.min.js", "js/bootstrap.bundle.min.js",
            //bpmn-js viewer
            "js/bpmn-navigated-viewer.js",
            //application
            "js/download.js", "js/bpmn.io.viewer.app.js"], false);
        loadDomElements(executionScripts, false);
    }
}

function loadDataJs() {
    loadDomElements(createScriptTags(["externalReports/reportData.js"], true), () => {
        if (reportData.reportsPaths) {
            projectNames = [];
            reportData.reportsPaths.forEach(path => {
                loadDomElements(createScriptTags([path + "properties.js"], true),
                    () => {
                        projectNames.push(properties.projectName)
                    });
            });
        }
    });
    loadDomElements(createScriptTags(generateJsDataArray(), true));
}

var documentBackup;
if (!documentBackup) {
    documentBackup = document.cloneNode(true);
}
var projectNames;
loadDataJs();
loadLogicJs();