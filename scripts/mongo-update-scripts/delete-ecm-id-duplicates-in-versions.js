// Description
// -----------
// Find duplicate records with the same ecm:id
// and remove them.
var myCursor = db.default.find(
	{ $and: 
		[
			{"ecm:isVersion":true}, 
			{"ecm:lifeCycleState": {$ne: "deleted"}},
			{'dc:modified': {$lt : new Date('2019-02-03T00:00:00')}}
		]
	},
	{
		"_id" :1,
		"ecm:id": 1
	}
).sort( { "ecm:id" : 1 }).noCursorTimeout();
var latestId;
var latestEcmId;
var currentId;
var currentEcmId;
var idCounter = 0;
var duplicateCounter = 0;
myCursor.forEach(function(doc) {
	// print (doc["ecm:id"]);
	currentEcmId = doc["ecm:id"];
	currentId = doc["_id"];
	if ( idCounter > 0 && currentEcmId === latestEcmId ) {
		// to be sure check the the _id is different
		if ( currentId === latestId ) {
			new Error("Latest id == current id!!!");
			print ("Latest id == current id!!!");
			quit();
		}
		print("remove duplicate: " + currentEcmId )
		duplicateCounter++;
		// remove the duplicate:
		db.default.deleteOne( { "_id": currentId } );
	}
	latestId = currentId;
	latestEcmId = currentEcmId;
	idCounter++;
});
print ("removed " + duplicateCounter + " duplicates in " + idCounter + " ids.");
