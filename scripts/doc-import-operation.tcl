#!/usr/bin/tclsh
#exec tclsh "$0" "$@"

proc usage {} {
	puts "Usage:"
	puts "tclsh doc-import-operation.tcl -sUser xx -sPw xx -sBaseUrl xx -id xx  -idFile <filename> -blocksize <blocksize> -offset <offset> -maxids <maxids>"
}

proc executeCommand { sCommand va } {

	puts "executeCommand: $sCommand"
	upvar $va aResult
	set sEval "exec -keepnewline -- $sCommand"
	puts "eval: $sEval"
	if { [catch {
		set aResult(sOutput) [eval $sEval]
	} sError ] } {
		puts "errorInfo: $::errorInfo"
		puts "error: $sError"
	}
	
	return 
	
}

## simple tool which uses curl to upload

set sUser Administrator
set sPw Administrator
set id ""
set idFile ""
set blocksize ""
set offset ""
set maxids ""
set docExchangePath ""
set tStartJob [clock clicks]
set sTempFileName "body-temp-$tStartJob.txt"
set sProcessedIdsFileName "import-processed-ids-$tStartJob.txt"
set hProcessedIdsFile [open "$sProcessedIdsFileName" "w"]

set sBaseUrl "http://localhost:9080/"

## parse command line arguments
set i 0
set iMax [llength $argv]
while { $i < $iMax } {
	set sName [lindex $argv $i]
	set iNext [expr $i +1]
	if { $iNext < $iMax } {
		set sValue [lindex $argv $iNext]
		if { [string first "-" $sName] == 0 } {
			set sName [string trimleft $sName "-"]
			if { ![info exists $sName] } {
				error "ERROR: $sName is unknown!"
			} else {
				puts "set $sName => $sValue"
				set $sName $sValue
			}
		}
	}
	incr i 2 
}


proc importDoc { id } {
	variable sUser
	variable sPw
	variable sBaseUrl
	variable sTempFileName
	variable tStartJob


	set sCurlCommand curl 
	append sCurlCommand { -s}
	append sCurlCommand { -X POST} 
	append sCurlCommand { -H "Content-Type: application/json"}
	append sCurlCommand { -H "X-NXproperties: *"}
	append sCurlCommand { -H "X-NXRepository: default"}
	append sCurlCommand { -H "X-NXVoidOperation: false"}
	append sCurlCommand { -H "Nuxeo-Transaction-Timeout: 10000"}
	append sCurlCommand { -u } $sUser {:} $sPw
	append sCurlCommand { -d @} $sTempFileName


	if { $id == "" } {
		puts "error: no parameter 'id'"
		usage
		return
	}


	set sDentry ""
	set hBodyFile [open "$sTempFileName" "w"]
	append sDentry "{" {  "params": } "{" "\"uuid\": \"" $id "\", \"path\": \"default-domain/workspaces/mh-test/test-no-thumb\" }" {, "input":"} $id {", "context": } "{}}" 
	puts $hBodyFile $sDentry
	close $hBodyFile


	## append sCurlCommand $sDentry
	append sCurlCommand { } $sBaseUrl {nuxeo/api/v1/automation/Document.ImportOperation} 

	executeCommand $sCurlCommand aRes
	if { [info exists aRes(sOutput)] } {
		puts "[clock format [clock seconds]]: result for id: $id \n$aRes(sOutput)"
		
		set hOutFile [open "output-$tStartJob.txt" "w"]
		puts $hOutFile $aRes(sOutput)
		close $hOutFile
		
	}	

}

proc importDocList { idList } {
	variable sUser
	variable sPw
	variable sBaseUrl
	variable sTempFileName
	variable tStartJob


	set sCurlCommand curl 
	append sCurlCommand { -s}
	append sCurlCommand { -X POST} 
	append sCurlCommand { -H "Content-Type: application/json"}
	append sCurlCommand { -H "X-NXproperties: *"}
	append sCurlCommand { -H "X-NXRepository: default"}
	append sCurlCommand { -H "X-NXVoidOperation: false"}
	append sCurlCommand { -H "Nuxeo-Transaction-Timeout: 10000"}
	append sCurlCommand { -u } $sUser {:} $sPw
	append sCurlCommand { -d @} $sTempFileName

	if { $idList == "" } {
		puts "error: no parameter 'idList'"
		usage
		return
	}

	set sDentry ""
	set hBodyFile [open "$sTempFileName" "w"]
	append sDentry "{" {  "params": } "{" "\"uuids\": \"" [join $idList ","] "\", \"path\": \"default-domain/workspaces/mh-test/test-no-thumb\" }" {, "input": } "{}" {, "context": } "{}}" 
	puts $hBodyFile $sDentry
	close $hBodyFile


	append sCurlCommand { } $sBaseUrl {nuxeo/api/v1/automation/Document.ImportOperation} 


	executeCommand $sCurlCommand aRes
	if { [info exists aRes(sOutput)] } {
		puts "[clock format [clock seconds]]: result last id: [lindex $idList end]: \n$aRes(sOutput)"
		
		set hOutFile [open "output-$tStartJob.txt" "w"]
		puts $hOutFile $aRes(sOutput)
		close $hOutFile
		
	}

}

proc checkForImportFile { id } {
	variable docExchangePath
	if { $docExchangePath == "" } {
		## docExchangePath is an optional argument
		## if not used assume always that the file is available
		return 1
	}
	
	set lIdParts [split $id "-"]
	set sFirstPart [lindex $lIdParts 0]
	for { set i 0 } { $i < 9 } { incr i 2 } {
		lappend lFolderParts [string range $sFirstPart $i [expr { $i + 1}]]
	}
	set zipExt ".zip"
	set sFilePath "$docExchangePath/[join $lFolderParts "/"]$id$zipExt"
	
	if { [file exists $sFilePath] } {
		return 1
	} else {
		puts "path not found: $sFilePath"
		return 0
	}
	
}

if { $id != "" } {
	if { [checkForImportFile $id] == 1 } {
		importDoc $id
		puts $hProcessedIdsFile $id
	} else {
		puts "id: $id import file does not exists => skip"
	}
} elseif { $idFile != "" } {
	set hIds [open $idFile "r"]
	set numIds 0
	set tStart [clock seconds]
	if  { $blocksize != "" } {
		set idList [list]
		set iLines 0
		while { ![eof $hIds] } {
			set currentId [string trim [gets $hIds]]
			incr iLines
			if { $offset != "" && $iLines < $offset } {
				continue
			}
			if { [checkForImportFile $id] != 1 } {
				puts "id: $id import file does not exists => skip"
				continue
			}
			puts $hProcessedIdsFile $currentId
			if { [string length $currentId] < 35 } {
				puts "invalid id: $currentId"
			} else {
				if { [llength $idList] >= $blocksize } {
					importDocList $idList
					set idList [list ]
					set tEnd [clock seconds]
					puts "diff: [expr { $tEnd -$tStart}] secs"
					set tStart $tEnd
					puts "numIds: $numIds - iLines: $iLines"
					if { $maxids != "" && $numIds > $maxids } {
						puts "break look because $numIds > $maxids"
						break
					}
				}
				lappend idList $currentId
				incr numIds
			}
		}
		## last part
		if { [llength $idList] > 0 } {
			importDocList $idList
			set tEnd [clock seconds]
			puts "diff: [expr { $tEnd -$tStart}] secs"
			puts "numIds: $numIds - iLines: $iLines"
		}
	} else {
		while { ![eof $hIds] } {
			set currentId [string trim [gets $hIds]]
			puts $hProcessedIdsFile $currentId
			if { [string length $currentId] < 35 } {
				puts "invalid id: $currentId"
			} else {
				importDoc $currentId
			}
		}
	}
	close $hIds
} else {
	puts "Parameter 'id' or 'idFile' need to be used!"
	exit
}

if { [file exists $sTempFileName] } {
	file delete $sTempFileName
}

close $hProcessedIdsFile

puts "finished!"
