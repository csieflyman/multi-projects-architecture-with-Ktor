/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

module.exports = {
    downloadJson: downloadJson
}

const bent = require('bent');
const fs = require('fs');

const funName = process.argv[2];
const projectName = process.argv[3];
const schemaUrl = process.argv[4];

switch (funName) {
    case 'downloadJson':
        downloadJson();
        break;
    default:
        throw new Error("undefined function: " + funName);
}

async function downloadJson() {
    let getJson = bent(schemaUrl, 'json');
    let response = await getJson();
    fs.writeFileSync(`postman/${projectName}/${projectName}-openapi.json`, JSON.stringify(response));
    return response
}