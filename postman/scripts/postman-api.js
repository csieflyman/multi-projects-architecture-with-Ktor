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
const axios = require('axios');
axios.defaults.baseURL = 'https://api.getpostman.com'
axios.defaults.headers.common['X-Api-Key'] = process.env["X-Api-Key"]

const funName = process.argv[2];
const projectName = process.argv[3];
console.log(`funName = ${funName}`);
console.log(`projectName = ${projectName}`);

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
    fs.readdirSync(`projects/${projectName}/environment`).forEach(fileName => {
        let envJson = JSON.parse(
            fs.readFileSync(`projects/${projectName}/environment/${fileName}`, {encoding: 'utf8'})
        );
        uploadObj(envJson.environment.name, envJson, findEnvironmentByName, createEnvironment, updateEnvironment);
    });
}

function uploadCollection() {
    let collectionJson = JSON.parse(
        fs.readFileSync(`projects/${projectName}/${projectName}-collection.json`, {encoding: 'utf8'})
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

async function sendGetRequest(endpoint, uid) {
    const url = `/${endpoint}/${uid}`
    console.log("GET " + url);
    return axios.get(url)
}

async function sendPostRequest(endpoint, obj) {
    const url = `/${endpoint}`
    console.log("POST " + url);
    return axios.post(url, obj)
}

async function sendPutRequest(endpoint, uid, obj) {
    const url = `/${endpoint}/${uid}`
    console.log("PUT " + url);
    return axios.put(url, obj)
}

async function findObjByName(endpoint, objName) {
    const url = `/${endpoint}`
    console.log("GET " + url);
    return await axios.get(url)
        .then((res) => {
            return res.data[endpoint].find(obj => obj.name === objName)
        })
}

function uploadObj(objName, obj, findObjByNameFunction, createObjFunction, updateObjFunction) {
    findObjByNameFunction(objName)
        .then((objRes) => {
            if (objRes) {
                updateObjFunction(objRes.uid, obj);
            } else {
                createObjFunction(obj);
            }
            console.log(objRes)
        })
        .catch((err) => {
            console.error(`upload ${objName} To Postman Server failure`);
            console.error(err);
        });
}