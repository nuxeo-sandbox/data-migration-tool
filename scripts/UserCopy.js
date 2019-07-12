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

function checkEmail(nxUser) {
	if ( nxUser.hasOwnProperty('properties') ) {
		if(nxUser.properties.hasOwnProperty('email')){
			if (
				nxUser.properties.email !== null 
				&& nxUser.properties.email.indexOf('foobar.com') > -1 
			) {
				let old = nxUser.properties.email
				nxUser.properties.email = old.replace('foobar.com','nuxeo.com')
			} else {
				console.log('no match for substring');
			}
		} else {
			console.log('no property email');
		}
	} else {
		console.log('no property properties');
	}
}

function createUser(nxUser) {
	checkEmail(nxUser);
	return nuxeoTarget.users().create(nxUser).then(response => {
		console.log('user created: ' + nxUser.id , JSON.stringify(nxUser, null, 4));
	})
	.catch(error => {
		console.log('failed to create user: ' + nxUser.id + ' => ' + error);
	});
}
function updateUser(nxUser) {
	checkEmail(nxUser);
	return nuxeoTarget.users().update(nxUser).then(response => {
		console.log('user updated: ' + nxUser.id , JSON.stringify(nxUser, null, 4));
	})
	.catch(error => {
		console.log('failed to update user: ' + nxUser.id + ' => ' + error);
	});
}

function handleUserSearchResult (response) {
	if (!quiet) {
		console.log('copy search results to: ', copyToStage);
	}
	response.entries.forEach(nxUser => {
		console.log('current nxUser: ' + nxUser.id);
		if (user.id !== user) {
			if (dump) {
				console.log('current nxUser: ', JSON.stringify(nxUser, null, 4));
			}
			// check if user is in target instance
			nuxeoTarget.users().fetch(nxUser.id)
			.then( targetUser => {
				if (dump) {
					console.log('found target user: ', JSON.stringify(targetUser, null, 4));
				}
				console.log('found target user: ', targetUser.id);
				return updateUser(nxUser);
			})
			.catch( error => {
				console.log('failed to fetch user: ' + nxUser.id + ' ' + error);
				return createUser(nxUser);
			});
		} else {
			console.log('skip user: ' + user);
		}
	});

}

let queryPath = 'user/search';
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
		handleUserSearchResult(response);
	}
})
.catch(error => {
	console.log('error',error);
});
                        


