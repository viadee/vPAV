let scriptFiles = [
    //generated data
    "data/checkers.js", "data/bpmn_model.js", "data/bpmn_validation.js", "data/bpmn_validation_success.js",
    "data/infoPOM.js", "data/issue_severity.js", "data/ignoredIssues.js",
    "data/processVariables.js",
    //bootstrap with dependencies
    "js/jquery-3.5.1.min.js", "js/bootstrap.bundle.min.js",
    //bpmn-js viewer
    "js/bpmn-navigated-viewer.js",
    //application
    "js/download.js", "data/properties.js", "js/bpmn.io.viewer.app.js"

];

let scriptFragment = new DocumentFragment();
for (let scriptSrc of scriptFiles) {
    let script = document.createElement("script");
    script.src = scriptSrc;
    scriptFragment.appendChild(script);
}
document.body.appendChild(scriptFragment);