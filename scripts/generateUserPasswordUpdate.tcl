#!/usr/bin/tclsh
#exec tclsh "$0" "$@"

## user and pws generated with:
## select username,password from users limit 100;"


set lines "
 Administrator                  | {SSHA}/4p8UmwfrsXXBtzN6zbs7veyNrftC1TFZl0dpg==
"

set lLines [split $lines "\n"]

set bSql 0
set bMongo 1

foreach sLine $lLines {
	if { [string trim $sLine] == "" } {
		continue
	}
	set lParts [split $sLine "|"]
	set user [string trim [lindex $lParts 0]]
	set pw [string trim [lindex $lParts 1]]
	if { $bSql } {
		puts "update \"users\" set \"password\" = '$pw' where \"username\" = '$user';";
	} elseif { $bMongo } {
		puts "db.userDirectory.update( { username: \"$user\"}, {\$set: {\"password\": \"$pw\" }})";
	}
}
