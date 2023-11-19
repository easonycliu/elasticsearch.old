root_dir=/home/eason/elasticsearch_proj
user_name=eason

docker compose -f $root_dir/autocancel_exp/elasticsearch_exp/cluster/single_node.yml down && \
sudo chown -R $user_name:$user_name $root_dir/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
curr_wd=$(pwd) && \
cd $root_dir/elasticsearch && \
./gradlew localDistro && \
cd $curr_wd && \
sudo chown -R 1000:1000 $root_dir/elasticsearch/build/distribution/local/elasticsearch-8.9.0-SNAPSHOT && \
docker compose -f $root_dir/autocancel_exp/elasticsearch_exp/cluster/single_node.yml up

