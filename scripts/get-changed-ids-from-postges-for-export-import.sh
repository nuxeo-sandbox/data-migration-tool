#!/bin/bash

echo "start"
date
touch /home/mhuth/export-import-changes/start-processing.txt

echo "psql changes since yesterday"

if [ ! -f doc-head-ids-changed-live-psql_$(date +%Y-%m-%d).txt ]; then
    PGPASSWORD=xxx psql -U nuxeo -h db.host.net -P pager=off -c "select h.id from hierarchy h left join misc m on h.id = m.id join dublincore d on h.id = d.id where h.PRIMARYTYPE in ('Idms_file', 'santanderFile') and h.ISVERSION IS NULL and m.lifecyclestate != 'deleted' and d.modified >= (current_date - integer '1' + time '06:00') ;" | grep -v -e "----" | sed "s/ //g" > doc-head-ids-changed-live-psql_$(date +%Y-%m-%d).txt
else
	echo "file doc-head-ids-changed-live-psql_$(date +%Y-%m-%d).txt exists!!!"
fi


wc -l doc-head-ids-changed-live-psql_$(date +%Y-%m-%d).txt
sort doc-head-ids-changed-live-psql_$(date +%Y-%m-%d).txt > doc-head-ids-changed-live-psql_$(date +%Y-%m-%d)_s.txt
cp doc-head-ids-changed-live-psql_$(date +%Y-%m-%d)_s.txt /home/mhuth/export-import-changes/to-process-$(date +%Y-%m-%d).txt

date

cd /home/mhuth/export-import-changes/
echo "call start.sh"
rm /home/mhuth/export-import-changes/export-import-processed-ids-1*
/home/mhuth/export-import-changes/start.sh

echo "processes started"
date
disown -a
