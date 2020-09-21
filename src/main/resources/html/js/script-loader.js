function generateScriptSourcesArray(sourcePath = 'data/') {
    //generated model data by vPAV
    const srcFiles = ["checkers.js",
        "bpmn_model.js",
        "bpmn_validation.js",
        "bpmn_validation_success.js",
        "infoPOM.js",
        "ignoredIssues.js",
        "processVariables.js",
        "properties.js",
        "summary.js"
    ]
    return srcFiles.map(file => sourcePath + file);
}

function unloadDataScriptTags() {
    for (let script of Array.from(document.getElementsByClassName("data-script"))) {
        document.body.removeChild(script);
    }
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

function createScriptTags(scriptSources, isDataScript = false) {
    return scriptSources.map(scriptSource => {
        const script = document.createElement("script");
        script.src = scriptSource;
        script.async = false; //script tags added by DOM API are async by default (╯°□°）╯︵ ┻━┻
        if (isDataScript) {
            script.className = "data-script";
        }
        return script;
    });
}

function loadDomElements(scriptNodes) {
    return new Promise(function (resolve, reject) {
        const fragment = document.createDocumentFragment();
        const lastScriptTag = scriptNodes[scriptNodes.length - 1];
        lastScriptTag.onload = function () {
            resolve()
        }
        lastScriptTag.onerror = function () {
            reject(`Error loading script: ${this.src}`);
        }
        scriptNodes.forEach(script => {
            fragment.appendChild(script);
        });
        try {
            document.body.appendChild(fragment);
        } catch (error) {
            reject("Error loading fragment: " + error);
        }
    });
}

async function loadLogicJs() {
    if (!window.BpmnJS) {
        const executionScripts = [
            //bootstrap with dependencies
            "js/jquery-3.5.1.min.js",
            "js/bootstrap.bundle.min.js",
            //bpmn-js viewer
            "js/bpmn-navigated-viewer.js",
            //AdminLTE
            "js/adminlte.js",
            "js/jquery.knob.js",
            //bootsrap table
            "js/bootstrap-table.js",
            "js/jquery.treegrid.js",
            "js/bootstrap-table-treegrid.js",
            "js/bootstrap-table-export.js",
            "js/TableExport.js",
            "js/FileSaver.js",
            //miscellaneous
            "js/download.js",
            //application
            "js/bpmn.io.viewer.app.js"];
        const executionScriptsTags = createScriptTags(executionScripts, false);
        await loadDomElements(executionScriptsTags, false);
    }
}

async function loadDataJs() {
    try {
        await loadDomElements(createScriptTags(["externalReports/reportData.js"], true));
        projectNameToSummaryMap = new Map();
        projectNameToPathMap = new Map();
        for await (let path of reportData.reportsPaths) {
            await loadDomElements(createScriptTags([path + "properties.js"], true));
            await loadDomElements(createScriptTags([path + "summary.js"], true));
            projectNameToSummaryMap.set(properties.projectName, projectSummary);
            projectNameToPathMap.set(properties.projectName, path);
        }
        projectNamesSorted = Array.from(projectNameToPathMap.keys()).sort((a, b) => a.localeCompare(b));
    } catch (error) {
        console.warn(error);
    }
    await loadDomElements(createScriptTags(generateScriptSourcesArray(), true));
}

var documentBackup;
if (!documentBackup) {
    documentBackup = document.cloneNode(true);
}
var projectNameToSummaryMap;
var projectNamesSorted;
var projectNameToPathMap;
var projectSummaryFirstTimeLoaded = false;
(async () => {
    await loadDataJs();
    await loadLogicJs();
})();
