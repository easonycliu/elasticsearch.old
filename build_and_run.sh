docker-compose --compatibility -f /home/eason/elasticsearch_proj/autocancel_exp/cluster/single_node.yml down && \
sudo chown -R eason:eason /home/eason/elasticsearch_proj/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
curr_wd=$(pwd) && \
cd /home/eason/elasticsearch_proj/elasticsearch && \
./gradlew localDistro && \
cd $curr_wd && \
sudo chown -R 1000:1000 /home/eason/elasticsearch_proj/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
docker-compose --compatibility -f /home/eason/elasticsearch_proj/autocancel_exp/cluster/single_node.yml up