document.body.onload = async () => {
    for (let scriptSrc of [
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
    ]) {
        let script = document.createElement("script");
        script.src = scriptSrc;
        // script.async = true;
        document.body.appendChild(script);
        //TODO finding a more elegant way to prevent random JS script loading issues
        await sleep(1);
    }
}

function sleep(milliseconds) {
    return new Promise(resolve => setTimeout(resolve, milliseconds));
}