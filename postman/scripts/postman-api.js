/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

// Upload Collection To Postman Server
module.exports = {
    uploadAll: uploadAll,
    uploadEnvironments: uploadEnvironments,
    uploadCollection: uploadCollection
//    findCollectionByName: findCollectionByName,
//    getCollection: getCollection,
//    createCollection: createCollection,
//    updateCollection: updateCollection,
//    findEnvironmentByName: findEnvironmentByName,
//    getEnvironment: getEnvironment,
//    createEnvironment: createEnvironment,
//    updateEnvironment: updateEnvironment
};

const fs = require('fs');
const request = require('request');
const postmanApiUrl = 'https://api.getpostman.com';
const baseRequest = request.defaults({
    headers: {'X-Api-Key': process.env["X-Api-Key"]}
});

const funName = process.argv[2];
const projectName = process.argv[3];

switch (funName) {
    case 'uploadAll':
        uploadAll();
        break;
    case 'uploadEnvironments':
        uploadEnvironments();
        break;
    case 'uploadCollection':
        uploadCollection();
        break;
    default:
        throw new Error("undefined function: " + funName);
}

function uploadAll() {
    uploadEnvironments();
    uploadCollection();
}

function uploadEnvironments() {
    fs.readdirSync(`postman/${projectName}/environment`).forEach(fileName => {
        let envJson = JSON.parse(
            fs.readFileSync(`postman/${projectName}/environment/${fileName}`, {encoding: 'utf8'})
        );
        uploadObj(envJson.environment.name, envJson, findEnvironmentByName, createEnvironment, updateEnvironment);
    });
}

function uploadCollection() {
    let collectionJson = JSON.parse(
        fs.readFileSync(`postman/${projectName}/${projectName}-collection.json`, {encoding: 'utf8'})
    );
    uploadObj(collectionJson.info.name, {collection: collectionJson}, findCollectionByName, createCollection, updateCollection);
}

function getCollection(collectionUid) {
    return sendGetRequest('collections', collectionUid);
}

function createCollection(collection) {
    delete collection.collection.info.version; // no versioning
    return sendPostRequest('collections', collection);
}

function updateCollection(collectionUid, collection) {
    delete collection.collection.info.version; // no versioning
    return sendPutRequest('collections', collectionUid, collection);
}

function findCollectionByName(collectionName) {
    return findObjByName('collections', collectionName);
}

function getEnvironment(environmentUid) {
    return sendGetRequest('environments', environmentUid);
}

function createEnvironment(environment) {
    return sendPostRequest('environments', environment);
}

function updateEnvironment(environmentUid, environment) {
    return sendPutRequest('environments', environmentUid, environment);
}

function findEnvironmentByName(environmentName) {
    return findObjByName('environments', environmentName);
}

function sendGetRequest(endpoint, uid) {
    return new Promise((resolve, reject) => {
        const url = `${postmanApiUrl}/${endpoint}/${uid}`;
        console.log("GET " + url);
        baseRequest.get({url: url, json: true}, function (err, res, body) {
            if (err) {
                console.error(`GET ${url} failure`)
                reject();
            }
            console.log(body);
            resolve(body);
        });
    });
}

function sendPostRequest(endpoint, obj) {
    return new Promise((resolve, reject) => {
        const url = `${postmanApiUrl}/${endpoint}`
        console.log("POST " + url);
        baseRequest.post({url: url, body: obj, json: true}, function (err, res, body) {
            if (err) {
                console.error(`POST ${url} failure`)
                reject();
            }
            console.log(body);
            resolve(body);
        });
    });
}

function sendPutRequest(endpoint, uid, obj) {
    return new Promise((resolve, reject) => {
        const url = `${postmanApiUrl}/${endpoint}/${uid}`;
        console.log("PUT " + url);
        baseRequest.put({url: url, body: obj, json: true}, function (err, res, body) {
            if (err) {
                console.error(`PUT ${url} failure`)
                reject();
            }
            console.log(body);
            resolve(body);
        });
    });
}

function findObjByName(endpoint, objName) {
    return new Promise((resolve, reject) => {
        const url = `${postmanApiUrl}/${endpoint}`;
        console.log("GET " + url);
        baseRequest.get({url: url, json: true}, function (err, res, body) {
            if (err) {
                console.error(`GET ${url} failure`)
                reject();
            }
            //console.log(body);
            let objRes = body[endpoint].find(obj => obj.name === objName);
            //console.log(objRes);
            resolve(objRes);
        });
    });
}

function uploadObj(objName, obj, findObjByNameFunction, createObjFunction, updateObjFunction) {
    findObjByNameFunction(objName)
        .then((objRes) => {
            if (objRes) {
                updateObjFunction(objRes.uid, obj);
            } else {
                createObjFunction(obj);
            }
        })
        .catch(() => console.error(`upload ${objName} To Postman Server failure`));
}