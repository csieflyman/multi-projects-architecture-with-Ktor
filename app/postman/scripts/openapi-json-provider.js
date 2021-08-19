/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

const bent = require('bent');
const fs = require('fs');

const projectName = process.argv[2];
const schemaUrl = process.argv[3];
const username = process.argv[4];
const password = process.argv[5];

console.log(`projectName = ${projectName}`)
console.log(`schemaUrl = ${schemaUrl}`)
console.log(`username = ${username}`)

downloadJson().then(() => console.log("download success"));

async function downloadJson() {
    let headers = {}
    if (username && password) {
        headers = {
            'Authorization': `Basic ${new Buffer(`${username}:${password}`).toString('base64')}==`
        }
    }

    const getJson = bent('GET', 'json', headers, schemaUrl);
    let response = await getJson()
    fs.writeFileSync(`postman/${projectName}/${projectName}-openapi.json`, JSON.stringify(response));
    return response
}