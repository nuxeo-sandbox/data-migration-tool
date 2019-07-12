let count = 0;
var headIds = db.default.find(
	{ $and: 
		[
			{"ecm:isProxy":null}, 
			{"ecm:isVersion":null},
			{"ecm:lifeCycleState": {$ne: "deleted"}},
			{"dc:modified": { // 1.5 Tage (from now)
				$gt: new Date(ISODate().getTime() - 1000 * 60 * 60 * 24 * 1.2)
			}}
		]
	},
	{
		"_id" :0,
		"ecm:id": 1,
		"ecm:racl": 1,
	}
).sort( { "ecm:id": 1 } ).skip(count).noCursorTimeout();

headIds.forEach(function(doc) {
		print (doc["ecm:id"]);
		print (doc["ecm:racl"]);
		count++;
		print ("count: " + count + " date: " + new Date());
		
		let headId = doc["ecm:id"];
		let headAcl = doc["ecm:racl"];
		
		let versionIds = db.default.find(
			{ $and: 
				[
					{"ecm:isVersion":true},
					{"ecm:versionSeriesId": headId}
				]
			},
			{
				"_id" :1,
				"ecm:id": 1,
				"ecm:racl": 1,
				"ecm:versionLabel": 1
			}
		);
		versionIds.forEach(function(versionDoc) {
				let versionAcl = versionDoc["ecm:racl"];
				let versionUuid = versionDoc["ecm:id"];
				let versionId = versionDoc["_id"];
				if (versionAcl.length === 0) {
					print ( "update version doc" + versionDoc["ecm:id"] + " in version: " + versionDoc["ecm:versionLabel"]);
					
					db.default.update(
					   { "_id" : versionId },
					   { $set: { "ecm:racl" : headAcl }},
					   {
						 upsert: false,
						 multi: false
					   }
					)
					
					
				}
			}
		);
	}
);

print ( "finished - count: " + count);
