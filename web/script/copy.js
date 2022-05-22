#!/usr/bin/env node

const argv = process.argv
if (argv.length <= 4) {
    console.log('Usage: node copy.js from to name')
    return
}
const fromParam = argv[2]
const toParam = argv[3]
const newName = argv[4]

console.log("Try copy " + fromParam + " to " + toParam + "\n\n")

var shelljs = require('shelljs');
var addCheckMark = require('./checkmark');
var path = require('path');

var cpy = path.join(__dirname, '../node_modules/cpy-cli/cli.js');

shelljs.exec("node " + cpy + ' ' + fromParam + ' ' + toParam + ' --rename=' + newName, addCheckMark.bind(null, callback));

function callback() {
  process.stdout.write(' Copied ' + fromParam + ' to the ' + toParam + ' directory\n\n');
}
