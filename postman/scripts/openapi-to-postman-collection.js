/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

const fs = require('fs')
const postmanConverter = require('openapi-to-postmanv2');
//const Event = require('postman-collection').Event
const {v4: uuidv4} = require('uuid');

const projectName = process.argv[2];
console.log(`projectName = ${projectName}`)

convert().then(() => console.log("convert success"));

async function convert(json) {
    let input = json ? {type: 'json', data: json} : {
        type: 'file',
        data: `projects/${projectName}/${projectName}-openapi.json`
    };
    let options = {
        requestNameSource: "Fallback",
        folderStrategy: "Tags"
    }
    let collection = {}
    postmanConverter.convert(input, options, (err, conversionResult) => {
        if (!conversionResult.result) {
            throw err
        } else {
            console.log("openapiToPostmanCollection success");
            collection = conversionResult.output[0].data;
        }
    });
    manipulateCollection(collection);
}

function manipulateCollection(collection) {
    addCollectionEvent(collection);
    manipulateRequests(collection);
    addDummyRequestAtFirstPosition(collection);
    console.log("manipulateCollection success");
    fs.writeFileSync(`projects/${projectName}/${projectName}-collection.json`, JSON.stringify(collection));
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

function setRequest(request) {
    if (request.body) {
        request.body = {mode: "raw", raw: "{{_requestBody}}"};
    }

    if (request.auth) {
        if (request.auth.apikey) {
            request.auth.apikey.find(it => it.key === "value").value = "{{X-API-KEY}}";
        }
    }

    const ignoreHeaders = new Set(["Content-Type"]);
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
                    "let script = pm.iterationData.get('_test');eval(script);postman.setNextRequest(null);"
                ]
            }
        }
    );
}

function addDummyRequestAtFirstPosition(collection) {
    collection.item.forEach(folder => {
        if (folder.item) {
            folder.item.splice(0, 0, {
                id: uuidv4(),
                name: 'Dummy Request',
                request: {
                    url: {
                        host: ['{{dummyRequestUrl}}']
                    },
                    method: 'GET'
                },
                event: [
                    {
                        listen: "prerequest",
                        script: {
                            type: "text/javascript",
                            exec: [
                                "postman.setNextRequest(pm.iterationData.get('_requestName'));"
                            ]
                        }
                    }
                ]
            });
        }
    });
}

function addCollectionEvent(collection) {
    collection.event.push(
        {
            listen: "prerequest",
            script: {
                type: "text/javascript",
                exec: [
                    "pm.request.url.query.filter(it => it.value.startsWith('{{') && it.value.endsWith('}}') && !pm.iterationData.has(it.value.substring(2, it.value.length -2)) && !pm.variables.has(it.value.substring(2, it.value.length -2))).forEach(it => it.disabled = true);",
                    "pm.request.url.variables.filter(it => it.value.startsWith('{{') && it.value.endsWith('}}') && !pm.iterationData.has(it.value.substring(2, it.value.length -2)) && !pm.variables.has(it.value.substring(2, it.value.length -2))).forEach(it => it.disabled = true);",
                    "pm.request.headers.filter(it => it.value.startsWith('{{') && it.value.endsWith('}}') && !pm.iterationData.has(it.value.substring(2, it.value.length -2)) && !pm.variables.has(it.value.substring(2, it.value.length -2))).forEach(it => it.disabled = true);"
                ]
            }
        }
    );
}



