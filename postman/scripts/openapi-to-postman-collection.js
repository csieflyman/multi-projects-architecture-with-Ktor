/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

const fs = require('fs')
const postmanConverter = require('openapi-to-postmanv2');
//const Event = require('postman-collection').Event
const {v4: uuidv4} = require('uuid');

const funName = process.argv[2];
const projectName = process.argv[3];

switch (funName) {
    case 'convert':
        convert();
        break;
    case 'downloadThenConvert':
        require("./openapi-json-provider").downloadJson().then(openapiJson => convert(openapiJson));
        break;
    default:
        throw new Error("undefined function: " + funName);
}

async function convert(json) {
    let input = json ? {type: 'json', data: json} : {
        type: 'file',
        data: `postman/${projectName}/${projectName}-openapi.json`
    };
    let options = {
        requestNameSource: "Fallback",
        folderStrategy: "Tags"
    }
    let collection = await postmanConverter.convert(input, options, (err, conversionResult) => {
        if (!conversionResult.result) {
            throw err
        } else {
            console.log("openapiToPostmanCollection success");
            return conversionResult.output[0].data;
        }
    });
    manipulateCollection(collection);
}

function manipulateCollection(collection) {
    manipulateRequests(collection);
    addMockRequestAtFirstPosition(collection);
    console.log("manipulateCollection success");
    fs.writeFileSync(`postman/${projectName}/${projectName}-collection.json`, JSON.stringify(collection));
}

function manipulateRequests(collection) {
    collection.item.forEach(folder => {
        if (folder.item) {
            folder.item.forEach(function (requestObj) {
                setRequestNameAndDescription(requestObj);
                setRequest(requestObj.request);
                setResponse(requestObj.response);
                addEvent(requestObj.event);
            });
        }
    });
}

/**
 openapi-to-postman SchemaUtils.js =>
 case 'fallback' : {
        // operationId is usually camelcase or snake case
        reqName = operation.summary || utils.insertSpacesInName(operation.operationId) || reqUrl;
        break;
      }
 */
function setRequestNameAndDescription(request) {
    let index = request.name.indexOf("=>");
    if (index !== -1) {
        let requestName = request.name
        request.name = requestName.substr(0, index).trim();
        request.description = requestName.substr(index + 2, requestName.length).trim();
    }
    request.request.name = request.name;
    request.request.description = request.description;
}

const ignoreHeaders = new Set(["Content-Type"]);

function setRequest(request) {
    if (request.body) {
        request.body = {mode: "raw", raw: "{{_requestBody}}"};
    }

    if (request.auth) {
        if (request.auth.apikey) {
            request.auth.apikey.find(it => it.key === "value").value = "{{X-API-KEY}}";
        }
    }

    // postman has at most one auth (see openapi-to-postman SchemaUtils.js getAuthHelper function).
    // we add session id header here
    if (request.description.indexOf("user") !== -1) {
        if (projectName === 'club') {
            request.header.push({
                key: "sid",
                value: "{{sid}}"
            });
        }
    }

    if (request.header) {
        request.header.filter(it => !ignoreHeaders.has(it.key)).forEach(it => it.value = `{{${it.key}}}`)
    }

    if (request.url.query) {
        request.url.query.forEach(it => it.value = `{{${it.key}}}`)
    }

    if (request.url.variable) {
        request.url.variable.forEach(it => it.value = `{{${it.key}}}`)
    }
}

function setResponse(response) {

}

function addEvent(event) {
    event.push(
        {
            listen: "test",
            script: {
                type: "text/javascript",
                exec: [
                    "let script = pm.iterationData.get(\"_test\");eval(script);postman.setNextRequest(null);"
                ]
            }
        }
    );
}

function addMockRequestAtFirstPosition(collection) {
    collection.item.forEach(folder => {
        if (folder.item) {
            folder.item.splice(0, 0, {
                id: uuidv4(),
                name: 'Mock Request',
                request: {
                    url: {
                        host: ['{{baseUrl}}/public/blank_page.html']
                    },
                    method: 'GET'
                },
                event: [
                    {
                        listen: "prerequest",
                        script: {
                            type: "text/javascript",
                            exec: [
                                "postman.setNextRequest(pm.iterationData.get(\"_requestName\"));"
                            ]
                        }
                    }
                ]
            });
        }
    });
}



