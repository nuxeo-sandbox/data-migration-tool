# data-migration-tool


Operations for exporting and importing document meta data
---------------------------------------------------------
The export and import operations are contained in the file `nuxeo-cli-new-core-1.0-SNAPSHOT.jar`.  
Each nuxeo instance are enhanced with these export and import operations after the jar is installed in `<nuxeo base folder>/nxserver/bundles/` and nuxeo is restared.

The operations are called:
* **Document.ExportOperation**:  
The operation creates a `<uuid>.zip` archive in the `doc-exchange` folder.  
This archive contains the meta information of the passed UUID, including all versions of the document.  
The passed UUID should be "version series id", not a version.  
The blob attachements of the document is only referenced by their digest, not included.  
    
* **Document.ImportOperation**:  
The operation reads an archive with file name `<uuid>.zip` from the doc-exchange folder.  
All documents within this archive are imported.  
All attached to the documents which are referenced by their digest are searched in the binary store of the target instance.  
This means the target instance needs all binaries which are also contained in the binary store of the source instance.  
This is usually done by having exactly the same binary store configured for the target instance as in the source instance.  
The imported documents are created with their UUID from the source instance.  

These operations are listed on the nuxeo instance after installation of the jar in the automation documentation of the instance, e.g.:
http://localhost:8080/nuxeo/site/automation/doc/
  
Additional scripts
------------------

* `scripts/doc-export-operation.tcl` for exporting documents from the source instance.
* `scripts/doc-import-operation.tcl` for importing documents to the target instance.
  * parameter path is a dummy and a left over from a path in the beginning, please ignore it.
* `scripts/generateUserPasswordUpdate.tcl` for generating the update statements to set the passwords for the copied users.
  

Sequence of steps for data migration
------------------------------------

1. Copy groups:  
`node GroupCopy.js -stage <source host identfier> -user <user> -pw <pw> -copyToStage <target host identifier>`
1. Copy users:  
`node UserCopy.js -stage <source host identfier> -user <user> -pw <pw> -copyToStage <target host identifier>`
1. Update and set users password:
   1. Export usernames with passwords from old instance:
      1. sql: `select username,password from users;`
   1. Import passwords into new instance:
      1. Generate the update statements with: `./generateUserPasswordUpdate.tcl` 
      1. Paste the output of this script into the mongo client of the target mongo db instance.
1. Export import directory
   1. Create the folder `doc-exchange` out of `<nuxeo_base>`.
      The folder needs to be anywhere else because nuxeo won't start if this 
	folder is too big. 
   1. Link the folder `<nuxeo_base>/doc-exchange` on the source instance.
   1. Link the folder `<nuxeo_base>/doc-exchange` on the target instance.
   1. Link both folders to the same physical folder.
   Tests showed that the folder should be on a fast drive, so a local folder.  
   Source and target instane should run on the same host so that the same folder can be used from source and target instance.  
1. Use the nuxeo operation `Document.ExportOperation` to export documents.
    Helpful scripts: `doc-export-operation.tcl`
    If no shared folder is used copy the exported zip files to the target `doc-exchange` folder of the target instance.
1. Use the nuxeo operation `Document.ImportOperation` to import documents.
   1. Helpful scripts: `doc-import-operation.tcl`
> For steps 5 and 6:  
> Its important that the directories are exported and imported before the documents.  
> To find the UUIDs for the directories use as SQL query:
> ```sql
> SELECT id from HIERARCHY h where PRIMARYTYPE in ('Workspace', 'Folder') and PARENTID <> '<UUID of the users workspaces parent folder> order by PRIMARYTYPE desc';
> ```
> sort descending to have folders after worspaces.
> For each file type to copy (XXX_file and YYY_file) export the ids with statements like:
> ```sql
> SELECT id from HIERARCHY where PRIMARYTYPE = 'XXX_file' and ISVERSION IS NULL
> ```
> Optionally enhanced by the id of a parent folder of the export should be done per folder:
> ```sql
> SELECT id from HIERARCHY where PRIMARYTYPE = 'XXX_file' and ISVERSION IS NULL and PARENTID = '<UUID of parent folder>';
> ```
> or both together:
> ```sql
> select id from hierarchy where PRIMARYTYPE in ('XXX_file', 'YYY-File') and ISVERSION IS NULL;
> ```

7. Re-iterate steps 5 and 6 to process changes since the start of the latest export/import.  
To find changed ids since last export (replace the date):
```sql
select h.id from hierarchy h join dublincore d on h.id = d.id where h.PRIMARYTYPE in ('XXX_file', 'YYY_File') and ISVERSION IS NULL and d.modified >= '2018-10-29'::date ;
```
This step has to be done multiple times until the amount of changed data can be processed one last time over a weekend.  

Fake Binary Manager
-------------------
It was intended for the case of performance issues with the binary store but was not necessary at the end so its not finished and does not work correctly.  

log4j.xml - Logging
-------------------
To enable detailed logging add the following to `<nuxeo base folder>/lib/log4j.xml`:
```xml
<!-- Export operation -->
<category name="org.nuxeo.migration.operation.document.export">
    <priority value="TRACE" />
</category>

<!-- Import operation -->
<category name="org.nuxeo.migratio.operation.document.importer">
    <priority value="TRACE" />
</category>

<!-- Fake Binary Manager -->
<category name="org.nuxeo.migratio.operation.document">
    <priority value="TRACE" />
</category>
```

Overall process per stage for LTS 2017 upgrade
----------------------------------------------

1. Use an own instance or remove one nuxeo instance out of the existing cluster.
If one instance of an existing cluster is used:  
Deactivate apache on this instance or bettern reconfigure it to another port.  
By doing this the f5 load balance will not distribute any more request to this nuxeo instance because the apache is no longer listing on the expected port.  
Also disable the redis configuration to prevent that this instance will communicate to the other cluster instances over redis.  
1. Upgrade this instance from LTS 2015 to LTS 2016 
	Disable elsastic search in the configuration file,
	we don't want to re-index for LTS 2016.
1. Setup a new elaticsearch cluster for LTS 2017 with elasticsearch 5.6.x
1. Upgrade the instance from LTS 2016 to LTS 2017
1. Do a re-index into the new elastic search instances.
1. Disable importer and do a re-index for elasticsearch by nxql since starting the new instance.
1. Upgrade other cluster instances to LTS 2017 and connect them to the new elastic search instance.
1. Re-Integrate the first migrated instance into the cluster.
1. Upgrade also the importer instance to LTS 2017 and the new elasticsearch cluster.
1. Re-enable importers
1. Check applications that they work as expected with the new LTS 2017 version, especially searches.
1. Setup one or more new nuxeo instances with LTS 2017 
   1. Install the new instances on the same hosts as the existing instance to be able to use the same doc-exchange folder for import and export.
1. Configure for these instances to the same binary store as for the existing instances
1. Configure for these instances the new mongodb for the meta data repository.
1. Continue with "Sequence of steps for data migration" above for the user and meta data mirgration.
1. Stop importers (preferrably over a weekend)
1. Do step 7 from "Sequence of steps for data migration" one last time
1. reconfigure the all existing instances to use the mongo db as new meta data repository.
1. re-enable importers.
1. Shut down the new nuxeo instances which where used only for importing the data.


References
----------
* [Nuxeo Import Export API] (https://doc.nuxeo.com/nxdoc/nuxeo-core-import-export-api/)
* [Nuxeo Cli Development] (https://doc.nuxeo.com/nxdoc/nuxeo-cli/)
* [Nuxeo Contribute Operation] (https://doc.nuxeo.com/910/nxdoc/contributing-an-operation/)
* [Nuxeo Upgrade Plattform] (https://doc.nuxeo.com/710/admindoc/upgrading-the-nuxeo-platform/)
* [Nuxeo Upgrade to LTS 2016] (https://doc.nuxeo.com/nxdoc/upgrade-from-lts-2015-to-lts-2016/)
* [Nuxeo Upgrade to LTS 2017] (https://doc.nuxeo.com/nxdoc/upgrade-from-lts-2016-to-lts-2017/)
