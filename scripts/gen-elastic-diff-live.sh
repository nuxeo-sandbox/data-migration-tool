#!/bin/bash

echo "start"
date

touch start-processing-elastic-diffs.txt


echo "psql export"
## check only not deleted docs
PGPASSWORD=xxx psql -U nuxeo -h db.host.net -P pager=off -c "SELECT  h.id from HIERARCHY h where h.PRIMARYTYPE in ('santanderFile','Idms_file') ;" | grep -v -e "----" | sed "s/ //g" > /home/mhuth/el-dump/doc-ids-psql-$(date +%Y-%m-%d).txt

date

echo "elastic export"
elasticdump --input=http://localhost:9200/nuxeo_live --output=/home/mhuth/el-dump/elastic-dump-res-$(date +%Y-%m-%d).txt --type=data --searchBody '{"query": { "match_all": {} }, "_source": false }' > /home/mhuth/el-dump/dump-out-$(date +%Y-%m-%d).txt

date

echo "filter ids from elastic dump"
cut -c45-80 /home/mhuth/el-dump/elastic-dump-res-$(date +%Y-%m-%d).txt > /home/mhuth/el-dump/elastic-ids-$(date +%Y-%m-%d).txt


echo "sort"
sort --temporary-directory=/home/mhuth/tmp/ /home/mhuth/el-dump/doc-ids-psql-$(date +%Y-%m-%d).txt > /home/mhuth/el-dump/doc-ids-psql-$(date +%Y-%m-%d)_s.txt
sort --temporary-directory=/home/mhuth/tmp/ /home/mhuth/el-dump/elastic-ids-$(date +%Y-%m-%d).txt > /home/mhuth/el-dump/elastic-ids-$(date +%Y-%m-%d)_s.txt
wc -l /home/mhuth/el-dump/doc-ids-psql-$(date +%Y-%m-%d)_s.txt
wc -l /home/mhuth/el-dump/elastic-ids-$(date +%Y-%m-%d)_s.txt

date 

echo "diff"
comm -2 -3 /home/mhuth/el-dump/doc-ids-psql-$(date +%Y-%m-%d)_s.txt /home/mhuth/el-dump/elastic-ids-$(date +%Y-%m-%d)_s.txt > /home/mhuth/el-dump/doc-ids-to-reindex-live-$(date +%Y-%m-%d).txt
wc -l /home/mhuth/el-dump/doc-ids-to-reindex-live-$(date +%Y-%m-%d).txt


echo "kill running processes from last diff"
/home/mhuth/el-dump/kill.sh

rm /home/mhuth/el-dump/reindex-processed-ids-1*

date

echo "start processing"
cd /home/mhuth/el-dump/ 
/home/mhuth/el-dump/start.sh

echo "end"
date
