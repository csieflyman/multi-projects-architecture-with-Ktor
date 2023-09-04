/*
 * Copyright (c) 2021. fanpoll All rights reserved.
 */

const fs = require('fs')
const newman = require('newman');
//require('newman-reporter-htmlextra');
const async = require('async');

const projectName = process.argv[2];
const envName = process.argv[3];
const folderName = process.argv[4];
console.log(`project : ${projectName}`);
console.log(`env : ${envName}`);
console.log(`folder: ${folderName ? folderName : ''}`);

if (!envName) {
    throw '[ERROR] env name is required';
}
const envFilePath = `projects/${projectName}/environment/${envName}.json`;
if (!fs.existsSync(envFilePath)) {
    throw `[ERROR] ${envFilePath} is not exist.`;
}

const collectionFilePath = `projects/${projectName}/${projectName}-collection.json`;
if (!fs.existsSync(collectionFilePath)) {
    throw `[ERROR] ${collectionFilePath} is not exist.`;
}
collection = JSON.parse(fs.readFileSync(collectionFilePath, {encoding: 'utf8'}));

const htmlextraDefaultOptions = {
    //export: "",
    // template: './template.hbs'
    // logs: true,
    // showOnlyFails: true,
    // noSyntaxHighlighting: true,
    // testPaging: true,
    browserTitle: `${projectName} Newman report`,
    title: `${projectName} Newman report`,
    // titleSize: 4,
    // omitHeaders: true,
    // skipHeaders: "Authorization",
    // hideRequestBody: ["Login"],
    // hideResponseBody: ["Auth Request"],
    showEnvironmentData: true,
    skipEnvironmentVars: ["API_KEY"],
    showGlobalData: true,
    skipGlobalVars: ["API_TOKEN"],
    // skipSensitiveData: true,
    // showMarkdownLinks: true,
    showFolderDescription: true,
    timezone: "Asia/Taipei"
}

if (folderName) {
    runSpecifiedFolder(folderName);
} else {
    runAllFoldersAndSuitesInSequence();
}

function runSpecifiedFolder(folderName) {
    console.log(`start to run folder ${folderName}...`);
    htmlextraDefaultOptions.export = `projects/${projectName}/report/folder/${folderName}-report.html`
    newman.run({
        globals: `projects/${projectName}/globals.json`,
        environment: envFilePath,
        collection: collectionFilePath,
        folder: folderName,
        iterationData: `projects/${projectName}/data/folder/${folderName}.json`,
        reporters: ['htmlextra'],
        reporter: {
            htmlextra: htmlextraDefaultOptions
        }
    }, function (err) {
        if (err) {
            console.error(`[ERROR] run folder ${folderName} failure`);
            throw err;
        }
    });
    console.log(`finish running folder ${folderName}`);
}

function runAllFoldersAndSuitesInSequence() {
    async.series([runAllFoldersInParallel, runAllSuitesInParallel], function (err, results) {
        if (err) {
            throw err;
        }
    });
}

function runAllFoldersInParallel(next) {
    if(!fs.existsSync(`projects/${projectName}/data/folder`))
        return
    console.log('==================== Run Folders Begin ====================');
    const runners = getAllFoldersRunners();
    async.parallel(runners, function (err, results) {
        if (err) {
            console.error('run folders failure!');
            throw err;
        }
        console.log('==================== Run Folders End ====================');
        next(err);
    });
}

function runAllSuitesInParallel(next) {
    if(!fs.existsSync(`projects/${projectName}/data/suite`))
        return
    console.log('==================== Run Suites Begin ====================');
    const runners = getAllSuitesRunners();
    async.parallel(runners, function (err, results) {
        if (err) {
            console.error('run suites failure!');
            throw err;
        }
        console.log('==================== Run Suites End ====================');
        next(err);
    });
}

function getAllFoldersRunners() {
    return fs.readdirSync(`projects/${projectName}/data/folder`).map(fileName => {
        let folderName = fileName.replace('.json', '');
        htmlextraDefaultOptions.export = `projects/${projectName}/report/folder/${folderName}-report.html`
        return function (finish) {
            console.log(`========== Folder ${folderName} Begin ==========`);
            newman.run({
                globals: `projects/${projectName}/globals.json`,
                environment: envFilePath,
                collection: collectionFilePath,
                folder: folderName,
                iterationData: `projects/${projectName}/data/folder/${folderName}.json`,
                reporters: ['htmlextra'],
                reporter: {
                    htmlextra: htmlextraDefaultOptions
                }
            }, function (err) {
                if (err) {
                    console.error(`[ERROR] run folder ${folderName} failure`);
                }
                finish(err, folderName);
            });
            console.log(`========== Folder ${folderName} End ==========`);
        }
    });
}

function getAllSuitesRunners() {
    return fs.readdirSync(`projects/${projectName}/data/suite`).map(fileName => {
        let suiteName = fileName.replace('.json', '');
        htmlextraDefaultOptions.export = `projects/${projectName}/report/suite/${suiteName}-report.html`
        return function (finish) {
            console.log(`========== Suite ${suiteName} Begin ==========`);
            newman.run({
                globals: `projects/${projectName}/globals.json`,
                environment: envFilePath,
                collection: collectionFilePath,
                iterationData: `projects/${projectName}/data/suite/${fileName}`,
                reporters: ['htmlextra'],
                reporter: {
                    htmlextra: htmlextraDefaultOptions
                }
            }, function (err) {
                if (err) {
                    console.error(`[ERROR] run suite ${suiteName} failure`);
                }
                finish(err, suiteName);
            });
            console.log(`========== Suite ${suiteName} End ==========`);
        }
    });
}