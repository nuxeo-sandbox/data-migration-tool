'use strict';
const Nuxeo               = require('nuxeo');
const http                = require('http');
const https                = require('https');
const fs                  = require('fs');
const url                 = require('url');

//
// documenation for the java script client available here:
// https://doc.nuxeo.com/nxdoc/javascript-client/
//

function usage () {
    console.log('Usage: ' +
        'node Query.js ' +
        '-stage <stage> -dump ' +
        '-user <user> -pw <pw> -q ' +
        '-userNameFilter <userName> ' +
        '-copyToStage <targetStage>'
    )
};

function getBaseUrl(stage) {
    let baseUrl = '';
    switch (stage) {
      case "local2":
        baseUrl = 'http://localhost:9080/nuxeo/'
        break; 
      case "local":
        baseUrl = 'http://localhost:8080/nuxeo/'
        break; 
      default:
        throw new Error('Unknown stage! ' + stage)
    }
    return baseUrl;
}

// console.log('Nuxeo', nuxeo);

// defaults
let stage = 'dev';
let dump = false;
let user = '';
let quiet = false;
let pw = '';
let copyToStage = '';
let userNameFilter = '';

// read command line:
process.argv.forEach((val, index, array) => {
  // console.log(index + ': ' + val);
  if ( val === '-userNameFilter' ) {
    userNameFilter =  process.argv[index +1];
  }
  if ( val == '-stage' ) {
      stage = process.argv[index +1];
  }
  if ( val == '-copyToStage' ) {
      copyToStage = process.argv[index +1];
  }
  if ( val == '-user' ) {
      user = process.argv[index +1];
  }
  if ( val == '-pw' ) {
      pw = process.argv[index +1];
  }
  if ( val == '-dump' ) {
    dump = true;
  }
  if ( val == '-q' ) {
        quiet = true;
  }
});

if (!quiet) {
    usage();
}

let baseUrl = getBaseUrl(stage);

if ( user === '' ) {
   throw new Error('Unknown user! ')
}
if ( pw === '' ) {
   throw new Error('Missing pw! ')
}

var nuxeo = new Nuxeo({
  baseURL: baseUrl,
  auth: {
    method: 'basic',
    username: user,
    password: pw
  },
});

var nuxeoTarget = new Nuxeo({
  baseURL: getBaseUrl(copyToStage),
  auth: {
    method: 'basic',
    username: user,
    password: pw
  },
});

if (!quiet) {
console.log('baseUrl: ', baseUrl);
console.log('stage: ', stage);
console.log('copyToStage: ', copyToStage);
console.log('user: ', user);
}

function createGroup(group) {
	return nuxeoTarget.groups().create(group).then(response => {
		console.log('group created: ' + group.id , JSON.stringify(group, null, 4));
	})
	.catch(error => {
		console.log('failed to create group: ' + group.id + ' => ' + error);
	});
}
function updateGroup(group) {
	return nuxeoTarget.groups().update(group).then(response => {
		console.log('group updated: ' + group.id , JSON.stringify(group, null, 4));
	})
	.catch(error => {
		console.log('failed to update group: ' + group.id + ' => ' + error);
	});
}

function handleGroupSearchResult (response) {
	if (!quiet) {
		console.log('copy search results to: ', copyToStage);
	}
	response.entries.forEach(group => {
		console.log('current group: ' + group.id);
		if (dump) {
			console.log('current group: ', JSON.stringify(group, null, 4));
		}
		// check if user is in target instance
		nuxeoTarget.groups().fetch(group.id)
		.then( targetGroup => {
			if (dump) {
				console.log('found target group: ', JSON.stringify(targetGroup, null, 4));
			}
			console.log('found target group: ', targetGroup.id);
			// return updateUser(nxUser);
		})
		.catch( error => {
			console.log('failed to fetch group: ' + group.id + ' ' + error);
			return createGroup(group);
		});
	});

}

let queryPath = 'group/search';
if (!quiet) {
    console.log('queryPath: ', queryPath);
}
let userQuery = "*"
let queryParameters = {"q":userQuery};

// get list of users
nuxeo.request(queryPath)
.queryParams(queryParameters)
.get()
.then(response => {
	if (dump) {
		console.log('response: ', JSON.stringify(response, null, 4));
	} 
	if (response.hasError) {
		throw new Error(response.errorMessage);
	} else {
		if (!quiet) {
			console.log ('resultsCount: ', response.resultsCount);
			console.log ('pageCount: ', response.pageCount);
			console.log ('currentPageSize: ', response.currentPageSize);
		}
		handleGroupSearchResult(response);
	}
})
.catch(error => {
	console.log('error',error);
});
                        


