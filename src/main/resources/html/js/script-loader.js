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

function loadJs(js) {
    js.forEach(scriptSrc => {
        let script = document.createElement("script");
        script.src = scriptSrc;
        script.async = false; //script tags added by DOM API are async by default (╯°□°）╯︵ ┻━┻
        script.defer = true;
        document.body.appendChild(script);
    });
}

let dataFiles = generateJsDataArray();
dataFiles.push('externalReports/reportPaths.js');
loadJs(dataFiles);
dataFiles = undefined;
if (!window.BpmnJS) {
    loadJs([
        //bootstrap with dependencies
        "js/jquery-3.5.1.min.js", "js/bootstrap.bundle.min.js",
        //bpmn-js viewer
        "js/bpmn-navigated-viewer.js",
        //application
        "js/download.js", "js/bpmn.io.viewer.app.js"]);
}